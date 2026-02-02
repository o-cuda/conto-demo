package it.demo.fabrick.unit.verticle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import it.demo.fabrick.vertx.GestisciRequestVerticle;

/**
 * Unit tests for GestisciRequestVerticle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GestisciRequestVerticle Tests")
class GestisciRequestVerticleTest {

	@Mock
	private Vertx vertx;

	@Mock
	private EventBus eventBus;

	@InjectMocks
	private GestisciRequestVerticle verticle;

	@BeforeEach
	void setUp() {
		when(vertx.eventBus()).thenReturn(eventBus);
	}

	// ==================== start() Tests ====================

	@Test
	@DisplayName("start - should subscribe to gestisci-chiamata-bus")
	void testStart_subscribesToEventBus() throws Exception {
		Promise<Void> startPromise = Promise.promise();

		verticle.start(startPromise);

		verify(eventBus).consumer(any(), any());
	}
}
