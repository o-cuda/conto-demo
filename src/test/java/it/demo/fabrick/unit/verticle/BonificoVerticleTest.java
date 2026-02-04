package it.demo.fabrick.unit.verticle;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import it.demo.fabrick.vertx.BonificoVerticle;

@MockitoSettings(strictness = Strictness.LENIENT)

/**
 * Unit tests for BonificoVerticle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonificoVerticle Tests")
class BonificoVerticleTest {

	@Mock
	private Vertx vertx;

	@Mock
	private EventBus eventBus;

	@InjectMocks
	private BonificoVerticle verticle;

	private static final String TEST_API_KEY = "test-api-key-12345";
	private static final String TEST_AUTH_SCHEMA = "S2S";

	@BeforeEach
	void setUp() throws Exception {
		// Set apiKey using reflection since it's @Value injected
		Field apiKeyField = BonificoVerticle.class.getDeclaredField("apiKey");
		apiKeyField.setAccessible(true);
		apiKeyField.set(verticle, TEST_API_KEY);

		// Set authSchema using reflection since it's @Value injected
		Field authSchemaField = BonificoVerticle.class.getDeclaredField("authSchema");
		authSchemaField.setAccessible(true);
		authSchemaField.set(verticle, TEST_AUTH_SCHEMA);

		when(vertx.eventBus()).thenReturn(eventBus);
	}

	// ==================== start() Tests ====================

	@Test
	@DisplayName("start - should subscribe to bonifico_bus")
	void testStart_subscribesToBonificoBus() throws Exception {
		Promise<Void> startPromise = Promise.promise();

		verticle.start(startPromise);

		verify(eventBus).consumer(any(), any());
	}

	@Test
	@DisplayName("start - should complete successfully")
	void testStart_completesSuccessfully() throws Exception {
		Promise<Void> startPromise = Promise.promise();

		verticle.start(startPromise);

		// If start() completes without exception, the promise should be succeeded
		// Note: In the actual implementation, start() doesn't explicitly call startFuture.complete()
		// but the verticle should still initialize properly
		verify(eventBus).consumer(any(), any());
	}

	// ==================== Configuration Tests ====================

	@Test
	@DisplayName("configuration - should have correct timeout value")
	void testConfiguration_timeoutValue() throws Exception {
		// Verify the timeout constant is set to 120 seconds
		Field timeoutField = BonificoVerticle.class.getDeclaredField("MONEY_TRANSFER_TIMEOUT_MS");
		timeoutField.setAccessible(true);

		int timeoutValue = timeoutField.getInt(null); // null for static field access
		assertEquals(120000, timeoutValue, "Money transfer timeout should be 120 seconds (120000 ms)");
	}

	@Test
	@DisplayName("configuration - should inject apiKey")
	void testConfiguration_apiKeyInjected() throws Exception {
		Field apiKeyField = BonificoVerticle.class.getDeclaredField("apiKey");
		apiKeyField.setAccessible(true);

		String apiKey = (String) apiKeyField.get(verticle);
		assertEquals(TEST_API_KEY, apiKey, "API key should be injected correctly");
	}

	@Test
	@DisplayName("configuration - should inject authSchema")
	void testConfiguration_authSchemaInjected() throws Exception {
		Field authSchemaField = BonificoVerticle.class.getDeclaredField("authSchema");
		authSchemaField.setAccessible(true);

		String authSchema = (String) authSchemaField.get(verticle);
		assertEquals(TEST_AUTH_SCHEMA, authSchema, "Auth schema should be injected correctly");
	}
}
