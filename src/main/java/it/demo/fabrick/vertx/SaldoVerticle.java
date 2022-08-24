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
import it.demo.fabrick.dto.BalanceDto;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SaldoVerticle extends AbstractVerticle {

	@Value("${fabrick.apiKey}")
	private String apiKey;

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		log.info("start - lanciato");

		String bus = "saldo_bus";
		log.debug("mi sottoscrivo al bus '{}' ..", bus);
		vertx.eventBus().consumer(bus, message -> {

			lanciaChiamataEsterna(message);
		});
	}

	public void lanciaChiamataEsterna(Message<Object> message) {
		
		log.info("lanciaChiamataEsterna - start");

		JsonObject json = (JsonObject) message.body();

		String indirizzo = json.getString("indirizzo");

		log.info("message.body().\"indirizzo\" = {}", indirizzo);

		WebClient client = WebClient.create(vertx);

		log.debug("richiamo servizio REST ...");
		client.requestAbs(HttpMethod.GET, indirizzo)
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
						
						BalanceDto balance = null;
						ObjectMapper mapper = new ObjectMapper();
						try {
							balance = mapper.readValue(bodyAsString, BalanceDto.class);
						} catch (JsonProcessingException e) {
							log.error("error in json parsing", e);
							message.fail(1, "error in json parsing");
							return;
						}
						
						StringBuilder finaResponse = new StringBuilder();
						finaResponse.append("balance: ")
								.append(balance.getPayload().getBalance()).append(" ")
								.append(balance.getPayload().getCurrency())
								.append(", availableBalance: ").append(balance.getPayload().getAvailableBalance())
								.append(" ").append(balance.getPayload().getCurrency());
						
						message.reply(finaResponse.toString());
						
					} else {
						String messaggio = String.format("Impossibile effettuare la chiamata al servizio, forse e' down: %s", ar.cause().getMessage());
						log.error(messaggio);
						message.fail(1, messaggio);
					}
				});
	}

}
