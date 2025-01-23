package gov.cdc.izgateway.security.principal;

import gov.cdc.izgateway.principal.provider.CertificatePrincipalProvider;
import gov.cdc.izgateway.security.CertificatePrincipal;
import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.utils.X500Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Globals;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
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

    @Override
    public IzgPrincipal createPrincipalFromCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(Globals.CERTIFICATES_ATTR);

        if (certs == null || certs.length == 0) {
            return null;
        }

        return createPrincipalFromCertificate(certs[0]);
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
}
