package gov.cdc.izgateway.security.ocsp;

import org.springframework.util.MimeType;

public final class ContentTypes {
    public static final String OCSP_REQ_TYPE = "application";
    public static final String OCSP_REQ_SUBTYPE = "ocsp-request";
    public static final MimeType OCSP_REQ = new MimeType(OCSP_REQ_TYPE, OCSP_REQ_SUBTYPE);
    public static final String OCSP_RESP_TYPE = "application";
    public static final String OCSP_RESP_SUBTYPE = "ocsp-response";
    public static final MimeType OCSP_RESP = new MimeType(OCSP_RESP_TYPE, OCSP_RESP_SUBTYPE);

    private ContentTypes() {
    }
}
