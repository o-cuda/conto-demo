package it.demo.fabrick.vertx;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.demo.fabrick.ContoDemoApplication;
import it.demo.fabrick.dto.ConfigurazioneDto;
import it.demo.fabrick.dto.ErrorCode;
import it.demo.fabrick.exception.ExceptionMessageIn;
import it.demo.fabrick.utils.MessageParserUtil;

@Component
public class GestisciRequestVerticle extends AbstractVerticle {

	private static Logger log = LoggerFactory.getLogger(GestisciRequestVerticle.class);

	@Value("${fabrick.accountId}")
	private String accountId;

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		log.debug("Subscribing to event bus: gestisci-chiamata-bus");
		vertx.eventBus().consumer("gestisci-chiamata-bus", message -> {

			gestisciChiamata(message);
		});
	}

	public void gestisciChiamata(Message<Object> message) {

		Object body = message.body();
		String messageIn = body.toString();
		String operazioneInEntrata = messageIn.substring(0, 3);

		log.info("Processing request for operation: {}", operazioneInEntrata);

		vertx.eventBus().request("get-configurazione-bus", operazioneInEntrata, ContoDemoApplication.getDefaultDeliverOptions(), asyncResult -> {

			if (asyncResult.succeeded()) {

				JsonArray line = (JsonArray) asyncResult.result().body();
				log.debug("Configuration received for operation: {}", operazioneInEntrata);

				ConfigurazioneDto configurazione = new ConfigurazioneDto(line);

				Map<String, String> mappaInput = MessageParserUtil.parseConfiguration(configurazione.getMessageIn());
				Map<String, String> mappaMessageIn;
				try {
					mappaMessageIn = MessageParserUtil.decodeMessage(messageIn, mappaInput);

					log.debug("Message parsed for operation: {}", operazioneInEntrata);

					lanciaChiamateEsterne(configurazione, message, mappaMessageIn);

				} catch (ExceptionMessageIn e) {
					log.error("Message parsing failed for operation: {}", operazioneInEntrata, e);
					message.fail(ErrorCode.VALIDATION_ERROR.getCode(), e.getMessage());
				}

			} else {
				log.error("Failed to get configuration for operation: {}", operazioneInEntrata, asyncResult.cause());
				message.fail(ErrorCode.CONFIGURATION_ERROR.getCode(), asyncResult.cause().getMessage());
			}
		});

	}

	private void lanciaChiamateEsterne(ConfigurazioneDto configurazione, Message<Object> message,
			Map<String, String> mappaMessageIn) {

		JsonObject json = new JsonObject();

		String indirizzo = substituteAccountId(configurazione.getIndirizzo());
		indirizzo = MessageParserUtil.substituteUrlParameters(mappaMessageIn, indirizzo);
		json.put("indirizzo", indirizzo);
		json.put("mappaMessageIn", mappaMessageIn);

		log.debug("Routing to bus: {}", configurazione.getMessageOutFromBus());

		vertx.eventBus().request(configurazione.getMessageOutFromBus(), json, ContoDemoApplication.getDefaultDeliverOptions(), asyncResult -> {

			try {
				if (asyncResult.succeeded()) {

					String responseString = (String) asyncResult.result().body();

					log.debug("Response received from {}: {}", configurazione.getMessageOutFromBus(), responseString);

					message.reply(responseString);
				} else {
					log.error("Request to {} failed", configurazione.getMessageOutFromBus(), asyncResult.cause());
					// Propagate the error code from the downstream verticle, or use UNKNOWN_ERROR if not available
					int failureCode = asyncResult.cause() instanceof io.vertx.core.eventbus.ReplyException
							? ((io.vertx.core.eventbus.ReplyException) asyncResult.cause()).failureCode()
							: ErrorCode.UNKNOWN_ERROR.getCode();
					message.fail(failureCode, asyncResult.cause().getMessage());
				}
			} catch (Exception e) {
				log.error("Unexpected exception while processing response", e);
				message.fail(ErrorCode.UNKNOWN_ERROR.getCode(), "Unexpected error: " + e.getMessage());
			}
		});

	}

	/**
	 * Substitute {account-number} placeholder with actual accountId from application properties.
	 *
	 * @param indirizzo URL with {account-number} placeholder
	 * @return URL with account number substituted
	 */
	private String substituteAccountId(String indirizzo) {
		return indirizzo.replace("{account-number}", accountId);
	}

}
