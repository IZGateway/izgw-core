package gov.cdc.izgateway.security.crypto;

import java.nio.charset.StandardCharsets;

public class TestKeyProvider extends KeyProviderBase implements KeyProvider {
    private static int callCount = 0;
    private static final String BASE_KEY = "MySecretKeyForTestingPurposes12";

    public TestKeyProvider() {
    }

    public void rotateKey() {
        // This method can be used to reset the call count if needed
        callCount++;
    }

    @Override
    public byte[] loadKey() throws CryptoException {
        int suffix = (callCount % 3) + 1;

        String key = BASE_KEY + suffix;
        return key.getBytes(StandardCharsets.UTF_8);
    }

}