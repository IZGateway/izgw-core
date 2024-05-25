package gov.cdc.izgateway.security.ocsp;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.ocsp.OCSPResp;

public enum ResponseStatusType {
    SUCCESSFUL(OCSPResp.SUCCESSFUL), MALFORMED_REQUEST(OCSPResp.MALFORMED_REQUEST), INTERNAL_ERROR(OCSPResp.INTERNAL_ERROR), TRY_LATER(OCSPResp.TRY_LATER),
    SIG_REQUIRED(OCSPResp.SIG_REQUIRED), UNAUTHORIZED(OCSPResp.UNAUTHORIZED);

    private final int tag;
    private final String id;

    private ResponseStatusType(int tag) {
        this.tag = tag;
        this.id = CryptoUtils.joinCamelCase(StringUtils.split(this.name(), CryptoUtils.UNDERSCORE));
    }

    public String getId() {
        return this.id;
    }

    public int getTag() {
        return this.tag;
    }

	public static ResponseStatusType findByTag(int status) {
		for (ResponseStatusType type: ResponseStatusType.values()) {
			if (type.getTag() == status) {
				return type;
			}
		}
		return null;
	}
}
