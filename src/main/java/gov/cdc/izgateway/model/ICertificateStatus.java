package gov.cdc.izgateway.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.ServiceConfigurationError;

import jakarta.xml.bind.DatatypeConverter;

/**
 * Represents the status and audit information for an X.509 certificate used in IZ Gateway.
 * <p>
 * Provides methods to get and set certificate details, timestamps, and status information.
 * Includes static helpers for computing certificate thumbprints.
 * </p>
 */
public interface ICertificateStatus extends DbAudit {

	/**
	 * Gets the unique identifier for the certificate.
	 * @return the certificate ID
	 */
	String getCertificateId();

	/**
	 * Sets the unique identifier for the certificate.
	 * @param certificateId the certificate ID
	 */
	void setCertificateId(String certificateId);

	/**
	 * Gets the common name from the certificate.
	 * @return the common name
	 */
	String getCommonName();

	/**
	 * Sets the common name for the certificate.
	 * @param commonName the common name
	 */
	void setCommonName(String commonName);

	/**
	 * Gets the serial number of the certificate.
	 * @return the certificate serial number
	 */
	String getCertSerialNumber();

	/**
	 * Sets the serial number of the certificate.
	 * @param certificateSerialNumber the certificate serial number
	 */
	void setCertSerialNumber(String certificateSerialNumber);

	/**
	 * Gets the last checked timestamp for the certificate status.
	 * @return the last checked timestamp
	 */
	Date getLastCheckedTimeStamp();

	/**
	 * Sets the last checked timestamp for the certificate status.
	 * @param lastCheckedTimeStamp the last checked timestamp
	 */
	void setLastCheckedTimeStamp(Date lastCheckedTimeStamp);

	/**
	 * Gets the next scheduled check timestamp for the certificate status.
	 * @return the next check timestamp
	 */
	Date getNextCheckTimeStamp();

	/**
	 * Sets the next scheduled check timestamp for the certificate status.
	 * @param nextCheckTimeStamp the next check timestamp
	 */
	void setNextCheckTimeStamp(Date nextCheckTimeStamp);

	/**
	 * Gets the last check status for the certificate.
	 * @return the last check status
	 */
	String getLastCheckStatus();

	/**
	 * Sets the last check status for the certificate.
	 * @param lastCheckStatus the last check status
	 */
	void setLastCheckStatus(String lastCheckStatus);

	/**
	 * Helper function to get a SHA-1 MessageDigest instance.
	 * @return MessageDigest for SHA-1
	 */
    static MessageDigest getMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// This should never happen. SHA-1 is so intrinsic to everything done in web-services that there's guaranteed to be 
			// an instance unless something is badly broken.
			throw new ServiceConfigurationError("Cannot initialize SHA-1 Digest Function", e);
		}
	}

    /**
     * Helper function to compute the certificate identifier (thumbprint).
     * @param cert The certificate to compute the thumbprint for
     * @return A string representing the thumbprint using SHA-1
     */
    static String computeThumbprint(X509Certificate cert) {
    	if (cert == null) {
    		return null;
    	}
		try {
			return DatatypeConverter.printHexBinary(getMessageDigest().digest(cert.getEncoded())).toLowerCase();
		} catch (CertificateEncodingException e) {
			throw new IllegalArgumentException("Invalid Certificate", e);
		}
    }
}