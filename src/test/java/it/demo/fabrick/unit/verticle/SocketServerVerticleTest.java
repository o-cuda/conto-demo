package it.demo.fabrick.unit.verticle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import it.demo.fabrick.vertx.SocketServerVerticle;

/**
 * Unit tests for SocketServerVerticle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SocketServerVerticle Tests")
class SocketServerVerticleTest {

	@Mock
	private Vertx vertx;

	@Mock
	private EventBus eventBus;

	@Mock
	private NetServer netServer;

	@InjectMocks
	private SocketServerVerticle verticle;

	@BeforeEach
	void setUp() {
		when(vertx.createNetServer(any(NetServerOptions.class))).thenReturn(netServer);
		when(netServer.connectHandler(any())).thenReturn(netServer);
		when(netServer.listen(any(io.vertx.core.Handler.class))).thenReturn(netServer);
	}

	// ==================== Basic tests ====================

	@Test
	@DisplayName("start - verticle should initialize without exceptions")
	void testStart_noExceptions() throws Exception {
		verticle.start();
	}
}
