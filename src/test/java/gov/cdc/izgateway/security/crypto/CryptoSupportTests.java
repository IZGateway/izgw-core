package gov.cdc.izgateway.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoSupportTests {

    private static final String TEST_PLAINTEXT = "Hello, World!";
    private TestKeyProvider testKeyProvider;

    @BeforeEach
    void setUp() {
        CryptoSupport.initialize();

        // Inject test key provider
        testKeyProvider = new TestKeyProvider();
        CryptoSupport.setKeyProvider(testKeyProvider);
    }

    @Test
    void testEncryptDecrypt() throws CryptoException {
        byte[] key = testKeyProvider.loadKey();
        String encrypted = CryptoSupport.encrypt(TEST_PLAINTEXT, key);
        String decrypted = CryptoSupport.decrypt(encrypted);

        assertEquals(TEST_PLAINTEXT, decrypted, "Decrypted text should match original");
    }

    @Test
    void testEncryptDecryptWithKeyRotation() throws CryptoException {
        // Encrypt with initial key
        byte[] originalKey = testKeyProvider.loadKey();
        String encrypted = CryptoSupport.encrypt(TEST_PLAINTEXT, originalKey);
        String decrypted = CryptoSupport.decrypt(encrypted);
        assertEquals(TEST_PLAINTEXT, decrypted, "Initial decryption should work");

        // Rotate the key
        testKeyProvider.rotateKey();
        byte[] newKey = testKeyProvider.loadKey();
        assertFalse(java.util.Arrays.equals(originalKey, newKey), "New key should differ from original");

        // Encrypt with new key
        String encryptedWithNewKey = CryptoSupport.encrypt(TEST_PLAINTEXT, newKey);
        String decryptedWithNewKey = CryptoSupport.decrypt(encryptedWithNewKey);
        assertEquals(TEST_PLAINTEXT, decryptedWithNewKey, "Decryption with new key should work");

        // Verify old encrypted data can still be decrypted (key history)
        String decryptedOld = CryptoSupport.decrypt(encrypted);
        assertEquals(TEST_PLAINTEXT, decryptedOld, "Old encrypted data should still be decryptable");
    }

    @Test
    void testEncryptWithEmptyInput() throws CryptoException {
        byte[] key = testKeyProvider.loadKey();
        String encrypted = CryptoSupport.encrypt("", key);
        String decrypted = CryptoSupport.decrypt(encrypted);
        assertEquals("", decrypted, "Empty string should encrypt and decrypt correctly");
    }

}