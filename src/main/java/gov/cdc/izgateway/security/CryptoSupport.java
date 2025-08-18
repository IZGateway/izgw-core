package gov.cdc.izgateway.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cryptographic support for encryption and decryption of sensitive data.
 * 
 * @author Audacious Inquiry
 */
@Slf4j
public class CryptoSupport {
    private static final String PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME = "PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME";
	/** Length in bytes of the Initialization Vector */
	private static final int IV_LENGTH = 16;
	/** Length in bytes of the Authentication Tag */
	private static final int TAG_LENGTH = 16;
	/** Length in bytes of the Key */
    private static final int KEY_LENGTH = 32;
	/** The Cipher Algorithm to Use */
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    /** A secure random number generator */
    private static final SecureRandom secureRandom = getSecureRandom();
    // Key is now loaded from AWS Secrets Manager if available
    private static final Set<ByteArrayWrapper> keyHistory = new LinkedHashSet<>();

    private static KeyProvider keyProvider = new AwsSecretsManagerKeyProvider(); // Default

    // Add setter for dependency injection (package-private for testing)
    static void setKeyProvider(KeyProvider provider) {
        keyProvider = provider;
    }

    public static String encrypt(String plainText) throws CryptoException {
        for (byte[] key : getAllKeys()) {
            try {
                return encrypt(plainText, key);
            } catch (CryptoException e) {
                // Log and continue to next key
                log.error("Encryption failed with key");
            }
        }

        // Attempt to get the key from AWS Secrets Manager
        try {
            byte[] keyBytes = keyProvider.loadKey();
            if (!keyExists(keyBytes)) {
                addKeyToHistory(keyBytes);
                return encrypt(plainText, keyBytes);
            } else {
                throw new CryptoException("Encryption failed with all available keys.");
            }
        } catch (CryptoException e) {
            throw new CryptoException("Failed to encrypt with all available keys.", e);
        }
    }

    /**
     * Encrypts the given plain text using AES-GCM with a random IV.
     * 
     * @param plainText	the text to encrypt
     * @return	the encrypted text, base64-encoded and prefixed with "=="
     * @throws CryptoException	if an error occurs during encryption
     */
    private static String encrypt(String plainText, byte[] keyBytes) throws CryptoException {
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

    public static String decrypt(String encryptedText) throws CryptoException {
        if (encryptedText == null || !encryptedText.startsWith("==")) {
            return encryptedText;
        }

        for (byte[] key : getAllKeys()) {
            try {
                return decrypt(encryptedText, key);
            } catch (CryptoException e) {
                // Log and continue to next key
                log.error("Decryption failed with a key from history, trying next if available.", e);
            }
        }

        // Attempt to get the key from AWS Secrets Manager
        try {
            byte[] keyBytes = keyProvider.loadKey();
            if (!keyExists(keyBytes)) {
                addKeyToHistory(keyBytes);
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
    
    /**
     * Small verification main
     * @param args
     * @throws Exception
     */
    public static void main(String ... args) throws Exception {
    	initialize();
		String originalText = "Hello, World!";
		String encryptedText = encrypt(originalText);
		String decryptedText = decrypt(encryptedText);

        log.info("Original: {}", originalText);   // NOSONAR
        log.info("Encrypted: {}", encryptedText); // NOSONAR
        log.info("Decrypted: {}", decryptedText); // NOSONAR
		encryptedText = "==FI0+iBynP/FWea18NeeZ0XY43cNtlgPb3V6zvwRKP99G9Lyr/SQo9yY59kLO";
		decryptedText = decrypt(encryptedText);
        log.info("Decrypted: {}", decryptedText); // NOSONAR
    }

    private static String getEncryptionKeySecretName() {
        return System.getenv().getOrDefault(PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME, "izgw-dev-password-encryption-key");
    }


    // Add a key
    private static void addKeyToHistory(byte[] keyBytes) {
        synchronized (CryptoSupport.class) {
            keyHistory.add(new ByteArrayWrapper(keyBytes));
            // Optional: limit size to prevent memory issues
            if (keyHistory.size() > 10) {
                Iterator<ByteArrayWrapper> iterator = keyHistory.iterator();
                iterator.next();
                iterator.remove();
            }
        }
    }

    // Check if key exists
    private static boolean keyExists(byte[] keyBytes) {
        synchronized (CryptoSupport.class) {
            return keyHistory.contains(new ByteArrayWrapper(keyBytes));
        }
    }

    // Get all keys
    private static List<byte[]> getAllKeys() {
        synchronized (CryptoSupport.class) {
            return keyHistory.stream()
                    .map(ByteArrayWrapper::getData)
                    .collect(Collectors.toList());
        }
    }

    private static class ByteArrayWrapper {
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
}