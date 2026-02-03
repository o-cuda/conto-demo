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
import it.demo.fabrick.dto.BonificoRequestDto;
import it.demo.fabrick.dto.ErrorDto;
import it.demo.fabrick.dto.ListaTransactionDto;
import it.demo.fabrick.dto.TransactionDto;
import it.demo.fabrick.utils.TransactionValidationUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BonificoVerticle extends AbstractVerticle {

	@Value("${fabrick.apiKey}")
	private String apiKey;

	@Value("${fabrick.baseUrl:https://sandbox.platfr.io/api/gbs/banking/v4.0}")
	private String apiBaseUrl;

	private static final int MONEY_TRANSFER_TIMEOUT_MS = 110000; // 110 seconds (more than required 100)

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
			String messaggio = "Errore parse della request";
			log.error(messaggio, e1);
			message.fail(1, messaggio);
			return;
		}

		log.debug("richiamo servizio REST ...");
		client.requestAbs(HttpMethod.POST, indirizzo)
				.timeout(MONEY_TRANSFER_TIMEOUT_MS)
				.putHeader("Content-Type", "application/json")
				.putHeader("Auth-Schema", "S2S")
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
								log.error("error in json parsing", e);
								message.fail(1, "error in json parsing");
								return;
							}

							StringBuilder builder = new StringBuilder();
							errore.getErrors().stream().forEach(anError -> {
								builder.append("code: ").append(anError.getCode()).append(", description:")
										.append(anError.getDescription()).append(",");
							});

							String messaggioDiErrore = builder.toString();
							messaggioDiErrore = messaggioDiErrore.substring(0, messaggioDiErrore.length() - 1);
							log.error(messaggioDiErrore);
							message.fail(1, messaggioDiErrore);
							return;
						}

						log.info("bodyAsString: {}", bodyAsString);

						// FIXME not to do
						message.reply("TODO");

					} else {
						String messaggio = String.format("Impossibile effettuare la chiamata al servizio, forse e' down: %s", ar.cause().getMessage());
						log.error(messaggio);
						message.fail(1, messaggio);
					}
				});
	}

	/**
	 * Performs a validation enquiry when receiving HTTP 500 or 504 errors.
	 * Searches the transactions list for a matching money transfer to determine
	 * if the transfer was executed despite the error response.
	 *
	 * @param originalMessage the original event bus message to reply to
	 * @param mappaMessageIn the original request parameters
	 * @param mapper the Jackson object mapper for JSON parsing
	 */
	private void performValidationEnquiry(Message<Object> originalMessage, JsonObject mappaMessageIn, ObjectMapper mapper) {

		log.info("performValidationEnquiry - starting validation enquiry");

		String accountNumber = mappaMessageIn.getString("account-number");
		String amountStr = mappaMessageIn.getString("amount");
		String currency = mappaMessageIn.getString("currency");
		String description = mappaMessageIn.getString("description");
		String creditorName = mappaMessageIn.getString("creditor-name");

		log.info("Searching for transfer - account: {}, amount: {}, currency: {}, description: {}, creditor: {}",
				accountNumber, amountStr, currency, description, creditorName);

		// Use today's date for the search (money transfer should appear same day)
		LocalDate today = LocalDate.now();
		String todayStr = today.toString();

		// Build transactions list URL
		String transactionsUrl = String.format("%s/accounts/%s/transactions?fromAccountingDate=%s&toAccountingDate=%s",
				apiBaseUrl, accountNumber, todayStr, todayStr);

		log.debug("Transactions URL: {}", transactionsUrl);

		WebClient client = WebClient.create(vertx);

		client.requestAbs(HttpMethod.GET, transactionsUrl)
				.timeout(30000) // 30 seconds timeout for validation enquiry
				.putHeader("Auth-Schema", "S2S")
				.putHeader("Api-Key", apiKey)
				.putHeader("Content-Type", "application/json")
				.send(ar -> {
					if (ar.succeeded()) {
						HttpResponse<Buffer> response = ar.result();
						int statusCode = response.statusCode();
						String bodyAsString = response.bodyAsString();

						log.info("Validation enquiry response - status: {}", statusCode);

						if (statusCode >= 300) {
							log.error("Validation enquiry failed with status {}: {}", statusCode, bodyAsString);
							originalMessage.fail(1, "Transfer failed: " + bodyAsString);
							return;
						}

						// Parse transactions and search for matching transfer
						try {
							TransactionDto transactionDto = mapper.readValue(bodyAsString, TransactionDto.class);

							if (transactionDto.getPayload() == null || transactionDto.getPayload().getList() == null) {
								log.warn("No transactions found in response");
								originalMessage.fail(1, "Transfer execution uncertain - no transactions found. Please verify before retrying.");
								return;
							}

							// Search for matching transaction
							ListaTransactionDto matchingTransaction = TransactionValidationUtil.findMatchingTransaction(
									transactionDto.getPayload().getList(),
									amountStr, currency, description, creditorName);

							if (matchingTransaction != null) {
								log.info("Found matching transaction - transfer was executed successfully: {}", matchingTransaction.getTransactionId());
								originalMessage.reply("Transfer executed - Transaction ID: " + matchingTransaction.getTransactionId());
							} else {
								log.warn("No matching transaction found - transfer was likely not executed");
								originalMessage.fail(1, "Transfer not executed. You may safely retry.");
							}

						} catch (JsonProcessingException e) {
							log.error("Error parsing validation enquiry response", e);
							originalMessage.fail(1, "Transfer execution uncertain - unable to verify. Please manually check before retrying.");
						}

					} else {
						log.error("Validation enquiry request failed: {}", ar.cause().getMessage());
						originalMessage.fail(1, "Transfer execution uncertain - validation enquiry failed. Please manually check before retrying.");
					}
				});
	}

}
