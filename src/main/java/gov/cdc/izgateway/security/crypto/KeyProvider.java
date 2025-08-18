package gov.cdc.izgateway.security.crypto;

import java.util.List;

/**
 * Interface for managing cryptographic keys and key history.
 * <p>
 * This provider handles the loading and management of encryption keys,
 * maintaining a history of keys to support key rotation scenarios where
 * data encrypted with older keys may still need to be decrypted.
 * </p>
 *
 * @author CDC IZ Gateway Team
 * @since 1.0
 */
interface KeyProvider {

    /**
     * Loads the current active encryption key.
     * <p>
     * This method returns the primary key that should be used for new
     * encryption operations. The implementation should ensure the key
     * is valid and properly formatted for the intended cryptographic use.
     * </p>
     *
     * @return the current active key as a byte array
     * @throws CryptoException if the key cannot be loaded or is invalid
     */
    byte[] loadKey() throws CryptoException;

    /**
     * Retrieves all available keys including the current key and historical keys.
     * <p>
     * This method returns a list of all keys that can be used for decryption,
     * supporting scenarios where data was encrypted with older keys that are
     * still valid for decryption but no longer used for new encryption.
     * </p>
     *
     * @return an immutable list of all available keys as byte arrays,
     *         with the current key typically being the first element
     */
    List<byte[]> getAllKeys();

    /**
     * Checks if a specific key exists.
     * <p>
     * This method can be used to verify if a particular key is known
     * to the provider before attempting to use it for cryptographic operations.
     * </p>
     *
     * @param keyBytes the key to check for existence
     * @return {@code true} if the key exists in the provider, {@code false} otherwise
     * @throws IllegalArgumentException if keyBytes is null
     */
    boolean keyExists(byte[] keyBytes);

    /**
     * Adds a key to the historical key storage.
     * <p>
     * This method is typically called during key rotation to preserve
     * older keys for decryption purposes. The key will be available
     * through {@link #getAllKeys()} but should not be used for new
     * encryption operations.
     * </p>
     *
     * @param keyBytes the key to add to the historical key store
     * @throws IllegalArgumentException if keyBytes is null or invalid
     */
    void addKeyToHistory(byte[] keyBytes);
}