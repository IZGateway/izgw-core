package gov.cdc.izgateway.soap.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

/**
 * This class converts an HttpURLConnection into an HttpInputMessage to work with SpringBoot Message Converters.
 */
final class HttpUrlConnectionInputMessage implements HttpInputMessage {
	private final HttpURLConnection con;
	private final BufferedInputStream is;
	private final HttpHeaders headers = new HttpHeaders();
	private final int bufferSize;
	private final int statusCode;
	
	HttpUrlConnectionInputMessage(HttpURLConnection con, int maxBufferSize) throws IOException {
		this.con = con;
		long contentLength = con.getContentLengthLong();
		if (contentLength < 0) {
			contentLength = maxBufferSize;
		}
		
		// 1. Add some slop to the expressed length just in case, and 2. cast to int is OK, because maxBufferSize is an int.
		this.bufferSize = (int) Math.min(contentLength + 1024, maxBufferSize);
		this.statusCode = con.getResponseCode();
		this.is = new BufferedInputStream(statusCode < HttpURLConnection.HTTP_BAD_REQUEST ? con.getInputStream() : con.getErrorStream(), bufferSize);
	}
	
	@Override
	public HttpHeaders getHeaders() {
		if (headers.isEmpty()) {
			con.getHeaderFields().entrySet().forEach(
				field -> headers.addAll(field.getKey(), field.getValue())
			);
		}
		return headers;
	}

	@Override
	public InputStream getBody() {
		return is;
	}

	public void mark() {
		is.mark(bufferSize);
	}

	public void reset() {
		try {
			is.reset();
		} catch (IOException ex) {
			// Ignore it.
		}
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}