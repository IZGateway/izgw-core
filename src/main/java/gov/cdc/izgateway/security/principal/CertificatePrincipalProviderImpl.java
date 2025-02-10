package gov.cdc.izgateway.security.principal;

import gov.cdc.izgateway.principal.provider.CertificatePrincipalProvider;
import gov.cdc.izgateway.security.CertificatePrincipal;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.utils.X500Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Globals;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * A Principal Provider that uses certificates to identify the principal
 * by common name and organization.
 *
 * @author Audacious Inquiry
 *
 */
@Slf4j
@Component
public class CertificatePrincipalProviderImpl implements CertificatePrincipalProvider {
    // The header key that contains the certificate (populated by the ALB )
    @Value("${client.ssl.certificate-header:}")
    private String certHeaderKey;

    @Override
    public IzgPrincipal createPrincipalFromCertificate(HttpServletRequest request) {
        X509Certificate cert = getCertificate(request);

        if (cert == null) {
            return null;
        }

        return createPrincipalFromCertificate(cert);
    }

    /**
     * Create a principal from a certificate
     * @param cert	The certificate
     * @return	The principal
     */
    public static IzgPrincipal createPrincipalFromCertificate(X509Certificate cert) {
        IzgPrincipal principal = new CertificatePrincipal();
        X500Principal subject = cert.getSubjectX500Principal();

        Map<String, String> parts = X500Utils.getParts(subject);
        principal.setName(parts.get(X500Utils.COMMON_NAME));
        String o = parts.get(X500Utils.ORGANIZATION);
        if (StringUtils.isBlank(o)) {
            o = parts.get(X500Utils.ORGANIZATION_UNIT);
        }
        principal.setOrganization(o);

        principal.setValidFrom(cert.getNotBefore());
        principal.setValidTo(cert.getNotAfter());
        principal.setSerialNumber(String.valueOf(cert.getSerialNumber()));
        principal.setIssuer(cert.getIssuerX500Principal().getName());

        return principal;
    }

    /**
     * Returns the certificate from the request attribute if it exists, otherwise
     * it will attempt to parse the certificate from the header (populated by the ALB).
     * @param request The HTTPServletRequest
     * @return The certificate or null if it cannot be found
     */
    private X509Certificate getCertificate(HttpServletRequest request) {
        X509Certificate cert = null;

        // Check for certificate in request attribute
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(Globals.CERTIFICATES_ATTR);
        if (certs != null && certs.length > 0) {
            cert = certs[0];
        } else {
            // Check for certificate in header
            String certHeader = request.getHeader(certHeaderKey);
            if (StringUtils.isNotBlank(certHeader)) {
                // The certificate is base64 encoded and URL encoded
                certHeader = certHeader.replace("+", "%2B");
                certHeader = URLDecoder.decode(certHeader, StandardCharsets.UTF_8);
                try (PemReader pemReader = new PemReader(new StringReader(certHeader))) {
                    PemObject pemObject = pemReader.readPemObject();
                    byte[] decodedCert = pemObject.getContent();
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedCert));
                } catch (Exception e) {
                    log.error("Failed to parse certificate from header", e);
                    return null;
                }
            }
        }

        return cert;
    }
}