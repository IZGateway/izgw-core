package gov.cdc.izgateway.security.ocsp;

import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.http.HttpHeaders;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.model.ICertificateStatus;
import gov.cdc.izgateway.service.ICertificateStatusService;
import gov.cdc.izgateway.utils.X500Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RevocationChecker {
	private static final long GOODCERT_RECHECK_HOURS = 24;
	private static final Map<String, String> BASE_OCSP_REQ_HEADERS = new LinkedHashMap<>();
	private static final DigestCalculator digestCalc = getDigestCalculator();
	private static RevocationChecker instance;
	
	public enum SslLocation {
	    CLIENT, SERVER;

	    private final String id;

	    private SslLocation() {
	        this.id = this.name().toLowerCase();
	    }

	    public String getId() {
	        return this.id;
	    }
	}
	
	private final ICertificateStatusService certificateStatusService;
	private final SecureRandom secureRandom;
	
	@Autowired
	public
	RevocationChecker(ICertificateStatusService certificateStatusService, SecureRandom secureRandom) {
		this.certificateStatusService = certificateStatusService;
		this.secureRandom = secureRandom;
		setInstance(this);
	}
	
	private int connectTimeout;
	private AlgorithmIdentifier digestAlgId;
	private boolean nonceOptional;
	private int nonceSize;
	private ListOrderedSet<AlgorithmIdentifier> preferredSigAlgIds;
	private int readTimeout;

	static {
		BASE_OCSP_REQ_HEADERS.put(HttpHeaders.ACCEPT, ContentTypes.OCSP_RESP.toString());
		BASE_OCSP_REQ_HEADERS.put(HttpHeaders.CONTENT_TYPE, ContentTypes.OCSP_REQ.toString());
	}
	
	private static DigestCalculator getDigestCalculator() {
		try {
			return CryptoUtils.DIGEST_CALC_PROV.get(CertificateID.HASH_SHA1);
		} catch (OperatorCreationException e) {
			throw new ServiceConfigurationError("Cannot build DigestCalculator", e);
		}
	}
	
    public void check(SslLocation loc, X509Certificate baseCert, X509Certificate issuer) throws CertPathValidatorException {
        try {
            this.checkInternal(loc, baseCert, issuer);
        } catch (CertPathValidatorException ex) {  // NOSONAR, log it here for local detail, rethrow for handling
            // Log exception so that we know why the certificate is invalid.
            log.error(Markers2.append(ex), "Invalid Certificate: {}", ex.getMessage());
            throw ex;
        }
    }

	protected void checkInternal(SslLocation loc, X509Certificate cert, X509Certificate issuer) throws CertPathValidatorException {
		long recheck = Duration.ofHours(GOODCERT_RECHECK_HOURS).toMillis(); 
		long now = System.currentTimeMillis();
		Timestamp currentTs = new Timestamp(now);
		
		ICertificateStatus checkedCert = null;
		
		try {
			checkedCert = certificateStatusService.findByCertificateId(ICertificateStatus.computeThumbprint(cert));
			if (checkedCert == null) {
				checkedCert = certificateStatusService.create(cert);
			} else if ("NO_RESPONDER_URL_FOUND".equalsIgnoreCase(checkedCert.getLastCheckStatus())) {
				// Uncheckable, there is no OCSP Responder for this certificate
				log.debug("OCSP responder url not found for: {}", checkedCert.getCommonName());
				return;
			} else if (CertificateStatusType.REVOKED.toString().equalsIgnoreCase(checkedCert.getLastCheckStatus())) {
				// Previously revoked
				throw new CertPathValidatorException(String.format("Certificate for %s has been revoked", checkedCert.getCommonName()));
			} else if (checkedCert.getNextCheckTimeStamp() != null && currentTs.compareTo(checkedCert.getNextCheckTimeStamp()) < 0) {
				// It was previous checked, and sufficient time hasn't passed
				return;
			}
		} catch (Exception e) {
			log.error(Markers2.append(e), "Error retrieving certificate status for {}", X500Utils.getCommonName(cert));
			// DB Access failure should NOT block certificate validation
			return;
		}

		CertificateStatusType status = CertificateStatusType.UNKNOWN; 
		try {
			checkedCert.setLastCheckedTimeStamp(new Timestamp(now));
			
			status = revocationCheck(loc, cert, issuer);
			
			if (status == null) {
				// Uncheckable Certificate
				checkedCert.setLastCheckStatus("NO_RESPONDER_URL_FOUND");
				checkedCert.setNextCheckTimeStamp(new Timestamp(now + recheck));
				return;
			}
			
			checkedCert.setLastCheckStatus(status.toString());
			checkedCert.setNextCheckTimeStamp(new Timestamp(now + recheck));
			
			switch (status) {
			case GOOD:
				log.debug("{} is good.", checkedCert.getCommonName());
				recheck = GOODCERT_RECHECK_HOURS * 3600000l;
				checkedCert.setNextCheckTimeStamp(new Timestamp(now + recheck));
				break;
			case REVOKED:
				log.warn("{} is revoked.", checkedCert.getCommonName());
				break;
			default:
				log.warn("{} is unknown.", checkedCert.getCommonName());
				break;
			}
		} catch (Exception e) {
			log.warn(Markers2.append(e), "Certificate status is unknown: {}", e.getMessage());
			checkedCert.setLastCheckStatus(CertificateStatusType.UNKNOWN.toString());
		} finally {
			certificateStatusService.save(checkedCert);
		}
		
		if (status == CertificateStatusType.REVOKED) {
			throw new CertPathValidatorException(String.format("Certificate for %s has been revoked", checkedCert.getCommonName()));
		}
	}
	
	public CertificateStatusType revocationCheck(SslLocation loc, X509Certificate cert, X509Certificate issuerCert)
			throws CertPathValidatorException, OCSPException {
		
		String certName = cert.getSubjectX500Principal().getName();
		String certDescription = String.format(
				"SSL %s certificate (subjectDnName=%s, issuerDnName=%s, serialNum=%d)", loc.getId(),
				certName, cert.getIssuerX500Principal().getName(),
				cert.getSerialNumber());
		log.debug("Cert to be verified {}", certDescription);
		ResponderUrlHelper responderUrlHelper = new ResponderUrlHelper();
		
		X509CertificateHolder issuerCertHolder = null;
		try {
			issuerCertHolder = new JcaX509CertificateHolder(issuerCert);
		} catch (CertificateEncodingException e1) {
			log.error(Markers2.append(e1), "Issuer Certificate error during OCSP checking for {}", X500Utils.getCommonName(issuerCert));
			return null;
		}

		try {
			// find the URL for OCSP Responder server against which the cert can be verified
			URL ocspResponderUrl = responderUrlHelper.getOcspResponderUrl(cert);

			log.debug("URL of the OCSP Responder: {}" ,ocspResponderUrl);

			if (ocspResponderUrl == null) {
				log.warn("OCSP Responer URL not found in cert extension for: {}" ,certDescription);
				return null;
			}
			
			byte[] nonceOcspReqExtContent = this.generateNonce();
			Extension nonceOcspReqExt = new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false,
					new DEROctetString(nonceOcspReqExtContent));
			RequestHelper ocspRequestHelper = new RequestHelper();

			// Generate the Certificate Id with info hashed with Digest for easy processing
			CertificateId ocspReqCertId = ocspRequestHelper.getOcspRequestCertificateId(digestCalc,
					issuerCertHolder, cert);
			OCSPReq ocspReq = ocspRequestHelper.createOCSPRequest(ocspReqCertId, nonceOcspReqExt);

			OCSPResp ocspResponse = ocspRequestHelper.queryOcspResponder(ocspResponderUrl, ocspReq,
					this.connectTimeout, this.readTimeout, BASE_OCSP_REQ_HEADERS);

			if (ocspResponse == null) {
				log.warn("No response from OCSP Responder: {} for {}", ocspResponderUrl, certDescription);
				return CertificateStatusType.UNKNOWN;
			}

			log.debug("Got response from OCSP Responder- {}.", ocspResponse);

			ResponseStatusType ocspRespStatus = ResponseStatusType.findByTag(ocspResponse.getStatus());

			if (ocspRespStatus != ResponseStatusType.SUCCESSFUL) {
				log.warn("Unable to query OCSP responder server at (url={}). due to status {}", ocspResponderUrl, ocspRespStatus);
				return CertificateStatusType.UNKNOWN;
			} 
			
			log.debug("OCSP Responder url connection status {}", ocspRespStatus);
			ResponseHelper responseHelper = new ResponseHelper();
			org.bouncycastle.cert.ocsp.CertificateStatus certificateStatus = null;
			BasicOCSPResp basicOcspResp = responseHelper.getOcspResponse(ocspResponderUrl, certDescription,
					ocspResponse);
			
			if (basicOcspResp == null) {
				log.warn("No response from OCSP responder server at (url= {}) for {}", ocspResponderUrl, certDescription);
				return CertificateStatusType.UNKNOWN;
			}
			
			boolean verifyNonce = responseHelper.checkNonce(certDescription, ocspResponderUrl,
					nonceOcspReqExtContent, basicOcspResp);

			SingleResp ocspCertResp = 
				responseHelper.getOcspCertificateResponse(certDescription, ocspReqCertId, ocspResponderUrl, basicOcspResp);

			if (!verifyNonce) {
				// if nonce can not be verified, check for latest status response
				responseHelper.checkOcspResponse(ocspCertResp);
			}
			
			certificateStatus = ocspCertResp.getCertStatus();

			/*  RevokedStatus is a subclass of CertificateStatus,BC-FIPS returns RevokedStatus subclass if
			    certificateStatus Object is null. Refer to org.bouncycastle.cert.ocsp.SingleResp class for further details
			  */
			CertificateStatusType ocspCertRespStatus = CertificateStatusType.findByType(certificateStatus);  // NOSONAR, no immediate return to enable debugging

			return ocspCertRespStatus;
		} catch (IOException |OCSPException e) {
			log.error(Markers2.append(e), "Exception processing OCSP request for {}", certDescription);
			return  CertificateStatusType.UNKNOWN;
		}
	}

	private byte[] generateNonce() {
		byte[] ocspNonce = new byte[this.nonceSize];

		secureRandom.nextBytes(ocspNonce);

		return ocspNonce;
	}

	public int getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public AlgorithmIdentifier getDigestAlgorithm() {
		return digestAlgId;
	}

	public void setDigestAlgorithmId(String digestAlgId) {
		this.digestAlgId = CryptoUtils.DIGEST_ALG_ID_FINDER.find(digestAlgId);
	}
	
	public boolean isNonceOptional() {
		return this.nonceOptional;
	}

	public void setNonceOptional(boolean nonceOptional) {
		this.nonceOptional = nonceOptional;
	}

	public int getNonceSize() {
		return this.nonceSize;
	}

	public void setNonceSize(int nonceSize) {
		this.nonceSize = nonceSize;
	}
	public ListOrderedSet<AlgorithmIdentifier> getPreferredSignatureAlgorithmIds() {
		return this.preferredSigAlgIds;
	}

	public void setPreferredSignatureAlgorithmIds(List<String> preferredSigAlgIds) {
		this.preferredSigAlgIds = ListOrderedSet.listOrderedSet(
				preferredSigAlgIds.stream().map(CryptoUtils.SIG_ALG_ID_FINDER::find).toList());
	}

	public int getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public static RevocationChecker getInstance() {
		return instance;
	}
	private static void setInstance(RevocationChecker instance) {
		RevocationChecker.instance = instance;
	}
}
