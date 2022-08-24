package it.demo.fabrick;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;

@PropertySource("${appProp:file:/data/application.properties}")
@SpringBootApplication
@EnableWebSecurity
@ComponentScan(basePackages = "it.demo.fabrick")
public class ContoDemoApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContoDemoApplication.class);

    @Autowired
	private List<Verticle> verticleList;

	@Value("${vertx.eventLoopExecuteTime:2000000000}")
	private String eventLoopExecuteTime;

    public static void main(String[] args) {

        // lancio di SpringBoot
        SpringApplication.run(ContoDemoApplication.class, args);
    }

    @PostConstruct
	public void deployVerticle() {

		final VertxOptions vertOptions = new VertxOptions();
		vertOptions.setMaxEventLoopExecuteTime(Long.parseLong(eventLoopExecuteTime));
		Vertx vertx = Vertx.vertx(vertOptions);

        configureInterceptor(vertx);

        // i deploy sono asincroni
		verticleList.stream().forEach(verticle -> {
			vertx.deployVerticle(verticle);
		});

        // stessa cosa di sopra ma con le lambda
//		vertx.deployVerticle(socketServerVerticle, stringAsyncResult -> {
//			System.out.println("socketServerVerticle deployment complete");
//		});
        // per fare in modo che quando venga chiuso SpringBoot, venga chiuso anche il contesto vert.x e tutti i verticle
        // deplotati altrimenti rimangono attivi
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                LOGGER.info("shutdown");
                vertx.deploymentIDs().forEach(vertx::undeploy);
                vertx.close();
            }
        });

    }

    public static DeliveryOptions getDefaultDeliverOptions(){
        return new DeliveryOptions().setSendTimeout(10000);
    }

    private void configureInterceptor(Vertx vertx) {
        vertx.eventBus().addOutboundInterceptor(event -> {
			String requestId = ContextualData.get("requestId");
			if (requestId != null) {
				event.message().headers().add("requestId", requestId);
			}
			event.next();
		});
		
		vertx.eventBus().addInboundInterceptor(event -> {
			String requestId = event.message().headers().get("requestId");
			if (requestId != null) {
				ContextualData.put("requestId", requestId);
			}
			event.next();
		});
    }
}
