package gov.cdc.izgateway.security;

public interface KeyProvider {
    byte[] loadKey() throws CryptoException;
}
