package it.demo.fabrick.vertx;

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
import it.demo.fabrick.dto.TransactionDto;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ListaTransazioniVerticle extends AbstractVerticle {

	@Value("${fabrick.apiKey}")
	private String apiKey;

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		log.info("start - lanciato");

		String bus = "lista_bus";
		log.debug("mi sottoscrivo al bus '{}' ..", bus);
		vertx.eventBus().consumer(bus, message -> {

			lanciaChiamataEsterna(message);
		});
	}

	public void lanciaChiamataEsterna(Message<Object> message) {
		
		log.info("lanciaChiamataEsterna - start");

		JsonObject json = (JsonObject) message.body();

		String indirizzo = json.getString("indirizzo");
		String httpMethod = json.getString("httpMethod");

		log.info("message.body().\"indirizzo\" = {}", indirizzo);
		log.info("message.body().\"httpMethod\" = {}", httpMethod);

		WebClient client = WebClient.create(vertx);

		log.debug("richiamo servizio REST ...");
		client.requestAbs(HttpMethod.valueOf(httpMethod), indirizzo)
				.putHeader("Content-Type", "application/json")
				.putHeader("Auth-Schema", "S2S")
				.putHeader("Api-Key", apiKey)
				.putHeader("Content-Type", "application/json")
				.sendBuffer(Buffer.buffer(""), ar -> {
					if (ar.succeeded()) {

						HttpResponse<Buffer> response = ar.result();
						int statusCode = response.statusCode();
						@Nullable
						String bodyAsString = response.bodyAsString();

						log.info("Received response with status code: {}", statusCode);

						if (statusCode >= 300) {
							String messaggio = "probabile errore nella response: " + bodyAsString;
							log.error(messaggio);
							message.fail(1, messaggio);
							return;
						} 
							
						log.info("bodyAsString: {}", bodyAsString);
						
						TransactionDto balance = null;
						ObjectMapper mapper = new ObjectMapper();
						try {
							balance = mapper.readValue(bodyAsString, TransactionDto.class);
						} catch (JsonProcessingException e) {
							log.error("error in json parsing", e);
							message.fail(1, "error in json parsing");
							return;
						}
						
						message.reply(bodyAsString);
						
					} else {
						String messaggio = String.format("Impossibile effettuare la chiamata al servizio, forse e' down: %s", ar.cause().getMessage());
						log.error(messaggio);
						message.fail(1, messaggio);
					}
				});
	}

}
