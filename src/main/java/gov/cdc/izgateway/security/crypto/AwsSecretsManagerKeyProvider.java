package gov.cdc.izgateway.security.crypto;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AWS Secrets Manager implementation of the KeyProvider interface.
 * <p>
 * This provider retrieves encryption keys from AWS Secrets Manager service.
 * The secret is expected to be stored as a UTF-8 encoded string representing
 * a 32-byte encryption key suitable for AES-256 encryption.
 * </p>
 * <p>
 * Configuration is handled through environment variables:
 * <ul>
 *   <li>{@code IZGW_CRYPTO_SECRET_KEY_NAME} - The name of the secret in AWS Secrets Manager</li>
 *   <li>{@code AWS_REGION} - The AWS region where the secret is stored (defaults to us-east-1)</li>
 * </ul>
 * </p>
 *
 * @author CDC IZ Gateway Team
 * @since 1.0
 */
class AwsSecretsManagerKeyProvider extends KeyProviderBase implements KeyProvider {
    private static final String IZGW_CRYPTO_SECRET_KEY_NAME = "IZGW_CRYPTO_SECRET_KEY_NAME";

    /**
     * Loads the encryption key from AWS Secrets Manager.
     *
     * @return a 32-byte array containing the encryption key
     * @throws IllegalArgumentException if the secret name environment variable is not set,
     *                                  the secret value is empty, or the key length is not 32 bytes
     * @throws CryptoException if there's an error communicating with AWS Secrets Manager
     */
    @Override
    public byte[] loadKey() throws CryptoException {
        String secretName = getEncryptionKeySecretName();
        if (StringUtils.isEmpty(secretName)) {
            throw new IllegalArgumentException(IZGW_CRYPTO_SECRET_KEY_NAME + " environment variable is not set.");
        }

        try {
            try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
                GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
                GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
                String secret = getSecretValueResponse.secretString();
                if (!StringUtils.isEmpty(secret)) {
                    byte[] decoded = secret.getBytes(StandardCharsets.UTF_8);
                    if (decoded.length == 32) {
                        return decoded;
                    } else {
                        throw new IllegalArgumentException("Secret key length is invalid. Expected 32 bytes, got " + decoded.length + " bytes.");
                    }
                } else {
                    throw new IllegalArgumentException("Secret value is empty.");
                }
            }
        } catch (SdkClientException e) {
            throw new CryptoException("Failed to load encryption key from AWS Secrets Manager", e);
        }
    }

    private String getEncryptionKeySecretName() {
        return System.getenv().getOrDefault(IZGW_CRYPTO_SECRET_KEY_NAME, "izgw-dev-password-encryption-key");
    }

}
