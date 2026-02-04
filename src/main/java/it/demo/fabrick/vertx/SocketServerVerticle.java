package it.demo.fabrick.vertx;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import it.demo.fabrick.ContoDemoApplication;
import it.demo.fabrick.dto.ErrorCode;

@Component
public class SocketServerVerticle extends AbstractVerticle {

	private Logger log = LoggerFactory.getLogger(getClass());

	private static final String CHARSET = "cp280";
//	private static final String charset = "UTF-8";

	@Override
	public void start() throws Exception {



		NetServerOptions options = new NetServerOptions().setPort(9221);
		options.setIdleTimeout(55);
		NetServer server = vertx.createNetServer(options);

		server.connectHandler(socket -> {

			socket.handler(bufferIn -> {
				String requestId = UUID.randomUUID().toString();
				ContextualData.put("requestId", requestId);
				String messageInMessage = bufferIn.toString(CHARSET);
				log.info("Request received - requestId: {}, operation: {}", requestId, messageInMessage.substring(0, 3));

				vertx.eventBus().request("gestisci-chiamata-bus", messageInMessage,
						ContoDemoApplication.getDefaultDeliverOptions(), asyncResult -> {

					if (asyncResult.succeeded()) {

						String messageOut = (String) asyncResult.result().body();
						log.info("Request completed successfully - requestId: {}", requestId);

						int stringLen = 500;

						String stringOut = "0" + messageOut;
						String stringFill = String.format("%-" + (stringLen) + "s", stringOut);

						socket.write(stringFill, CHARSET);
						log.debug("Response sent for requestId: {} - {} bytes", requestId, stringFill.length());
					} else {
						// Include error code in response format
						int errorCode = asyncResult.cause() instanceof io.vertx.core.eventbus.ReplyException
								? ((io.vertx.core.eventbus.ReplyException) asyncResult.cause()).failureCode()
								: ErrorCode.UNKNOWN_ERROR.getCode();
						String errorMessage = asyncResult.cause().getMessage();
						String errorResponse = String.format("1[%s] %s", requestId, errorMessage);
						log.error("Request failed - requestId: {}, code: {}, message: {}", requestId, errorCode, errorMessage);
						socket.write(errorResponse, CHARSET);
					}
					socket.end();
				});

			});

			log.debug("Socket connection opened");

			socket.closeHandler(v -> log.debug("Socket connection closed") );

		});

		server.listen(res -> {
			if (res.succeeded()) {
				log.info("TCP server listening on port 9221");
			} else {
				log.error("Failed to start TCP server", res.cause());
			}
		});
	}

}
