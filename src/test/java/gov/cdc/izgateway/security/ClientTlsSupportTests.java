package gov.cdc.izgateway.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;

/**
 * Regression coverage for IGDD-3582: {@link ClientTlsSupport#sslReloadConnectorCustomizer()}
 * must wire the embedded HTTPS connector's real protocol handler into
 * {@link ClientTlsSupport.SslReloader}, closing the dead-wiring gap where
 * {@code SslReloader.setProtocol(...)} was never called by anything.
 */
@ExtendWith(MockitoExtension.class)
class ClientTlsSupportTests {

	@Mock
	private ClientTlsConfiguration config;

	private TomcatConnectorCustomizer customizer() {
		return new ClientTlsSupport(config).sslReloadConnectorCustomizer();
	}

	@Test
	void httpsConnectorIsWiredToSslImplementationAndReloader() {
		Connector connector = mock(Connector.class);
		AbstractHttp11Protocol<?> handler = mock(AbstractHttp11Protocol.class);
		when(connector.getScheme()).thenReturn("https");
		when(connector.getProtocolHandler()).thenReturn(handler);

		customizer().customize(connector);

		verify(handler).setSslImplementationName(SSLImplementation.class.getName());

		// SslReloader.protocol has no getter by design (it's an internal accumulator) --
		// confirm it captured this exact handler by observing reloadSsl() delegate to it.
		ClientTlsSupport.SslReloader.reloadSsl();
		verify(handler).reloadSslHostConfigs();
	}

	@Test
	void nonHttpsConnectorIsIgnored() {
		Connector connector = mock(Connector.class);
		when(connector.getScheme()).thenReturn("http");

		customizer().customize(connector);

		verify(connector, never()).getProtocolHandler();
	}

	@Test
	void httpsConnectorWithNonHttp11ProtocolHandlerIsIgnored() {
		Connector connector = mock(Connector.class);
		ProtocolHandler handler = mock(ProtocolHandler.class);
		when(connector.getScheme()).thenReturn("https");
		when(connector.getProtocolHandler()).thenReturn(handler);

		// Must not throw a ClassCastException, and must not call any AbstractHttp11Protocol-only
		// method on a handler that isn't one.
		customizer().customize(connector);
	}
}
