package gov.cdc.izgateway.security;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLUtil;
import org.apache.tomcat.util.net.jsse.JSSEImplementation;
import org.apache.tomcat.util.net.jsse.JSSEUtil;

import gov.cdc.izgateway.security.ocsp.RevocationTrustManager;

public class SSLImplementation extends JSSEImplementation {
	/**
	 * This class overrides the JSSEUtil.getTrustManagers response to return a RevocationTrustManager.
	 * @author boonek
	 *
	 */
	private static class JSSEUtilWithOCSP extends JSSEUtil {
		public JSSEUtilWithOCSP(SSLHostConfigCertificate certificate) {
			super(certificate);
		}

		@Override
		public TrustManager[] getTrustManagers() throws Exception {
			TrustManager[] tmgrs = super.getTrustManagers();
			for (int i = 0; i < tmgrs.length; i++) {
				if (tmgrs[i] instanceof X509ExtendedTrustManager x509tmgr) {
					tmgrs[i] = new RevocationTrustManager(x509tmgr);
				}
			}
			return tmgrs;
		}
	}
	
    @Override
    public SSLUtil getSSLUtil(SSLHostConfigCertificate certificate) {
        return new JSSEUtilWithOCSP(certificate);
    }

}
