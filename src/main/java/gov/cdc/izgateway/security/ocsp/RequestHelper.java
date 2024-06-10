package gov.cdc.izgateway.security.ocsp;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.operator.DigestCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class RequestHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHelper.class);

	protected CertificateId getOcspRequestCertificateId(DigestCalculator digestCalc,
			X509CertificateHolder issuerCertHolder, X509Certificate cert)
			throws CertPathValidatorException {
		CertificateId ocspReqCertId;

		try {
			ocspReqCertId = new CertificateId(digestCalc, issuerCertHolder, cert.getSerialNumber());
		} catch (OCSPException e) {
			throw new CertPathValidatorException("Unable to determine OCSP ID." + e.getMessage());
		}
		return ocspReqCertId;
	}

	/*
	 * Create OCSP request with nonce extension to send to the responder url
	 * Certificate ID is generated for the verifying cert and send to the request
	 */
	protected OCSPReq createOCSPRequest(CertificateId ocspReqCertId, Extension nonceOcspReqExt) throws OCSPException {

		OCSPReq request = null;
		try {

			OCSPReqBuilder ocspGen = new OCSPReqBuilder();
			// add the certificate id to the request builder and build the request
			ocspGen.addRequest(ocspReqCertId);
			ocspGen.setRequestExtensions(new Extensions(nonceOcspReqExt));

			request = ocspGen.build();
		} catch (OCSPException e) {
			throw new OCSPException("Error with generating OCSPRequest: " + e.getMessage());
		}

		return request;
	}

	/* send the query to ocsp responder server url with the ocsp request content */

	protected OCSPResp queryOcspResponder(URL ocspResponderUrl, OCSPReq ocspReqContent,
			int connectTimeout, int readTimeout, Map<String, String> reqHeaders) throws IOException {

		HttpURLConnection ocspResponderConn = ((HttpURLConnection) ocspResponderUrl.openConnection());
		LOGGER.debug("open connection to the ocsp server url {}", ocspResponderUrl);
		ocspResponderConn.setDoInput(true);
		ocspResponderConn.setDoOutput(true);
		ocspResponderConn.setUseCaches(false);
		ocspResponderConn.setConnectTimeout(connectTimeout);
		ocspResponderConn.setReadTimeout(readTimeout);
		ocspResponderConn.setRequestMethod(HttpPost.METHOD_NAME);
		byte[] ocspReq = ocspReqContent.getEncoded();

		reqHeaders.forEach(ocspResponderConn::setRequestProperty);
		ocspResponderConn.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(ocspReq.length));

		try (OutputStream ocspResponderOutStream = ocspResponderConn.getOutputStream()) {
			ocspResponderOutStream.write(ocspReq);
			ocspResponderOutStream.flush();
		}

		OCSPResp ocspResp = null;

		try (InputStream ocspResponderInStream = ocspResponderConn.getInputStream()) {
			ocspResp = new OCSPResp(IOUtils.toByteArray(ocspResponderInStream));
		}

		return ocspResp;
	}

}