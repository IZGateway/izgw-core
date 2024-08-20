package gov.cdc.izgateway.security.ocsp;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x509.CRLReason;

public enum OcspRevokeReasonType {
    UNSPECIFIED(CRLReason.unspecified), KEY_COMPROMISE(CRLReason.keyCompromise), CA_COMPROMISE(CRLReason.cACompromise, "cACompromise"), AFFILIATION_CHANGED(
        CRLReason.affiliationChanged), SUPERSEDED(CRLReason.superseded), CESSATION_OF_OPERATION(CRLReason.cessationOfOperation), CERTIFICATE_HOLD(
        CRLReason.certificateHold), REMOVE_FROM_CRL(CRLReason.removeFromCRL, "removeFromCRL"), PRIVILEGE_WITHDRAWN(CRLReason.privilegeWithdrawn),
    AA_COMPROMISE(CRLReason.aACompromise, "aACompromise");

    private final int tag;
    private final String id;
    private final java.security.cert.CRLReason reason;

    private OcspRevokeReasonType(int tag) {
        this.tag = tag;
        this.id = CryptoUtils.joinCamelCase(StringUtils.split(this.name(), CryptoUtils.UNDERSCORE));
        this.reason = java.security.cert.CRLReason.valueOf(this.name());
    }

    private OcspRevokeReasonType(int tag, String id) {
        this.tag = tag;
        this.id = id;
        this.reason = java.security.cert.CRLReason.valueOf(this.name());
    }

    public String getId() {
        return this.id;
    }

    public java.security.cert.CRLReason getReason() {
        return this.reason;
    }

    public int getTag() {
        return this.tag;
    }
}
