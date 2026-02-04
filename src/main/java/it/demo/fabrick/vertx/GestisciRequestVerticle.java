package it.demo.fabrick.vertx;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import it.demo.fabrick.utils.Constants;

@Component
public class GestisciRequestVerticle extends AbstractVerticle {

	private static final String REGEX_URL_PARAMETER = "(\\{.+?\\})";
	private static Logger log = LoggerFactory.getLogger(GestisciRequestVerticle.class);

	@Value("${fabrick.accountId}")
	private String accountId;

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		log.info("start - lanciato");
		log.debug("mi sottoscrivo al bus 'gestisci-chiamata-bus' ..");
		vertx.eventBus().consumer("gestisci-chiamata-bus", message -> {
		
			gestisciChiamata(message);
		});
	}

	public void gestisciChiamata(Message<Object> message) {
		
		log.info("gestisciChiamata - start");

		Object body = message.body();
		log.trace("1 received message.body() = {}", body);
		

		String messageIn = body.toString();
		String operazioneInEntrata = messageIn.substring(0, 3);
		log.debug("scrivo sul bus get-configurazione-bus l'operazione {} ", operazioneInEntrata);

		vertx.eventBus().request("get-configurazione-bus", operazioneInEntrata, ContoDemoApplication.getDefaultDeliverOptions(), asyncResult -> {

			if (asyncResult.succeeded()) {

				JsonArray line = (JsonArray) asyncResult.result().body();
				log.debug("ricevuta risposta da get-configurazione-bus: {}", line);

				ConfigurazioneDto configurazione = new ConfigurazioneDto(line);
				log.trace("configurazione: {}", configurazione);

				Map<String, String> mappaInput = creaConfigurazioneMessage(configurazione.getMessageIn());
				Map<String, String> mappaMessageIn;
				try {
					mappaMessageIn = decodeAnInput(messageIn, mappaInput);
				

					if (log.isDebugEnabled()) {
						Set<String> keySet = mappaMessageIn.keySet();
						for (String key : keySet) {

							log.debug("{}: '{}'", key, mappaMessageIn.get(key));
						}
					}

					lanciaChiamateEsterne(configurazione, message, mappaMessageIn);

				} catch (ExceptionMessageIn e) {
					log.error("Message parsing failed", e);
					message.fail(ErrorCode.VALIDATION_ERROR.getCode(), e.getMessage());
				}

			} else {
				log.error("Failed to get configuration", asyncResult.cause());
				message.fail(ErrorCode.CONFIGURATION_ERROR.getCode(), asyncResult.cause().getMessage());
			}
		});

	}

	private void lanciaChiamateEsterne(ConfigurazioneDto configurazione, Message<Object> message,
			Map<String, String> mappaMessageIn) {

		log.info("lanciaChiamateEsterne - start");

		JsonObject json = new JsonObject();
		
		String indirizzo = sovrascriviAccountNumber(configurazione.getIndirizzo());
		indirizzo = sovrascriviChiaviSullIndirizzo(mappaMessageIn, indirizzo);
		json.put("indirizzo", indirizzo);
		json.put("mappaMessageIn", mappaMessageIn);

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

	private String sovrascriviChiaviSullIndirizzo(Map<String, String> mappaMessageIn, String indirizzo) {

		String result = null;
		StringBuffer builder = new StringBuffer();
		Matcher matcher = Pattern.compile(REGEX_URL_PARAMETER).matcher(indirizzo);
		while (matcher.find()){

			String key = matcher.group();
			key = key.substring(1, key.length() - 1);
			String value = mappaMessageIn.get(key);

			matcher.appendReplacement(builder, value);
		}

		matcher.appendTail(builder);

		result = builder.toString();
		if (Constants.EMPTY_STRING.equals(result)) {
			result = indirizzo;
		}

		return result;
	}

	private String sovrascriviAccountNumber(String indirizzo) {

		// Replace {account-number} placeholder with accountId from application properties
		return indirizzo.replace("{account-number}", accountId);
	}

	public static String padRight(String stringa, int lunghezza) {
		return String.format("%-" + lunghezza + "s", stringa);
	}

	public static String padLeft(String stringa, int lunghezza) {
		return String.format("%" + lunghezza + "s", stringa);
	}

	private Map<String, String> creaConfigurazioneMessage(String configurazioneDaLavorare) {

		Map<String, String> mappaConfigurazione = new LinkedHashMap<>();

		String[] split = configurazioneDaLavorare.split(";");
		for (String configurazineDiUnCampo : split) {

			String[] split2 = configurazineDiUnCampo.split("=");

			mappaConfigurazione.put(split2[0], split2[1]);
		}

		return mappaConfigurazione;
	}

	private Map<String, String> decodeAnInput(String messageInInput, Map<String, String> creaConfigurazioneInput)
			throws ExceptionMessageIn {

		Map<String, String> mappaMessageIn = new HashMap<>();

		int start = 0;
		int end = 0;

		Set<String> keySet = creaConfigurazioneInput.keySet();
		for (String key : keySet) {
			
			String valore = creaConfigurazioneInput.get(key);
			log.trace("Key {} , valore {}", key, valore);
			String substring = null;

			try {
				

				if (Pattern.matches("^NULLIFEMTPY.*", valore)) {

					start = end;
					end = start + Integer.valueOf(valore.substring(11));
					substring = messageInInput.substring(start, end).trim();

					if (substring.isEmpty()) {
						substring = null;
					}

				} else if (Pattern.matches("^NOTRIM.*", valore)) {

					start = end;
					end = start + Integer.valueOf(valore.substring(6));
					substring = messageInInput.substring(start, end);

				} else if (Pattern.matches("^[A-Z]{3}.*", valore)) {

					start = end;
					end = start + Integer.valueOf(valore.substring(3));
					substring = messageInInput.substring(start, end).replaceFirst("^0+(?!$)", Constants.EMPTY_STRING)
							.trim();

				} else {

					start = end;
					end = start + Integer.valueOf(valore);
					substring = messageInInput.substring(start, end).trim();
				}

			} catch (StringIndexOutOfBoundsException e) {
				log.error("Exception message [" + messageInInput + "]", e);
				throw new ExceptionMessageIn();
			}

			mappaMessageIn.put(key, substring);
		}

		return mappaMessageIn;

	}

}
