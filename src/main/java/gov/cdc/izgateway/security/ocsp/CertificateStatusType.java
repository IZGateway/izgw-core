package gov.cdc.izgateway.security.ocsp;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;

public enum CertificateStatusType {
    GOOD(CertificateStatus.class), REVOKED(RevokedStatus.class), UNKNOWN(UnknownStatus.class);

    private final int tag;
    private final String id;
    private final Class<?> type;

    private CertificateStatusType(Class<?> type) {
        this.tag = this.ordinal();
        this.id = CryptoUtils.joinCamelCase(StringUtils.split(this.name(), CryptoUtils.UNDERSCORE));
        this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public int getOrder() {
        return (this.tag * -1);
    }

    public int getTag() {
        return this.tag;
    }

    public Class<?> getType() {
        return this.type;
    }

	public static CertificateStatusType findByType(CertificateStatus certificateStatus) {
		if (certificateStatus instanceof RevokedStatus) {
			return REVOKED;
		} 
		if (certificateStatus instanceof CertificateStatus || certificateStatus == null) {
			return GOOD;
		} 
		return UNKNOWN;
	}
}
