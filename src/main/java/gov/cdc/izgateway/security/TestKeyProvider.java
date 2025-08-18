package gov.cdc.izgateway.security;

import java.nio.charset.StandardCharsets;

//public class TestKeyProvider implements KeyProvider {
//
//
//    public TestKeyProvider() {
//    }
//
//    @Override
//    public byte[] loadKey() throws CryptoException {
//        // Return "MySecretKeyForTestingPurposes12" + 1, 2, or 3 to make it 32 bytes
//        // This is a placeholder for testing purposes only.
//
//        return "MySecretKeyForTestingPurposes123".getBytes(StandardCharsets.UTF_8);
//    }
//}


public class TestKeyProvider implements KeyProvider {
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