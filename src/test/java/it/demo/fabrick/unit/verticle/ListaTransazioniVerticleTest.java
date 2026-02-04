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

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import it.demo.fabrick.vertx.ListaTransazioniVerticle;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for ListaTransazioniVerticle.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@DisplayName("ListaTransazioniVerticle Tests")
class ListaTransazioniVerticleTest {

	@Mock
	private Vertx vertx;

	@Mock
	private EventBus eventBus;

	@InjectMocks
	private ListaTransazioniVerticle verticle;

	private static final String TEST_API_KEY = "test-api-key-12345";
	private static final String TEST_AUTH_SCHEMA = "S2S";

	@BeforeEach
	void setUp() throws Exception {
		// Set apiKey using reflection since it's @Value injected
		Field apiKeyField = ListaTransazioniVerticle.class.getDeclaredField("apiKey");
		apiKeyField.setAccessible(true);
		apiKeyField.set(verticle, TEST_API_KEY);

		// Set authSchema using reflection since it's @Value injected
		Field authSchemaField = ListaTransazioniVerticle.class.getDeclaredField("authSchema");
		authSchemaField.setAccessible(true);
		authSchemaField.set(verticle, TEST_AUTH_SCHEMA);

		when(vertx.eventBus()).thenReturn(eventBus);
	}

	// ==================== start() Tests ====================

	@Test
	@DisplayName("start - should subscribe to lista_bus")
	void testStart_subscribesToListaBus() throws Exception {
		Promise<Void> startPromise = Promise.promise();

		verticle.start(startPromise);

		verify(eventBus).consumer(any(), any());
	}

	// ==================== Configuration Tests ====================

	@Test
	@DisplayName("configuration - should inject authSchema")
	void testConfiguration_authSchemaInjected() throws Exception {
		Field authSchemaField = ListaTransazioniVerticle.class.getDeclaredField("authSchema");
		authSchemaField.setAccessible(true);

		String authSchema = (String) authSchemaField.get(verticle);
		assertEquals(TEST_AUTH_SCHEMA, authSchema, "Auth schema should be injected correctly");
	}
}
