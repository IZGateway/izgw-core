package gov.cdc.izgateway.logging.info;

import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.cdc.izgateway.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    protected String commonName;

    @Schema(description="The cipher suite used by the endpoint.")
    @JsonProperty
    protected String cipherSuite;

    @Schema(description="The organization associated with the with the endpoint.")
    @JsonProperty
    protected String organization;

    @Schema(description="The serial number associated with the with certificate on the endpoint.")
    @JsonProperty
    protected String serialNumber;

    protected String serialNumberHex;

    @JsonProperty
    @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
    @Schema(description="The starting date associated with the with certificate on the endpoint.")
    protected Date validFrom;

    @JsonProperty
    @Schema(description="The expiration date associated with the with certificate on the endpoint.")
    @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
    protected Date validTo;

    @Schema(description="The identifier of the endpoint.")
    @JsonProperty
    protected String id;

    /**
     * Copy constructor
     * @param that The endpoint to copy
     */
    protected EndPointInfo(EndPointInfo that) {
    	super(that.ipAddress, that.host);
        this.commonName = that.getCommonName();
        this.organization = that.organization;
        this.serialNumber = that.serialNumber;
        this.serialNumberHex = that.serialNumberHex;
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
    @Schema(description="The serial number in decimal associated with the with certificate on the endpoint.")
    @JsonProperty
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * @return the serialNumber in hexadecimal
     */
    @Schema(description="The serial number in hex associated with the with certificate on the endpoint.")
    @JsonProperty
    public String getSerialNumberHex() {
        return serialNumberHex;
    }

    @Schema(description="The organization name in the certificate associated with the endpoint.")
    @JsonIgnore 
    public String getName() {
		return StringUtils.isBlank(organization) ? commonName : organization;
    }
}