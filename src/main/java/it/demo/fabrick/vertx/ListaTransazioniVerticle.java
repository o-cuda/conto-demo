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
import it.demo.fabrick.dto.ErrorCode;
import it.demo.fabrick.dto.TransactionDto;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ListaTransazioniVerticle extends AbstractVerticle {

	@Value("${fabrick.apiKey}")
	private String apiKey;

	@Value("${fabrick.authSchema}")
	private String authSchema;

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		String bus = "lista_bus";
		log.debug("Subscribing to event bus: {}", bus);
		vertx.eventBus().consumer(bus, message -> {

			lanciaChiamataEsterna(message);
		});
	}

	public void lanciaChiamataEsterna(Message<Object> message) {

		JsonObject json = (JsonObject) message.body();
		String indirizzo = json.getString("indirizzo");

		log.info("Processing transactions list request");

		WebClient client = WebClient.create(vertx);

		client.requestAbs(HttpMethod.GET, indirizzo)
				.putHeader("Content-Type", "application/json")
				.putHeader("Auth-Schema", authSchema)
				.putHeader("Api-Key", apiKey)
				.putHeader("Content-Type", "application/json")
				.sendBuffer(Buffer.buffer(""), ar -> {
					if (ar.succeeded()) {

						HttpResponse<Buffer> response = ar.result();
						int statusCode = response.statusCode();
						@Nullable
						String bodyAsString = response.bodyAsString();

						log.debug("Transactions API response status: {}", statusCode);

						if (statusCode >= 300) {
							String errorMessage = "API error response: " + bodyAsString;
							log.error(errorMessage);
							message.fail(ErrorCode.API_ERROR.getCode(), errorMessage);
							return;
						}

						TransactionDto transaction = null;
						ObjectMapper mapper = new ObjectMapper();
						String listaTransazioni = null;
						try {
							transaction = mapper.readValue(bodyAsString, TransactionDto.class);
							int transactionCount = transaction.getPayload().getList() != null ? transaction.getPayload().getList().size() : 0;
							listaTransazioni = mapper.writeValueAsString(transaction.getPayload().getList());
							log.info("Transactions retrieved successfully - count: {}", transactionCount);
						} catch (JsonProcessingException e) {
							log.error("Failed to parse transactions response", e);
							message.fail(ErrorCode.PARSE_ERROR.getCode(), "Failed to parse transactions response: " + e.getMessage());
							return;
						}

						message.reply(listaTransazioni);

					} else {
						String errorMessage = String.format("Failed to connect to transactions service: %s", ar.cause().getMessage());
						log.error(errorMessage);
						message.fail(ErrorCode.NETWORK_ERROR.getCode(), errorMessage);
					}
				});
	}

}
