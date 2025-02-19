package gov.cdc.izgateway.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.ServiceConfigurationError;

import jakarta.xml.bind.DatatypeConverter;

public interface ICertificateStatus {

	String getCertificateId();

	void setCertificateId(String certificateId);

	String getCommonName();

	void setCommonName(String commonName);

	String getCertSerialNumber();

	void setCertSerialNumber(String certificateSerialNumber);

	Date getLastCheckedTimeStamp();

	void setLastCheckedTimeStamp(Date lastCheckedTimeStamp);

	Date getNextCheckTimeStamp();

	void setNextCheckTimeStamp(Date nextCheckTimeStamp);

	String getLastCheckStatus();

	void setLastCheckStatus(String lastCheckStatus);
	
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
     * Helper function to compute the certificate identifier.
     * 
     * @param cert	The certificate to compute the thumbprint.
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