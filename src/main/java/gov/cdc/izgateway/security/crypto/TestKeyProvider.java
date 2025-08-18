package gov.cdc.izgateway.security.crypto;

import java.nio.charset.StandardCharsets;

/**
 * Test implementation of the KeyProvider interface for unit testing and development.
 * <p>
 * This provider generates deterministic test keys that can be rotated to simulate
 * key rotation scenarios in a controlled testing environment. It cycles through
 * three different keys based on an internal call counter, allowing tests to
 * verify encryption/decryption behavior across multiple key versions.
 * </p>
 * <p>
 * <strong>Warning:</strong> This provider uses predictable, hardcoded keys and
 * should NEVER be used in production environments. It is intended solely for
 * unit testing and development purposes.
 * </p>
 *
 * @author CDC IZ Gateway Team
 * @since 1.0
 */
class TestKeyProvider extends KeyProviderBase implements KeyProvider {
    private static int callCount = 0;
    private static final String BASE_KEY = "MySecretKeyForTestingPurposes12";

    static void rotateKey() {
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