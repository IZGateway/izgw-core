package gov.cdc.izgateway.utils;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public final class CapturingSSLSocketFactory extends SSLSocketFactory {
	private final SSLSocketFactory factory;

	public CapturingSSLSocketFactory(SSLSocketFactory factory) {
		this.factory = factory;
	}

	@Override
	public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3) throws IOException {
		return new CapturingSSLSocketWrapper((SSLSocket)factory.createSocket(arg0, arg1, arg2, arg3));
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return factory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return factory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
		Socket s = factory.createSocket(arg0, arg1);
		return new CapturingSSLSocketWrapper((SSLSocket)s);
	}

	@Override
	public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
		Socket s = factory.createSocket(arg0, arg1);
		return new CapturingSSLSocketWrapper((SSLSocket)s);
	}

	@Override
	public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3)
			throws IOException, UnknownHostException {
		Socket s = factory.createSocket(arg0, arg1, arg2, arg3);
		return new CapturingSSLSocketWrapper((SSLSocket)s);
	}

	@Override
	public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
		Socket s = factory.createSocket(arg0, arg1, arg2, arg3);
		return new CapturingSSLSocketWrapper((SSLSocket)s);
	}
}