package gov.cdc.izgateway.security.ocsp;

import java.net.URL;
import java.security.cert.CertPathValidatorException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.ResponderID;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHelper.class);

	/* Get BasicOCSPResp from the OCSPResp object */
	protected BasicOCSPResp getOcspResponse(URL ocspResponderUrl, String certDescription, OCSPResp ocspResponse)
			throws CertPathValidatorException, OCSPException {

		BasicOCSPResp basicOcspResp = null;

		ASN1ObjectIdentifier ocspRespType = ocspResponse.toASN1Structure().getResponseBytes().getResponseType();

		try {
			// Verify the response object type
			if (ocspRespType.equals(OCSPObjectIdentifiers.id_pkix_ocsp_basic)) {
				basicOcspResp = ((BasicOCSPResp) ocspResponse.getResponseObject());
				@SuppressWarnings("unused")
				ResponderID responderID = basicOcspResp.getResponderId().toASN1Primitive();			
			}
		} catch (OCSPException ocspException) {
			throw  ocspException;
		}
		return basicOcspResp;
	}

	/* Verify nonce matches between the OCSP Request and Response */
	protected boolean checkNonce(String certDescription, URL ocspResponderUrl, byte[] nonceOcspReqExtContent,
			BasicOCSPResp ocspResp) throws OCSPException {
		boolean isNonceMatching = false;
		Extension nonceOcspRespExt = ocspResp.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
		
		if (nonceOcspRespExt == null) {
			isNonceMatching = false;
			LOGGER.debug(
					"{} OCSP responder (url={}) response (producedAt={}) does not contain a nonce extension (oid={}).",
					certDescription, ocspResponderUrl, ocspResp.getProducedAt(),
					OCSPObjectIdentifiers.id_pkix_ocsp_nonce.getId());
		} else {
			byte[] nonceOcspRespExtContent = nonceOcspRespExt.getExtnValue().getOctets();

			if (Arrays.equals(nonceOcspReqExtContent, nonceOcspRespExtContent)) {
				LOGGER.debug("Nonce matches between request and response");
				isNonceMatching = true;
			} else {
				String error = String.format(
						"%s OCSP responder (url=%s) response (producedAt=%s) nonce extension (oid=%s) value does not match (%s).",
						certDescription, ocspResponderUrl, ocspResp.getProducedAt(),
						OCSPObjectIdentifiers.id_pkix_ocsp_nonce.getId(), Hex.encodeHexString(nonceOcspRespExtContent));
				throw new OCSPException(error);
			}
		}
		return isNonceMatching;
	}

	/* get SingleResp from the BasicOCSPResp */
	protected SingleResp getOcspCertificateResponse(String certDescription, CertificateId ocspReqCertId,
			URL ocspResponderUrl, BasicOCSPResp ocspResp) throws CertPathValidatorException, OCSPException {
		SingleResp ocspCertResp = null;

		for (SingleResp availableOcspCertResp : ocspResp.getResponses()) {
			CertificateId availableOcspCertRespId;
			availableOcspCertRespId = new CertificateId(availableOcspCertResp.getCertID());

			try {
				if (availableOcspCertRespId.matches(ocspReqCertId)) {
					ocspCertResp = availableOcspCertResp;
				}
			} catch (OCSPException ocspException) {
				String error = String.format(
						"Unable to match %s OCSP responder (url=%s) response (producedAt=%s) certificate (serialNum=%d). %s",
						certDescription, ocspResponderUrl, ocspResp.getProducedAt(),
						availableOcspCertRespId.getSerialNumber(), ocspException.getMessage());
				throw new OCSPException(error,ocspException);
			}
		}
		return ocspCertResp;
	}

	/* Verify the response is latest with the request */
	protected void checkOcspResponse(SingleResp resp) throws OCSPException {
		Date currentDate = Calendar.getInstance().getTime();
		// The time at which the ocsp resp status being indicated is known to be correct
		Date thisUpdate = resp.getThisUpdate();
		if (thisUpdate == null) {
			throw new OCSPException("thisUpdate field is missing in OCSP response");
		}
		// The time at or before which newer information will be available about the status of the certificate
		Date nextUpdate = resp.getNextUpdate();
		if (nextUpdate == null) {
			LOGGER.debug(
					" nextUpdate is not set, the responder is indicating that newer revocation information is available all the time.");
		}
		if (currentDate.compareTo(thisUpdate) < 0) {
			throw new OCSPException(
					"thisUpdate time is later than the local system time, so response should be considered unreliable");
		}
		if (currentDate.compareTo(nextUpdate) > 0) {
			throw new OCSPException(
					"nextUpdate value is earlier than the local system time value,so response should be considered unreliable");
		}
	}
}
