package gov.cdc.izgateway.security.ocsp;

import java.net.Socket;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import gov.cdc.izgateway.security.ocsp.RevocationChecker.SslLocation;
import gov.cdc.izgateway.utils.X500Utils;
import lombok.extern.slf4j.Slf4j;

/*
 * This TrustManager wraps around an existing X509ExtendedTrustManager to provide revocation support
 * using OCSP.
 */
@Slf4j
public class RevocationTrustManager extends X509ExtendedTrustManager {

	private X509ExtendedTrustManager trustManager;

	public RevocationTrustManager(X509ExtendedTrustManager trustManager) {
		this.trustManager = trustManager;
	}

	public RevocationChecker getChecker() {
		return RevocationChecker.getInstance();
	}

	public boolean hasChecker() {
		return getChecker() != null;
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		try {
			trustManager.checkClientTrusted(arg0, arg1);
			onSuccess(SslLocation.CLIENT, arg0);
		} catch (CertificateException e) {
			logException(SslLocation.CLIENT, arg0, e);
			throw e;
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		try {
			trustManager.checkServerTrusted(arg0, arg1);
			onSuccess(SslLocation.CLIENT, arg0);
		} catch (CertificateException e) {
			logException(SslLocation.CLIENT, arg0, e);
			throw e;
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
		try {
			trustManager.checkClientTrusted(arg0, arg1, arg2);
			onSuccess(SslLocation.CLIENT, arg0);
		} catch (CertificateException e) {
			logException(SslLocation.CLIENT, arg0, e);
			throw e;
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
		try {
			trustManager.checkClientTrusted(arg0, arg1, arg2);
			onSuccess(SslLocation.CLIENT, arg0);
		} catch (CertificateException e) {
			logException(SslLocation.CLIENT, arg0, e);
			throw e;
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
		try {
			trustManager.checkServerTrusted(arg0, arg1);
			onSuccess(SslLocation.CLIENT, arg0);
		} catch (CertificateException e) {
			logException(SslLocation.SERVER, arg0, e);
			throw e;
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
		try {
			trustManager.checkServerTrusted(arg0, arg1, arg2);
			onSuccess(SslLocation.CLIENT, arg0);
		} catch (CertificateException e) {
			logException(SslLocation.SERVER, arg0, e);
			throw e;
		}
	}

	private void logException(SslLocation loc, X509Certificate[] chain, Exception e) {
		if (log.isWarnEnabled()) {
			log.warn(
					"SSL {} certificate chain for {} is NOT trusted (subjectDnNames={}, issuerDnNames={}, serialNums={}): {}",
					loc.getId(), X500Utils.getCommonName(chain[0]), Arrays.asList(X500Utils.buildSubjectDnNames(chain)),
					Arrays.asList(X500Utils.buildIssuerDnNames(chain)),
					Arrays.asList(X500Utils.buildSerialNumbers(chain)), e.getMessage());
		}
	}

	private void onSuccess(SslLocation loc, X509Certificate[] chain) throws CertificateException {
		if (hasChecker() && chain.length > 1) {
			try {
				getChecker().check(loc, chain[0], chain[1]);
			} catch (CertPathValidatorException e) {
				throw new CertificateException(e.getMessage(), e);
			}
		}
		logSuccess(SslLocation.CLIENT, chain);
	}

	private void logSuccess(SslLocation loc, X509Certificate[] chain) {
		if (log.isDebugEnabled()) {
			log.debug(
					"SSL {} certificate chain for {} is trusted (subjectDnNames={}, issuerDnNames={}, serialNums={})",
					loc.getId(), X500Utils.getCommonName(chain[0]), Arrays.asList(X500Utils.buildSubjectDnNames(chain)),
					Arrays.asList(X500Utils.buildIssuerDnNames(chain)),
					Arrays.asList(X500Utils.buildSerialNumbers(chain)));
		}
	}
}