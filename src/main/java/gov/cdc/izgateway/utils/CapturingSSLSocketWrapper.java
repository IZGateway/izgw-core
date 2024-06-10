package gov.cdc.izgateway.utils;


import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.SocketChannel;

/**
 * This is a wrapper around an SSL Socket so that content can be inspected
 * on the console.
 * 
 * @author boonek
 */
public class CapturingSSLSocketWrapper extends SSLSocket {

	private final SSLSocket base;
	
	public CapturingSSLSocketWrapper(SSLSocket base) {
		this.base = base;
	}
	
	@Override
	public void addHandshakeCompletedListener(HandshakeCompletedListener arg0) {
		base.addHandshakeCompletedListener(arg0);
	}

	@Override
	public boolean getEnableSessionCreation() {
		return base.getEnableSessionCreation();
	}

	@Override
	public String[] getEnabledCipherSuites() {
		return base.getEnabledCipherSuites();
	}

	@Override
	public String[] getEnabledProtocols() {
		return base.getEnabledProtocols();
	}

	@Override
	public boolean getNeedClientAuth() {
		return base.getNeedClientAuth();
	}

	@Override
	public SSLSession getSession() {
		return base.getSession();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return base.getSupportedCipherSuites();
	}

	@Override
	public String[] getSupportedProtocols() {
		return base.getSupportedProtocols();
	}

	@Override
	public boolean getUseClientMode() {
		return base.getUseClientMode();
	}

	@Override
	public boolean getWantClientAuth() {
		return base.getWantClientAuth();
	}

	@Override
	public void removeHandshakeCompletedListener(HandshakeCompletedListener arg0) {
		base.removeHandshakeCompletedListener(arg0);
	}

	@Override
	public void setEnableSessionCreation(boolean arg0) {
		base.setEnableSessionCreation(arg0);
	}

	@Override
	public void setEnabledCipherSuites(String[] arg0) {
		base.setEnabledCipherSuites(arg0);
	}

	@Override
	public void setEnabledProtocols(String[] arg0) {
		base.setEnabledProtocols(arg0);
	}

	@Override
	public void setNeedClientAuth(boolean arg0) {
		base.setNeedClientAuth(arg0);
	}

	@Override
	public void setUseClientMode(boolean arg0) {
		base.setUseClientMode(arg0);
	}

	@Override
	public void setWantClientAuth(boolean arg0) {
		base.setWantClientAuth(arg0);
	}

	@Override
	public void startHandshake() throws IOException {
		base.startHandshake();
	}
	

	@Override
	public OutputStream getOutputStream() throws IOException {
		final OutputStream out = base.getOutputStream();
		return new DuplicatingOutputStream(out, System.err);
	}
	
	// Socket base handling
	// Same as CapturingSocketWrapper but cannot inherit from multiple classes in Java

	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		base.connect(endpoint);
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		base.connect(endpoint, timeout);
	}

	@Override
	public boolean equals(Object endpoint) {
		return base.equals(endpoint);
	}

	@Override
	public SocketChannel getChannel() {
		return base.getChannel();
	}

	@Override
	public InetAddress getInetAddress() {
		return base.getInetAddress();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new DuplicatingInputStream(base.getInputStream(), System.err);
	}

	@Override
	public boolean getKeepAlive() throws SocketException {
		return base.getKeepAlive();
	}

	@Override
	public InetAddress getLocalAddress() {
		return base.getLocalAddress();
	}

	@Override
	public int getLocalPort() {
		return base.getLocalPort();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return base.getLocalSocketAddress();
	}

	@Override
	public boolean getOOBInline() throws SocketException {
		return base.getOOBInline();
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		return base.getOption(name);
	}

	@Override
	public int getPort() {
		return base.getPort();
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return base.getRemoteSocketAddress();
	}

	@Override
	public boolean getReuseAddress() throws SocketException {
		return base.getReuseAddress();
	}

	@Override
	public int getSendBufferSize() throws SocketException {
		return base.getSendBufferSize();
	}

	@Override
	public int getSoLinger() throws SocketException {
		return base.getSoLinger();
	}

	@Override
	public int getSoTimeout() throws SocketException {
		return base.getSoTimeout();
	}

	@Override
	public boolean getTcpNoDelay() throws SocketException {
		return base.getTcpNoDelay();
	}

	@Override
	public int getTrafficClass() throws SocketException {
		return base.getTrafficClass();
	}

	@Override
	public int hashCode() {
		return base.hashCode();
	}

	@Override
	public boolean isBound() {
		return base.isBound();
	}

	@Override
	public boolean isClosed() {
		return base.isClosed();
	}

	@Override
	public boolean isConnected() {
		return base.isConnected();
	}

	@Override
	public boolean isInputShutdown() {
		return base.isInputShutdown();
	}

	@Override
	public boolean isOutputShutdown() {
		return base.isOutputShutdown();
	}

	@Override
	public void sendUrgentData(int data) throws IOException {
		base.sendUrgentData(data);
	}

	@Override
	public void setKeepAlive(boolean keepAlive) throws SocketException {
		base.setKeepAlive(keepAlive);
	}

	@Override
	public void setOOBInline(boolean oobInline) throws SocketException {
		base.setOOBInline(oobInline);
	}

	@Override
	public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
		return base.setOption(name, value);
	}

	@Override
	public void setPerformancePreferences(int connectTime, int latency, int basewidth) {
		base.setPerformancePreferences(connectTime, latency, basewidth);
	}

	@Override
	public void setReceiveBufferSize(int size) throws SocketException {
		base.setReceiveBufferSize(size);
	}

	@Override
	public void setReuseAddress(boolean on) throws SocketException {
		base.setReuseAddress(on);
	}

	@Override
	public void setSendBufferSize(int size) throws SocketException {
		base.setSendBufferSize(size);
	}

	@Override
	public void setSoLinger(boolean on, int linger) throws SocketException {
		base.setSoLinger(on, linger);
	}

	@Override
	public void setTcpNoDelay(boolean on) throws SocketException {
		base.setTcpNoDelay(on);
	}

	@Override
	public void setTrafficClass(int tc) throws SocketException {
		base.setTrafficClass(tc);
	}

	@Override
	public String toString() {
		return base.toString();
	}
	
}
