package gov.cdc.izgateway.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DuplicatingInputStream extends InputStream {
	private static final byte[] BLUE = { '\033', '[', '3', '4', 'm' };
	private static final byte[] NORMAL = { '\033', '[', '0', 'm' };
	private InputStream in;
	private OutputStream out;
	
	public DuplicatingInputStream(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	@Override
	public int read() throws IOException {
		int c = in.read();
		if (c >= 0) {
			try {
				out.write(BLUE);
				out.write((byte)c);
				out.write(NORMAL);
			} catch (IOException ex) {
				// Swallow it
			}
		}
		return c;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int val = in.read(b, off, len);
		if (val > 0) {
			try {
				out.write(BLUE);
				out.write(b, off, val);
				out.write(NORMAL);
			} catch (IOException ex) {
				// Swallow it.
			}
		}
		return val;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
}
