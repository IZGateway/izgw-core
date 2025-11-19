package gov.cdc.izgateway.service;

import java.security.cert.X509Certificate;
import java.util.List;

import gov.cdc.izgateway.model.ICertificateStatus;

/**
 * @author Audacious Inquiry
 *
 * @param <T> The type of Certificate Status this service manages 
 */
public interface ICertificateStatusService<T extends ICertificateStatus> {
	/**
	 * @return All certificates
	 */
	List<T> getAllCertificates();
	/**
	 * Save the given certificate status.
	 * @param certificateStatus The certificate status to save
	 * @return The saved certificate status
	 */
	T save(T certificateStatus);
	/**
	 * Find a given certificate by its ID.
	 * @param certificateId	The certificate ID
	 * @return	The certificate status or null if not found
	 */
	T findByCertificateId(String certificateId);
	/**
	 * Refresh the certificate statuses from their source.
	 */
	void refresh();
	/**
	 * Create a new certificate status from the given X509Certificate.
	 * @param cert The X509Certificate
	 * @return The created certificate status
	 */
	T create(X509Certificate cert);
}