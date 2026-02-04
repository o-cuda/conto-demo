package it.demo.fabrick.vertx;

import java.text.ParseException;
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

		log.info("start - lanciato");

		String bus = "bonifico_bus";
		log.debug("mi sottoscrivo al bus '{}' ..", bus);
		vertx.eventBus().consumer(bus, message -> {

			lanciaChiamataEsterna(message);
		});
	}

	public void lanciaChiamataEsterna(Message<Object> message) {

		log.info("lanciaChiamataEsterna - start");

		JsonObject json = (JsonObject) message.body();

		String indirizzo = json.getString("indirizzo");

		JsonObject mappaMessageIn = json.getJsonObject("mappaMessageIn");

		log.info("message.body().\"indirizzo\" = {}", indirizzo);
		log.info("message.body().\"mappaMessageIn\" = {}", mappaMessageIn);

		WebClient client = WebClient.create(vertx);

		String requestString = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			BonificoRequestDto request = new BonificoRequestDto(mappaMessageIn);
			requestString = mapper.writeValueAsString(request);
			log.debug("requestString: {}", requestString);
		} catch (JsonProcessingException | ParseException e1) {
			log.error("Failed to parse money transfer request", e1);
			message.fail(ErrorCode.PARSE_ERROR.getCode(), "Failed to parse money transfer request: " + e1.getMessage());
			return;
		}

		log.debug("richiamo servizio REST ...");
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

						log.info("Received response with status code: {}", statusCode);

						if (statusCode >= 300) {
							// Check if this is an error that requires validation enquiry (HTTP 500 or 504)
							if (statusCode == 500 || statusCode == 504) {
								log.warn("Received {} - performing validation enquiry to check if transfer was executed", statusCode);
								performValidationEnquiry(message, mappaMessageIn, mapper);
								return;
							}

							// Handle other errors
							ErrorDto errore = null;
							try {
								errore = mapper.readValue(bodyAsString, ErrorDto.class);
							} catch (JsonProcessingException e) {
								log.error("Failed to parse error response from API", e);
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
							log.error("Money transfer API error: {}", messaggioDiErrore);
							message.fail(ErrorCode.API_ERROR.getCode(), messaggioDiErrore);
							return;
						}

						log.info("Money transfer successful, parsing response");

						// Parse successful response and return transaction ID
						try {
							BonificoResponseDto bonificoResponse = mapper.readValue(bodyAsString, BonificoResponseDto.class);
							if (bonificoResponse != null && bonificoResponse.getPayload() != null && bonificoResponse.getPayload().getTransactionId() != null) {
								String transactionId = bonificoResponse.getPayload().getTransactionId();
								log.info("Money transfer executed successfully - Transaction ID: {}", transactionId);
								message.reply("Transfer executed - Transaction ID: " + transactionId);
							} else {
								log.warn("Transfer response received but no transaction ID found in response: {}", bodyAsString);
								message.reply("Transfer executed - Transaction ID not provided in response");
							}
						} catch (JsonProcessingException e) {
							log.error("Failed to parse successful money transfer response", e);
							message.fail(ErrorCode.PARSE_ERROR.getCode(), "Transfer executed but failed to parse response: " + e.getMessage());
						}

					} else {
						String errorMessage = String.format("Failed to call money transfer service: %s", ar.cause().getMessage());
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

		log.info("performValidationEnquiry - starting validation enquiry");

		String amountStr = mappaMessageIn.getString("amount");
		String currency = mappaMessageIn.getString("currency");
		String description = mappaMessageIn.getString("description");
		String creditorName = mappaMessageIn.getString("creditor-name");

		log.info("Searching for transfer - amount: {}, currency: {}, description: {}, creditor: {}",
				amountStr, currency, description, creditorName);

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
				log.info("Validation enquiry response received: {}", responseString);

				// Parse transactions and search for matching transfer
				try {
					com.fasterxml.jackson.core.type.TypeReference<java.util.List<ListaTransactionDto>> typeRef =
							new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ListaTransactionDto>>() {};
					java.util.List<ListaTransactionDto> transactions = mapper.readValue(responseString, typeRef);

					if (transactions == null || transactions.isEmpty()) {
						log.warn("No transactions found in response");
						originalMessage.fail(ErrorCode.API_ERROR.getCode(), "Transfer execution uncertain - no transactions found. Please verify before retrying.");
						return;
					}

					// Search for matching transaction
					ListaTransactionDto matchingTransaction = TransactionValidationUtil.findMatchingTransaction(
							transactions,
							amountStr, currency, description, creditorName);

					if (matchingTransaction != null) {
						log.info("Found matching transaction - transfer was executed successfully: {}", matchingTransaction.getTransactionId());
						originalMessage.reply("Transfer executed - Transaction ID: " + matchingTransaction.getTransactionId());
					} else {
						log.warn("No matching transaction found - transfer was likely not executed");
						originalMessage.fail(ErrorCode.API_ERROR.getCode(), "Transfer not executed. You may safely retry.");
					}

				} catch (JsonProcessingException e) {
					log.error("Error parsing validation enquiry response", e);
					originalMessage.fail(ErrorCode.PARSE_ERROR.getCode(), "Transfer execution uncertain - unable to verify. Please manually check before retrying.");
				}

			} else {
				log.error("Validation enquiry request failed: {}", ar.cause().getMessage());
				originalMessage.fail(ErrorCode.TIMEOUT_ERROR.getCode(), "Transfer execution uncertain - validation enquiry failed. Please manually check before retrying.");
			}
		});
	}

}
