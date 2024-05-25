package gov.cdc.izgateway.service;

import java.security.cert.X509Certificate;
import java.util.List;

import gov.cdc.izgateway.model.ICertificateStatus;

public interface ICertificateStatusService {
	List<ICertificateStatus> getAllCertificates();
	ICertificateStatus save(ICertificateStatus certificateStatus);
	ICertificateStatus findByCertificateId(String certificateId);
	void refresh();
	ICertificateStatus create(X509Certificate cert);
}