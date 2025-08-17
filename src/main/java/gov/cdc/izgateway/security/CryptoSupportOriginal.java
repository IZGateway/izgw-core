package gov.cdc.izgateway.security;

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
import java.util.Optional;

/**
 * Cryptographic support for encryption and decryption of sensitive data.
 * 
 * @author Audacious Inquiry
 */
public class CryptoSupportOriginal {
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
    private static final byte[] keyBytes = loadKeyBytes();

    /**
     * Encrypts the given plain text using AES-GCM with a random IV.
     * 
     * @param plainText	the text to encrypt
     * @return	the encrypted text, base64-encoded and prefixed with "=="
     * @throws Exception	if an error occurs during encryption
     */
    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty() || plainText.startsWith("==")) {
            return plainText;
        }

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
    }

    /**
     * Decrypts the given encrypted text using AES-GCM.
     * @param encryptedText	the text to decrypt, base64-encoded and prefixed with "=="
     * @return	the decrypted plain text
     * @throws Exception	if an error occurs during decryption
     */
    public static String decrypt(String encryptedText) throws CryptoException {
    	
        if (encryptedText == null || !encryptedText.startsWith("==")) {
            return encryptedText;
        }

        try {
            byte[] data = Base64.decode(encryptedText.substring(2));
            byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(data, IV_LENGTH, data.length);

            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BCFIPS");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] decrypted = cipher.doFinal(encrypted, 0, encrypted.length);
            String tempString = new String(decrypted, StandardCharsets.UTF_8);
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

		System.out.println("Original: " + originalText);   // NOSONAR
		System.out.println("Encrypted: " + encryptedText); // NOSONAR
		System.out.println("Decrypted: " + decryptedText); // NOSONAR
		encryptedText = "==UWRjW60RQkddEmaEDh0bAKTdCFbMi/zem5T1o55mXJ/pSjgTRM9ByCdPR/Yn";
		decryptedText = decrypt(encryptedText);
		System.out.println("Decrypted: " + decryptedText); // NOSONAR
    }
    
    /**
     * Loads the AES key from AWS Secrets Manager if configured, otherwise uses the default.
     * The secret name can be set via the environment variable 'ENCRYPTION_KEY_SECRET_NAME'.
     * The secret value should be a base64-encoded 32-byte key.
     */
    private static byte[] loadKeyBytes() throws SdkClientException {
        String secretName = "izgw-dev-password-encryption-key";
        if (StringUtils.isEmpty(secretName)) {
            throw new IllegalArgumentException("ENCRYPTION_KEY_SECRET_NAME environment variable is not set.");
        }

        Region region = Region.of(Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-1"));
        SecretsManagerClient client = SecretsManagerClient.builder().region(region).build();
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
        GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        String secret = getSecretValueResponse.secretString();
        if (!StringUtils.isEmpty(secret)) {
            byte[] decoded = secret.getBytes();
            if (decoded.length == KEY_LENGTH) {
                return decoded;
            } else {
                throw new IllegalArgumentException("Secret key length is invalid. Expected 32 bytes.");
            }
        } else {
            throw new IllegalArgumentException("Secret value is empty.");
        }
    }
}