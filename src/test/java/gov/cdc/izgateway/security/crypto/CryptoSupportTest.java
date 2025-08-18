package gov.cdc.izgateway.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CryptoSupportTest {

    private static final String TEST_PLAINTEXT = "Hello, World!";
    private KeyProvider originalProvider;
    private static TestKeyProvider testKeyProvider;

    @BeforeEach
    void setUp() {
        CryptoSupport.initialize();
        // Inject test key provider
        testKeyProvider = new TestKeyProvider();
        CryptoSupport.setKeyProvider(testKeyProvider);
    }

    @Test
    void testEncryptDecrypt() throws CryptoException {
        String encrypted = CryptoSupport.encrypt(TEST_PLAINTEXT, testKeyProvider.loadKey());
        String decrypted = CryptoSupport.decrypt(encrypted);

        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);
        assertEquals(decrypted, TEST_PLAINTEXT);
    }

    @Test
    void testEncryptDecryptWithKeyRotation() throws CryptoException {
        String encrypted = CryptoSupport.encrypt(TEST_PLAINTEXT, testKeyProvider.loadKey());
        String decrypted = CryptoSupport.decrypt(encrypted);

        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);
        assertEquals(decrypted, TEST_PLAINTEXT);

        // Rotate the key
        testKeyProvider.rotateKey();
        byte[] newKey = testKeyProvider.loadKey();
        if (!testKeyProvider.keyExists(newKey)) {
            testKeyProvider.addKeyToHistory(newKey);
        }

        String encrypted2 = CryptoSupport.encrypt(TEST_PLAINTEXT, testKeyProvider.loadKey());
        String decrypted2 = CryptoSupport.decrypt(encrypted2);

        System.out.println("Encrypted2: " + encrypted2);
        System.out.println("Decrypted2: " + decrypted2);
        assertEquals(decrypted2, TEST_PLAINTEXT);

        // Attempt to decrypt with the old key
        String decryptedOld = CryptoSupport.decrypt(encrypted);
        assertEquals(decryptedOld, TEST_PLAINTEXT);
    }

}


