package gov.cdc.izgateway.utils;

import java.io.IOException;
import java.io.OutputStream;

final class DuplicatingOutputStream extends OutputStream {
	private final OutputStream out;
	private final OutputStream dup;

	DuplicatingOutputStream(OutputStream out, OutputStream dup) {
		this.out = out;
		this.dup = dup;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
		dup.write(b);
	}

	public void write(byte b[]) throws IOException {
	    write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) throws IOException {
		out.write(b, off, len);
		dup.write(b, off, len);
	}

	public void flush() throws IOException {
		out.flush();
		dup.flush();
	}

	public void close() throws IOException {
		out.close();
	}
}