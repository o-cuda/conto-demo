package it.demo.fabrick.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import it.demo.fabrick.ContoDemoApplication;
import io.reactiverse.contextual.logging.ContextualData;

import java.util.UUID;

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

			// Ricezione dei dati da CICS
			socket.handler(bufferIn -> {
				String requestId = UUID.randomUUID().toString();
				ContextualData.put("requestId", requestId);
				String cobolInMessage = bufferIn.toString(CHARSET);
				log.info("cobolInMessage: [{}]", cobolInMessage);

				vertx.eventBus().request("gestisci-chiamata-bus", cobolInMessage, ContoDemoApplication.getDefaultDeliverOptions(), asyncResult -> {

					if (asyncResult.succeeded()) {

						String cobolOut = (String) asyncResult.result().body();
						log.info("cobolOut: {}", cobolOut);

						int stringLen = 500;

						// String stringCallBack = conv;
						String stringOut = "0" + cobolOut;
						String stringFill = String.format("%-" + (stringLen) + "s", stringOut);

						socket.write(stringFill, CHARSET);
						log.info("OUT: [{}] \n LEN: [{}]", stringFill, stringFill.length());
					} else {
						String errore = String.format("1[%s] %s", requestId, asyncResult.cause().getMessage());
						log.error("OUT: [{}]", errore);
						socket.write(errore, CHARSET);
					}
					socket.end();
				});

			});

			log.debug("Apertura socket in corso");

			// Intercettazione dell'evento di chiusura del socket
			socket.closeHandler(v -> log.debug("Il socket è stato chiuso") );

		});

		server.listen(res -> {
			if (res.succeeded()) {
				log.info("Il {} e' in ascolto!", getClass().getName());
			} else {
				log.error("Errore nell'avvio del {}!", getClass().getName());
			}
		});
	}

}
