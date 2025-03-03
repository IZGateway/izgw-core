package gov.cdc.izgateway.security;

import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.cert.*;

/**
 * CertificateProcessor provides utility methods for processing certificates.
 */
public class CertificateProcessor {
    public static X509Certificate processCertificateFromHeader(String certHeader) throws CertificateException {
        certHeader = normalizeCertHeader(certHeader);
        return parsePemCertificate(certHeader);
    }

    private static String normalizeCertHeader(String certHeader) {
        return URLDecoder.decode(certHeader.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static X509Certificate parsePemCertificate(String pemContent) throws CertificateException {
        try (PemReader pemReader = new PemReader(new StringReader(pemContent))) {
            PemObject pemObject = pemReader.readPemObject();
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(
                    new ByteArrayInputStream(pemObject.getContent()));
        } catch (Exception e) {
            throw new CertificateException("Failed to parse certificate", e);
        }
    }

    // Private constructor to prevent instantiation
    private CertificateProcessor() {
    }
}
