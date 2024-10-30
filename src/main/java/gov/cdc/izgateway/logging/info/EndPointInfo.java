package gov.cdc.izgateway.logging.info;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;


import gov.cdc.izgateway.security.Principal;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.utils.X500Utils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import javax.security.auth.x500.X500Principal;

/**
 * An endpoint describes the inbound or outbound connection to
 * IZ Gateway during a transaction.  It is abstract to ensure
 * that it is not used on its own.
 */
@Schema(description = "Information common to all endpoints")
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public abstract class EndPointInfo extends HostInfo implements Serializable {
	private static final long serialVersionUID = 1L;

    @Schema(description="The common name on the certificate associated with the requester")
    @JsonProperty
    private String principalName;

    @Schema(description="The cipher suite used by the endpoint.")
    @JsonProperty
    private String cipherSuite;

    @Schema(description="The organization associated with the with the endpoint.")
    @JsonProperty
    private String organization;

    @Schema(description="The serial number associated with the with certificate on the endpoint.")
    @JsonProperty
    private String uniqueId;

    @JsonProperty
    @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
    @Schema(description="The starting date associated with the with certificate on the endpoint.")
    private Date validFrom;

    @JsonProperty
    @Schema(description="The expiration date associated with the with certificate on the endpoint.")
    @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
    private Date validTo;

    @Schema(description="The identifier of the endpoint.")
    @JsonProperty
    private String id;

    /**
     * Copy constructor
     * @param that The endpoint to copy
     */
    protected EndPointInfo(EndPointInfo that) {
    	super(that.ipAddress, that.host);
        this.principalName = that.getPrincipalName();
        this.organization = that.organization;
        this.uniqueId = that.uniqueId;
        this.validFrom = that.validFrom;
        this.validTo = that.validTo;
        this.id = that.id;
        this.cipherSuite = that.cipherSuite;
    }

    @Schema(description="The FIPS state code associated with the jurisdiction associated with the endpoint.")
    @JsonProperty
    public String getFips() {
        return id == null ? null : id.toUpperCase();
    }
    
    /**
     * This getter is necessary to ensure that the serial number is reported
     * @return the serialNumber in decimal
     */
//    @Schema(description="The serial number in decimal associated with the with certificate on the endpoint.")
//    @JsonProperty
//    public String getSerialNumber() {
//        if (serialNumber == null) {
//            return null;
//        }
//        return serialNumber.toString(10);
//    }

    /**
     * @return the serialNumber in hexadecimal
     */
    @Schema(description="The serial number in hex associated with the with certificate on the endpoint.")
    @JsonProperty
    public String getSerialNumberHexOLDPAULTOCHANGEWITHPRINCIPAL() {
//        if (serialNumber == null) {
//            return null;
//        }
//        return serialNumber.toString(16);
        return "serialNumberHex not implemented";
    }

    public void setPrincipal(Principal principal) {
        if (principal == null) {
            principalName = null;
            organization = null;
            validFrom = null;
            validTo = null;
            uniqueId = null;
            return;
        }

        principalName = principal.getName();

//        // Get organization, and if not present, look for the Organizational Unit
//        String o = parts.get(X500Utils.ORGANIZATION);
//        if (StringUtils.isBlank(o)) {
//            o = parts.get(X500Utils.ORGANIZATION_UNIT);
//        }

        organization = principal.getOrganization();
        validFrom = principal.getValidFrom();
        validTo = principal.getValidTo();
        uniqueId = principal.getUniqueId();
    }

    // TODO: Paul Principal related code
    public void setCertificateORIGINAL(X509Certificate cert) {
//    	if (cert == null) {
//    		principalName = null;
//            organization = null;
//            validFrom = null;
//            validTo = null;
//            uniqueId = null;
//            return;
//    	}
//
//        X500Principal subject = cert.getSubjectX500Principal();
//        Map<String, String> parts = X500Utils.getParts(subject);
//
//        principalName = parts.get(X500Utils.COMMON_NAME);
//
//        // Get organization, and if not present, look for the Organizational Unit
//        String o = parts.get(X500Utils.ORGANIZATION);
//        if (StringUtils.isBlank(o)) {
//            o = parts.get(X500Utils.ORGANIZATION_UNIT);
//        }
//        organization = o;
//        validFrom = cert.getNotBefore();
//        validTo = cert.getNotAfter();
//        serialNumber = cert.getSerialNumber();
    }

    @Schema(description="The organization name in the certificate associated with the endpoint.")
    @JsonIgnore 
    public String getName() {
		return StringUtils.isBlank(organization) ? principalName : organization;
    }
}