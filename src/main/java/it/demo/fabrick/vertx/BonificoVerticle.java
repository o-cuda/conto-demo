package it.demo.fabrick.vertx;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import it.demo.fabrick.ContoDemoApplication;
import it.demo.fabrick.dto.BonificoRequestDto;
import it.demo.fabrick.dto.BonificoResponseDto;
import it.demo.fabrick.dto.ErrorDto;
import it.demo.fabrick.dto.ErrorCode;
import it.demo.fabrick.dto.ErrorResponse;
import it.demo.fabrick.dto.ListaTransactionDto;
import it.demo.fabrick.utils.TransactionValidationUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BonificoVerticle extends AbstractVerticle {

	@Value("${fabrick.apiKey}")
	private String apiKey;

	@Value("${fabrick.authSchema}")
	private String authSchema;

	private static final int MONEY_TRANSFER_TIMEOUT_MS = 120000; // 120 seconds (exceeds Fabrick's recommended 100 seconds)

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		String bus = "bonifico_bus";
		log.debug("Subscribing to event bus: {}", bus);
		vertx.eventBus().consumer(bus, message -> {

			lanciaChiamataEsterna(message);
		});
	}

	public void lanciaChiamataEsterna(Message<Object> message) {

		JsonObject json = (JsonObject) message.body();

		String indirizzo = json.getString("indirizzo");
		JsonObject mappaMessageIn = json.getJsonObject("mappaMessageIn");

		String creditorName = mappaMessageIn != null ? mappaMessageIn.getString("creditor-name") : "unknown";
		log.info("Processing money transfer to creditor: {}", creditorName);

		WebClient client = WebClient.create(vertx);

		String requestString = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			BonificoRequestDto request = new BonificoRequestDto(mappaMessageIn);
			requestString = mapper.writeValueAsString(request);
			log.debug("Money transfer request: {}", requestString);
		} catch (JsonProcessingException e1) {
			log.error("Failed to parse money transfer request for creditor: {}", creditorName, e1);
			message.fail(ErrorCode.PARSE_ERROR.getCode(), "Failed to parse money transfer request: " + e1.getMessage());
			return;
		}

		client.requestAbs(HttpMethod.POST, indirizzo)
				.timeout(MONEY_TRANSFER_TIMEOUT_MS)
				.putHeader("Content-Type", "application/json")
				.putHeader("Auth-Schema", authSchema)
				.putHeader("Api-Key", apiKey)
				.sendBuffer(Buffer.buffer(requestString), ar -> {
					if (ar.succeeded()) {

						HttpResponse<Buffer> response = ar.result();
						int statusCode = response.statusCode();
						@Nullable
						String bodyAsString = response.bodyAsString();

						log.debug("Received money transfer response with status code: {}", statusCode);

						if (statusCode >= 300) {
							// Check if this is an error that requires validation enquiry (HTTP 500 or 504)
							if (statusCode == 500 || statusCode == 504) {
								log.warn("Money transfer returned HTTP {} - performing validation enquiry for creditor: {}", statusCode, creditorName);
								performValidationEnquiry(message, mappaMessageIn, mapper);
								return;
							}

							// Handle other errors
							ErrorDto errore = null;
							try {
								errore = mapper.readValue(bodyAsString, ErrorDto.class);
							} catch (JsonProcessingException e) {
								log.error("Failed to parse error response from API for creditor: {}", creditorName, e);
								message.fail(ErrorCode.PARSE_ERROR.getCode(), "Failed to parse error response: " + e.getMessage());
								return;
							}

							StringBuilder builder = new StringBuilder();
							errore.getErrors().stream().forEach(anError -> {
								builder.append("code: ").append(anError.getCode()).append(", description:")
										.append(anError.getDescription()).append(",");
							});

							String messaggioDiErrore = builder.toString();
							messaggioDiErrore = messaggioDiErrore.substring(0, messaggioDiErrore.length() - 1);
							log.error("Money transfer API error for creditor {}: {}", creditorName, messaggioDiErrore);
							message.fail(ErrorCode.API_ERROR.getCode(), messaggioDiErrore);
							return;
						}

						// Parse successful response and return transaction ID
						try {
							BonificoResponseDto bonificoResponse = mapper.readValue(bodyAsString, BonificoResponseDto.class);
							if (bonificoResponse != null && bonificoResponse.getPayload() != null && bonificoResponse.getPayload().getTransactionId() != null) {
								String transactionId = bonificoResponse.getPayload().getTransactionId();
								log.info("Money transfer executed successfully for creditor {} - Transaction ID: {}", creditorName, transactionId);
								message.reply("Transfer executed - Transaction ID: " + transactionId);
							} else {
								log.warn("Transfer response received for creditor {} but no transaction ID found", creditorName);
								message.reply("Transfer executed - Transaction ID not provided in response");
							}
						} catch (JsonProcessingException e) {
							log.error("Failed to parse successful money transfer response for creditor: {}", creditorName, e);
							message.fail(ErrorCode.PARSE_ERROR.getCode(), "Transfer executed but failed to parse response: " + e.getMessage());
						}

					} else {
						String errorMessage = String.format("Failed to call money transfer service for creditor %s: %s", creditorName, ar.cause().getMessage());
						log.error(errorMessage);
						message.fail(ErrorCode.NETWORK_ERROR.getCode(), errorMessage);
					}
				});
	}

	/**
	 * Performs a validation enquiry when receiving HTTP 500 or 504 errors.
	 * Searches the transactions list for a matching money transfer to determine
	 * if the transfer was executed despite the error response.
	 * This simulates a LIS request through GestisciRequestVerticle to properly
	 * obtain the URL from CONTO_INDIRIZZI table.
	 *
	 * @param originalMessage the original event bus message to reply to
	 * @param mappaMessageIn the original request parameters
	 * @param mapper the Jackson object mapper for JSON parsing
	 */
	private void performValidationEnquiry(Message<Object> originalMessage, JsonObject mappaMessageIn, ObjectMapper mapper) {

		String amountStr = mappaMessageIn.getString("amount");
		String currency = mappaMessageIn.getString("currency");
		String description = mappaMessageIn.getString("description");
		String creditorName = mappaMessageIn.getString("creditor-name");

		log.info("Starting validation enquiry for transfer to creditor: {} (amount: {} {})", creditorName, amountStr, currency);

		// Parse amount as BigDecimal for precise comparison
		BigDecimal amount;
		try {
			amount = new BigDecimal(amountStr);
		} catch (NumberFormatException e) {
			log.error("Failed to parse amount for validation enquiry: {}", amountStr, e);
			originalMessage.fail(ErrorCode.PARSE_ERROR.getCode(), "Invalid amount format: " + amountStr);
			return;
		}

		// Use today's date for the search (money transfer should appear same day)
		LocalDate today = LocalDate.now();
		String todayStr = today.toString();

		// Build a raw LIS message to send through GestisciRequestVerticle
		// Format: LIS + 10 chars for start-date + 10 chars for end-date
		// Based on CONTO_CONFIGURATION: 'OPERAZIONE=3;start-date=10;end-date=10;'
		String lisMessage = "LIS" +
				String.format("%-10s", todayStr) +
				String.format("%-10s", todayStr);

		log.debug("Sending LIS message for validation enquiry: {}", lisMessage);

		// Send to GestisciRequestVerticle which will route to ListaTransazioniVerticle
		vertx.eventBus().request("gestisci-chiamata-bus", lisMessage,
				ContoDemoApplication.getDefaultDeliverOptions()
						.setSendTimeout(30000), // 30 seconds timeout for validation enquiry
				ar -> {
			if (ar.succeeded()) {
				String responseString = (String) ar.result().body();
				log.debug("Validation enquiry response received for creditor: {}", creditorName);

				// Parse transactions and search for matching transfer
				try {
					com.fasterxml.jackson.core.type.TypeReference<java.util.List<ListaTransactionDto>> typeRef =
							new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ListaTransactionDto>>() {};
					java.util.List<ListaTransactionDto> transactions = mapper.readValue(responseString, typeRef);

					if (transactions == null || transactions.isEmpty()) {
						log.warn("No transactions found in validation enquiry response for creditor: {}", creditorName);
						originalMessage.fail(ErrorCode.API_ERROR.getCode(), "Transfer execution uncertain - no transactions found. Please verify before retrying.");
						return;
					}

					// Search for matching transaction using BigDecimal for amount
					ListaTransactionDto matchingTransaction = TransactionValidationUtil.findMatchingTransaction(
							transactions,
							amount, currency, description, creditorName);

					if (matchingTransaction != null) {
						log.info("Validation enquiry successful - transfer executed for creditor {} - Transaction ID: {}", creditorName, matchingTransaction.getTransactionId());
						originalMessage.reply("Transfer executed - Transaction ID: " + matchingTransaction.getTransactionId());
					} else {
						log.info("Validation enquiry complete - no matching transfer found for creditor: {}", creditorName);
						originalMessage.fail(ErrorCode.API_ERROR.getCode(), "Transfer not executed. You may safely retry.");
					}

				} catch (JsonProcessingException e) {
					log.error("Failed to parse validation enquiry response for creditor: {}", creditorName, e);
					originalMessage.fail(ErrorCode.PARSE_ERROR.getCode(), "Transfer execution uncertain - unable to verify. Please manually check before retrying.");
				}

			} else {
				log.error("Validation enquiry request failed for creditor: {}", creditorName, ar.cause());
				originalMessage.fail(ErrorCode.TIMEOUT_ERROR.getCode(), "Transfer execution uncertain - validation enquiry failed. Please manually check before retrying.");
			}
		});
	}

}
