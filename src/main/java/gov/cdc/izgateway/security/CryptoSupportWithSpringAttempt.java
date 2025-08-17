//package gov.cdc.izgateway.security;
//import org.apache.commons.lang3.StringUtils;
//import org.bouncycastle.crypto.CryptoServicesRegistrar;
//import org.bouncycastle.crypto.EntropySourceProvider;
//import org.bouncycastle.crypto.fips.FipsDRBG;
//import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
//import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
//import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
//import org.bouncycastle.util.Arrays;
//import org.bouncycastle.util.encoders.Base64;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
//import software.amazon.awssdk.core.exception.SdkClientException;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.security.SecureRandom;
//import java.security.Security;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.GCMParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//
///**
// * Cryptographic support for encryption and decryption of sensitive data.
// *
// * @author Audacious Inquiry
// */
//@Component
//public class CryptoSupportWithSpringAttempt {
//	/** Length in bytes of the Initialization Vector */
//	private final int IV_LENGTH = 16;
//	/** Length in bytes of the Authentication Tag */
//	private final int TAG_LENGTH = 16;
//	/** Length in bytes of the Key */
//    private final int KEY_LENGTH = 32;
//	/** The Cipher Algorithm to Use */
//    private final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
//    /** A secure random number generator */
//    private SecureRandom secureRandom;
//    private volatile byte[] keyBytes;
//    private final Object keyLock = new Object();
//
//    private final CryptoConfig cryptoConfig;
//
//    @Autowired
//    public CryptoSupportWithSpringAttempt(CryptoConfig cryptoConfig) {
//        this.cryptoConfig = cryptoConfig;
//        //this.keyBytes = loadKeyBytes();
//        initialize();
//    }
//
//    /**
//     * Encrypts the given plain text using AES-GCM with a random IV.
//     *
//     * @param plainText	the text to encrypt
//     * @return	the encrypted text, base64-encoded and prefixed with "=="
//     * @throws Exception	if an error occurs during encryption
//     */
//    public String encrypt(String plainText) throws CryptoException {
//        if (plainText == null || plainText.isEmpty() || plainText.startsWith("==")) {
//            return plainText;
//        }
//
//        try {
//            byte[] iv = new byte[IV_LENGTH];
//            getSecureRandom().nextBytes(iv);
//
//            SecretKeySpec key = new SecretKeySpec(getKeyBytes(), "AES");
//            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BCFIPS");
//            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH*8, iv));
//
//            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
//            byte[] encrypted = cipher.doFinal(input, 0, input.length);
//
//            // Prepend IV to the ciphertext
//            byte[] result = Arrays.concatenate(iv, encrypted);
//
//            return "==" + Base64.toBase64String(result);
//        } catch(Exception e) {
//            throw new CryptoException("Failed to encrypt.", e);
//        }
//
//    }
//
//    /**
//     * Decrypts the given encrypted text using AES-GCM.
//     * @param encryptedText	the text to decrypt, base64-encoded and prefixed with "=="
//     * @return	the decrypted plain text
//     * @throws Exception	if an error occurs during decryption
//     */
//    public String decrypt(String encryptedText) throws CryptoException {
//
//        if (encryptedText == null || !encryptedText.startsWith("==")) {
//            return encryptedText;
//        }
//
//        try {
//            byte[] data = Base64.decode(encryptedText.substring(2));
//            byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
//            byte[] encrypted = Arrays.copyOfRange(data, IV_LENGTH, data.length);
//
//            SecretKeySpec key = new SecretKeySpec(getKeyBytes(), "AES");
//
//            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BCFIPS");
//            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH*8, iv));
//            byte[] decrypted = cipher.doFinal(encrypted, 0, encrypted.length);
//            return new String(decrypted, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new CryptoException("Failed to decrypt.", e);
//        }
//    }
//
//    /**
//     * Get a SecureRandom instance using a FIPS 140-2 approved DRBG
//     * @return The SecureRandom instance
//     */
//    private SecureRandom getSecureRandom() {
//		/*
//         * According to NIST Special Publication 800-90A, a Nonce is
//         * A time-varying value that has at most a negligible chance of
//         * repeating, e.g., a random value that is generated anew for each
//         * use, a timestamp, a sequence number, or some combination of
//         * these.
//         *
//         * The nonce is combined with the entropy input to create the initial
//         * DRBG seed.
//         */
//        if (secureRandom != null) {
//            return secureRandom;
//        }
//
//        byte[] nonce = ByteBuffer.allocate(8).putLong(System.nanoTime()).array();
//        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
//        FipsDRBG.Builder drgbBldr = FipsDRBG.SHA512
//                .fromEntropySource(entSource).setSecurityStrength(256)
//                .setEntropyBitsRequired(256);
//        secureRandom = drgbBldr.build(nonce, true);
//        return  secureRandom;
//    }
//
//    /**
//     * Initialize the BC-FIPS Module as the JCA/JCE provider.
//     */
//    private void initialize() {
//		CryptoServicesRegistrar.setSecureRandom(getSecureRandom());
//		Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
//		Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);
//	}
//
//    private void loadKeyFromAWS() throws SdkClientException {
//
//        String secretName = System.getenv().getOrDefault("PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME", "izgw-dev-password-encryption-key");
//
//        if (StringUtils.isEmpty(secretName)) {
//            throw new IllegalArgumentException("ENCRYPTION_KEY_SECRET_NAME environment variable is not set.");
//        }
//
//        Region region = Region.of(cryptoConfig.getAwsRegion());
//
//        try (SecretsManagerClient client = SecretsManagerClient.builder().region(region).build()) {
//
//            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
//            GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
//            String secret = getSecretValueResponse.secretString();
//            if (!StringUtils.isEmpty(secret)) {
//                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
//                if (keyBytes.length != KEY_LENGTH) {
//                    throw new IllegalArgumentException(
//                            "Secret key length is invalid. Expected " + KEY_LENGTH +
//                                    " bytes, got " + keyBytes.length);
//                }
//            } else {
//                throw new IllegalArgumentException("Secret value is empty.");
//            }
//        }
//    }
//
//    private byte[] getKeyBytes() throws CryptoException {
//        if (keyBytes == null) {
//            synchronized (keyLock) {
//                if (keyBytes == null) {
//                    try {
//                        loadKeyFromAWS();
//                    } catch (Exception e) {
//                        throw new CryptoException("Failed to load encryption key", e);
//                    }
//                }
//            }
//        }
//        return keyBytes;
//    }
//}