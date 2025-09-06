package gov.cdc.izgateway.security.crypto;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

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
     * Constructor that loads the current and previous keys into the key history using AWS Secret tags.
     */
    AwsSecretsManagerKeyProvider() {
        String secretName = getEncryptionKeySecretName();
        if (StringUtils.isEmpty(secretName)) {
            return;
        }
        try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
            // Get secret description (includes tags and version ids)
            var describeRequest = software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest.builder()
                    .secretId(secretName).build();
            var describeResponse = client.describeSecret(describeRequest);
            Map<String, List<String>> versionStages = describeResponse.versionIdsToStages();
            String currentVersionId = null;
            String previousVersionId = null;
            for (Map.Entry<String, List<String>> entry : versionStages.entrySet()) {
                if (entry.getValue().contains("AWSCURRENT")) {
                    currentVersionId = entry.getKey();
                } else if (entry.getValue().contains("AWSPREVIOUS")) {
                	// We must load the previous key in case CC is not yet finished rolling out the new key
                    previousVersionId = entry.getKey();
                }
            }
            if (currentVersionId != null) {
                byte[] currentKey = loadKeyVersion(client, secretName, currentVersionId);
                if (currentKey != null) addKeyToHistory(currentKey);
            }
            if (previousVersionId != null) {
                byte[] previousKey = loadKeyVersion(client, secretName, previousVersionId);
                if (previousKey != null) addKeyToHistory(previousKey);
            }
        } catch (SdkClientException e) {
			throw new ServiceConfigurationError("Failed to load encryption key from AWS Secrets Manager", e);
		}
    }

    private byte[] loadKeyVersion(SecretsManagerClient client, String secretName, String versionId) {
        try {
        	GetSecretValueRequest req = GetSecretValueRequest.builder().secretId(secretName).versionId(versionId).build();
        	GetSecretValueResponse resp = client.getSecretValue(req);
            String secret = resp.secretString();
			return checkSecret(secret);
        } catch (Exception ignored) {}
        return null;
    }

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

        try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
        	return loadKeyVersion(client, secretName, null);
        } catch (SdkClientException e) {
            throw new CryptoException("Failed to load encryption key from AWS Secrets Manager", e);
        }
    }

	private byte[] checkSecret(String secret) {
		if (StringUtils.isEmpty(secret)) {
		    throw new IllegalArgumentException("Secret value is empty.");
		}
	    byte[] decoded = Hex.decode(secret.trim());
	    if (decoded.length != 32) {
	        throw new IllegalArgumentException("Secret key length is invalid. Expected 32 bytes, got " + decoded.length + " bytes.");
	    }
        return decoded;
	}

    private String getEncryptionKeySecretName() {
        return System.getenv().getOrDefault(IZGW_CRYPTO_SECRET_KEY_NAME, "izgw-dev-password-encryption-key");
    }

}