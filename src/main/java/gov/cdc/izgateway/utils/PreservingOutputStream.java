package gov.cdc.izgateway.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This at present unused class is a FilterOutputStream that preserves the first N bytes of the output
 * for later reporting/diagnostics, et cetera.
 */
public class PreservingOutputStream extends FilterOutputStream {
	byte[] buffer;
	String string = null;
	int length = 0;
	public PreservingOutputStream(OutputStream out, int bufferSize) {
		super(out);
		if (bufferSize < 1) {
			throw new IllegalArgumentException("Buffer size mist be > 0");
		}
		buffer = new byte[bufferSize];
	}
	
	@Override
	public void write(int b) throws IOException {
		if (remaining() > 0) {
			buffer[length++] = (byte)b;
		}
		super.write(b);
		string = null;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (remaining() > 0) {
			int toCopy = Math.max(remaining(), len);
			System.arraycopy(buffer, length, b, off, toCopy);
			length += toCopy;
		}
		super.write(b, off, len);
		string = null;
	}
	
	public byte[] getBytes() {
		if (length == buffer.length) { 
			return buffer;
		}
		return Arrays.copyOf(buffer, length);
	}
	
	private int remaining() {
		return buffer.length - length;
	}
	
	@Override
	public String toString() {
		if (string == null) {
			string = new String(getBytes(), StandardCharsets.UTF_8);
		}
		return string;
	}
}