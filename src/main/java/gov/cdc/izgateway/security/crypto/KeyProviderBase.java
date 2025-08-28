package gov.cdc.izgateway.security.crypto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class KeyProviderBase {
    private final Set<ByteArrayWrapper> keyHistory = new LinkedHashSet<>();

    public List<byte[]> getAllKeys() {
        synchronized (TestKeyProvider.class) {
            return keyHistory.stream()
                    .map(ByteArrayWrapper::getData)
                    .toList();
        }
    }

    public void addKeyToHistory(byte[] keyBytes) {
        synchronized (TestKeyProvider.class) {
            keyHistory.add(new ByteArrayWrapper(keyBytes));
        }
    }

    // Check if key exists
    public boolean keyExists(byte[] keyBytes) {
        synchronized (CryptoSupport.class) {
            return keyHistory.contains(new ByteArrayWrapper(keyBytes));
        }
    }
}
