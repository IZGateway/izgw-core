package gov.cdc.izgateway.security.ocsp;

import java.util.ServiceConfigurationError;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public final class CryptoUtils  {
    public static final SignatureAlgorithmIdentifierFinder SIG_ALG_ID_FINDER = new DefaultSignatureAlgorithmIdentifierFinder();
    public static final String UNDERSCORE = "_";

    public static final DigestAlgorithmIdentifierFinder DIGEST_ALG_ID_FINDER = new DefaultDigestAlgorithmIdentifierFinder();

    public static final DigestCalculatorProvider DIGEST_CALC_PROV;

    static {
        try {
            DIGEST_CALC_PROV = new JcaDigestCalculatorProviderBuilder().build();
        } catch (OperatorCreationException e) {
            throw new ServiceConfigurationError("Cannot obtain Digest Provider", e);
        }
    }

    

    public static String joinCamelCase(String ... strParts) {
        for (int a = 0; a < strParts.length; a++) {
            strParts[a] = strParts[a].toLowerCase();

            if (a > 0) {
                strParts[a] = StringUtils.capitalize(strParts[a]);
            }
        }

        return StringUtils.join(strParts, StringUtils.EMPTY);
    }
    
    private CryptoUtils() {
    }

}