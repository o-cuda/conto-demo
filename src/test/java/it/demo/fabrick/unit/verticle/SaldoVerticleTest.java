package it.demo.fabrick.unit.verticle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

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
import it.demo.fabrick.vertx.SaldoVerticle;

/**
 * Unit tests for SaldoVerticle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SaldoVerticle Tests")
class SaldoVerticleTest {

	@Mock
	private Vertx vertx;

	@Mock
	private EventBus eventBus;

	@InjectMocks
	private SaldoVerticle verticle;

	private static final String TEST_API_KEY = "test-api-key-12345";

	@BeforeEach
	void setUp() throws Exception {
		// Set apiKey using reflection since it's @Value injected
		Field apiKeyField = SaldoVerticle.class.getDeclaredField("apiKey");
		apiKeyField.setAccessible(true);
		apiKeyField.set(verticle, TEST_API_KEY);

		when(vertx.eventBus()).thenReturn(eventBus);
	}

	// ==================== start() Tests ====================

	@Test
	@DisplayName("start - should subscribe to saldo_bus")
	void testStart_subscribesToSaldoBus() throws Exception {
		Promise<Void> startPromise = Promise.promise();

		verticle.start(startPromise);

		verify(eventBus).consumer(any(), any());
	}
}
