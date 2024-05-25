package gov.cdc.izgateway.utils;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A memory based output stream that can only hold so much
 * before it fails with an IOException.
 *  
 */
public class FixedByteArrayOutputStream  extends FilterOutputStream {
	private final int maxSize;
	private final ByteArrayOutputStream bos;
	private int currentSize = 0;
	public static final int DEFAULT_SIZE = 8192;
	
	public FixedByteArrayOutputStream(int maxSize) {
		super(new ByteArrayOutputStream());
		bos = (ByteArrayOutputStream) out;
		this.maxSize = maxSize;
	}
	
	public FixedByteArrayOutputStream() {
		this(DEFAULT_SIZE);
	}
	
	public class SizeExceededException extends IOException {
		private static final long serialVersionUID = 1L;

		private SizeExceededException() {
			super(String.format("Maximum size of %d exceeded", maxSize));
		}
		
		public int getSize() {
			return currentSize;
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (++currentSize > maxSize) {
			throw new SizeExceededException();
		}
		super.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if ((currentSize += len) > maxSize) {
			throw new SizeExceededException();
		}
		super.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		super.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
	}
	
	public byte[] toByteArray() {
		return bos.toByteArray();
	}
	
	public String toString() {
		return bos.toString(StandardCharsets.UTF_8);
	}
	
	public void reset() {
		currentSize = 0;
		bos.reset();
	}

}