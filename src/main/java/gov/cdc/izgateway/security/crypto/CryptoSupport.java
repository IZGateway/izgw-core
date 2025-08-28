package gov.cdc.izgateway.security.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Cryptographic support for encryption and decryption of sensitive data.
 * 
 * @author Audacious Inquiry
 */
@Slf4j
public class CryptoSupport {
	/** Length in bytes of the Initialization Vector */
	private static final int IV_LENGTH = 16;
	/** Length in bytes of the Authentication Tag */
	private static final int TAG_LENGTH = 16;
	/** The Cipher Algorithm to Use */
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    /** A secure random number generator */
    private static final SecureRandom secureRandom = getSecureRandom();

    private static KeyProvider keyProvider = new AwsSecretsManagerKeyProvider(); // Default

    private CryptoSupport() {
        // Prevent instantiation
    }
    /**
     * Sets the key provider for dependency injection.
     * <p>
     * This method is package-private and primarily intended for testing purposes
     * to inject mock or test key providers.
     * </p>
     *
     * @param provider the key provider to use for loading encryption keys
     */
    static void setKeyProvider(KeyProvider provider) {
        keyProvider = provider;
    }

    /**
     * Encrypts the given plain text using AES-GCM with a random IV.
     * 
     * @param plainText	the text to encrypt
     * @return	the encrypted text, base64-encoded and prefixed with "=="
     * @throws CryptoException	if an error occurs during encryption
     */
    public static String encrypt(String plainText, byte[] keyBytes) throws CryptoException {
        if (plainText == null || plainText.isEmpty() || plainText.startsWith("==")) {
            return plainText;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BCFIPS");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH*8, iv));

            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = cipher.doFinal(input, 0, input.length);

            // Prepend IV to the ciphertext
            byte[] result = Arrays.concatenate(iv, encrypted);

            return "==" + Base64.toBase64String(result);

        } catch(Exception e) {
        	throw new CryptoException("Failed to encrypt.", e);
        }
    }

    /**
     * Decrypts encrypted text using key rotation fallback strategy.
     * <p>
     * This method attempts to decrypt the input using multiple keys to support
     * key rotation scenarios. It first tries all keys from the provider's history,
     * then attempts to load a fresh key if decryption fails.
     * </p>
     * <p>
     * <strong>Decryption Strategy:</strong>
     * <ol>
     *   <li>Try decryption with each key from the key history</li>
     *   <li>If all history keys fail, load the current key from the provider</li>
     *   <li>Add the current key to history if not already present</li>
     *   <li>Attempt final decryption with the current key</li>
     * </ol>
     * </p>
     *
     * @param encryptedText the encrypted text to decrypt (must be prefixed with "==")
     * @return the decrypted plain text
     * @throws CryptoException if decryption fails with all available keys
     */
    public static String decrypt(String encryptedText) throws CryptoException {
        if (encryptedText == null || !encryptedText.startsWith("==")) {
            return encryptedText;
        }

        for (byte[] key : keyProvider.getAllKeys()) {
            try {
                return decrypt(encryptedText, key);
            } catch (CryptoException e) {
                // Log and continue to next key
                log.debug("Decryption failed with a key from history, trying next if available.", e);
            }
        }

        try {
            byte[] keyBytes = keyProvider.loadKey();
            if (!keyProvider.keyExists(keyBytes)) {
                keyProvider.addKeyToHistory(keyBytes);
                return decrypt(encryptedText, keyBytes);
            } else {
                throw new CryptoException("Decryption failed with all available keys.");
            }
        } catch (CryptoException e) {
            throw new CryptoException("Failed to decrypt with all available keys.", e);
        }
    }

    /**
     * Decrypts the given encrypted text using AES-GCM.
     * @param encryptedText	the text to decrypt, base64-encoded and prefixed with "=="
     * @return	the decrypted plain text
     * @throws CryptoException	if an error occurs during decryption
     */
    private static String decrypt(String encryptedText, byte[] keyBytes) throws CryptoException {

        try {
            byte[] data = Base64.decode(encryptedText.substring(2));
            byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(data, IV_LENGTH, data.length);

            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BCFIPS");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH*8, iv));
            byte[] decrypted = cipher.doFinal(encrypted, 0, encrypted.length);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Failed to decrypt.", e);
        }
    }
    
    /**
     * Get a SecureRandom instance using a FIPS 140-2 approved DRBG
     * @return The SecureRandom instance
     */
    public static SecureRandom getSecureRandom() {
		/*
         * According to NIST Special Publication 800-90A, a Nonce is
         * A time-varying value that has at most a negligible chance of
         * repeating, e.g., a random value that is generated anew for each
         * use, a timestamp, a sequence number, or some combination of
         * these.
         *
         * The nonce is combined with the entropy input to create the initial
         * DRBG seed.
         */
        byte[] nonce = ByteBuffer.allocate(8).putLong(System.nanoTime()).array();
        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512
                .fromEntropySource(entSource).setSecurityStrength(256)
                .setEntropyBitsRequired(256);
        return drgbBldr.build(nonce, true);
    }
    
    /**
     * Initialize the BC-FIPS Module as the JCA/JCE provider.
     */
    public static void initialize() {
		CryptoServicesRegistrar.setSecureRandom(getSecureRandom());
		Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
		Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);
	}
}