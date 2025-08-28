package gov.cdc.izgateway.security.crypto;

import org.bouncycastle.util.Arrays;

public class ByteArrayWrapper {
    private final byte[] data;
    private final int hashCode;

    public ByteArrayWrapper(byte[] data) {
        this.data = data.clone(); // Defensive copy
        this.hashCode = Arrays.hashCode(data);
    }

    public byte[] getData() {
        return data.clone(); // Defensive copy
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ByteArrayWrapper that = (ByteArrayWrapper) obj;
        return java.util.Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
