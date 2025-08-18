package gov.cdc.izgateway.security;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AwsSecretsManagerKeyProvider implements KeyProvider {
    private static final String PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME = "PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME";

    @Override
    public byte[] loadKey() throws CryptoException {
        String secretName = getEncryptionKeySecretName();
        if (StringUtils.isEmpty(secretName)) {
            throw new IllegalArgumentException(PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME + " environment variable is not set.");
        }

        try {
            Region region = Region.of(Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-1"));
            try (SecretsManagerClient client = SecretsManagerClient.builder().region(region).build()) {
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
        return System.getenv().getOrDefault(PHIZ_CRYPTO_ENCRYPTION_KEY_SECRET_NAME, "izgw-dev-password-encryption-key");
    }
}
