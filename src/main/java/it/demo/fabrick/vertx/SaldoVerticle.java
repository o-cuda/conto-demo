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
import it.demo.fabrick.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SaldoVerticle extends AbstractVerticle {

	@Value("${fabrick.apiKey}")
	private String apiKey;

	@Value("${fabrick.authSchema}")
	private String authSchema;

	@Override
	public void start(io.vertx.core.Promise<Void> startFuture) throws Exception {

		String bus = "saldo_bus";
		log.debug("Subscribing to event bus: {}", bus);
		vertx.eventBus().consumer(bus, message -> {

			lanciaChiamataEsterna(message);
		});
	}

	public void lanciaChiamataEsterna(Message<Object> message) {

		JsonObject json = (JsonObject) message.body();
		String indirizzo = json.getString("indirizzo");

		log.info("Processing balance request");

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

						log.debug("Balance API response status: {}", statusCode);

						if (statusCode >= 300) {
							String errorMessage = "API error response: " + bodyAsString;
							log.error(errorMessage);
							message.fail(ErrorCode.API_ERROR.getCode(), errorMessage);
							return;
						}

						BalanceDto balance = null;
						ObjectMapper mapper = new ObjectMapper();
						try {
							balance = mapper.readValue(bodyAsString, BalanceDto.class);
						} catch (JsonProcessingException e) {
							log.error("Failed to parse balance response", e);
							message.fail(ErrorCode.PARSE_ERROR.getCode(), "Failed to parse balance response: " + e.getMessage());
							return;
						}

						StringBuilder finalResponse = new StringBuilder();
						finalResponse.append("balance: ")
								.append(balance.getPayload().getBalance()).append(" ")
								.append(balance.getPayload().getCurrency())
								.append(", availableBalance: ").append(balance.getPayload().getAvailableBalance())
								.append(" ").append(balance.getPayload().getCurrency());

						log.info("Balance retrieved successfully - balance: {} {}, available: {} {}",
								balance.getPayload().getBalance(),
								balance.getPayload().getCurrency(),
								balance.getPayload().getAvailableBalance(),
								balance.getPayload().getCurrency());
						message.reply(finalResponse.toString());

					} else {
						String errorMessage = String.format("Failed to connect to balance service: %s", ar.cause().getMessage());
						log.error(errorMessage);
						message.fail(ErrorCode.NETWORK_ERROR.getCode(), errorMessage);
					}
				});
	}

}
