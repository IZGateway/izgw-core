package gov.cdc.izgateway.security.crypto;

import java.util.List;

public interface KeyProvider {
    byte[] loadKey() throws CryptoException;
    List<byte[]> getAllKeys();
    boolean keyExists(byte[] keyBytes);
    void addKeyToHistory(byte[] keyBytes);
}
