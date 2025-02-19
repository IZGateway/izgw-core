package gov.cdc.izgateway.logging.info;

import java.security.cert.X509Certificate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.principal.CertificatePrincipalProviderImpl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A SourceInfo records information about the Source of inbound messages.
 * 
 */
@JsonPropertyOrder({"commonName", "organization", "validFrom", "validTo", "serialNumber", "serialNumberHex", "fips"})
@Schema(description = "Information common to all requesters")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class SourceInfo extends EndPointInfo {
	private static final long serialVersionUID = 1L;
	public static final String SOURCE_TYPE_INTERNAL = "IZGW Internal";
    public static final String SOURCE_TYPE_PROVIDER = "Provider Connect";
	public static final String SOURCE_TYPE_ADS = "IIS to CDC";
    @JsonProperty
    @Schema(description = "The facility identifier associated with the request")
	private String facilityId;
    @Schema(description = "The type of requester", allowableValues = {"IZGW Internal", "IIS Share", "Provider Connect", "Patient Access"})
    @JsonProperty
    private String type;

    public SourceInfo(IzgPrincipal izgPrincipal) {
        setPrincipal(izgPrincipal);
    }

    public SourceInfo(SourceInfo that) {
        super(that);
        this.facilityId = that.facilityId;
        this.type = that.type;
    }

    public void setPrincipal(IzgPrincipal principal) {
        if (principal == null) {
            commonName = null;
            organization = null;
            validFrom = null;
            validTo = null;
            serialNumber = null;
            serialNumberHex = null;
            return;
        }

        commonName = principal.getName();
        organization = principal.getOrganization();
        validFrom = principal.getValidFrom();
        validTo = principal.getValidTo();
        serialNumber = principal.getSerialNumber();
        serialNumberHex = principal.getSerialNumberHex();
    }

	/**
	 * Set the principal information from the certificate
	 * @param certificate
	 */
	public void setCertificate(X509Certificate certificate) {
		setPrincipal(CertificatePrincipalProviderImpl.createPrincipalFromCertificate(certificate));
	}

}
