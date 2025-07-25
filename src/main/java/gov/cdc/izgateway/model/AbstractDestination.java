package gov.cdc.izgateway.model;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.common.HasDestinationUri;
import gov.cdc.izgateway.service.IJurisdictionService;
import gov.cdc.izgateway.utils.SystemUtils;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AbstractDestination implements most of the necessary functionality for a Destination entity
 * @author Audacious Inquiry
 */
@MappedSuperclass
@SuppressWarnings("serial")
@Data
@JsonPropertyOrder({ "destId", "destType", "destUri", "destVersion", "facilityId", "msh3", "msh4", "msh5", "msh6",
	"msh22", "rxa11" })
public abstract class AbstractDestination implements IEndpoint, Serializable, IDestination {
	/**
	 * A destination id.
	 * A composite of the destination endpoint identifier, and the environment id (a.k.a., destination type). 
	 * @author Audacious Inquiry
	 */
	@Schema(properties= {
		@StringToClassMapItem(key="ak", value=AbstractDestination.class),
		@StringToClassMapItem(key="dev", value=AbstractDestination.class),
		@StringToClassMapItem(key="dex", value=AbstractDestination.class),
		@StringToClassMapItem(key="dex-dev", value=AbstractDestination.class),
		@StringToClassMapItem(key="md", value=AbstractDestination.class),
		@StringToClassMapItem(key="md_c", value=AbstractDestination.class),
		@StringToClassMapItem(key="wy", value=AbstractDestination.class)
	})
	@Embeddable
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DestinationId implements Serializable, IDestinationId {
		@Column(name="dest_id")
		private String destId;
		@Column(name="dest_type")
		private int destType;

		/**
		 * Constructor for a destination id
		 * @param destId
		 */
		public DestinationId(String destId) {
			this.destId = destId;
			this.destType = SystemUtils.getDestType();
		}
		/**
		 * Create a new destination identifier from an existing one. 
		 * @param id
		 */
		public DestinationId(IDestinationId id) {
			this.destId = id.getDestId();
			this.destType = id.getDestType();
		}
		
		@Override
		public DestinationId copy() {
			return new DestinationId(destId, destType);
		}

		@Override
		public void setDestType(String destType) {
			int result = SystemUtils.getDestTypeId(destType);
			if (result < 1) {
				throw new IllegalArgumentException("Destination Type must come from " + SystemUtils.getDestTypes());
			}
			this.destType = result;
		}

		@Override
		public void setDestType(int destType) {
			this.destType = destType;
		}
		
		@Override
		public String toString() {
			return String.format("%d#%s", destType, destId);
		}
	}

	@EmbeddedId
	@Schema(hidden = true)
	private DestinationId id = new DestinationId();
	
	@Column(name = "dest_uri")
	@Schema(description = "The destination endpoint URL", pattern=ID_PATTERN)
	private String destUri;// NOT NULL

	@Column(name = "username")
	@JsonIgnore
	@Schema(description = "The destination endpoint username", hidden=true)
	private String username;

	@Column(name = "password")
	@JsonIgnore
	@Schema(description = "The destination endpoint password", hidden=true)
	private String password;
	
	@Column(name = "pass_expiry")
	@JsonIgnore
	@Schema(description = "The expiration date of the password", hidden=true)
	private Date passExpiry;

	@Column(name = "dest_version")
	@Schema(description = "The schema or protocol version for use with the endpoint", 
		hidden=true, pattern="2011|2014|V2022-12-31|DEX1.0|DEX2.0")
	private String destVersion;

	@Column(name = "jurisdiction_id")
	@JsonIgnore
	@Schema(description = "The jurisdiction responsible for the endpoint", hidden=true)
	private int jurisdictionId;

	@Column(name = "maint_reason")
	@Schema(description = "The reason for destination maintenance")
	private String maintReason;

	@Schema(description = "The start of the maintenance period")
	@Column(name = "maint_start")
	@JsonFormat(shape = Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	private Date maintStart;

	@Column(name = "maint_end")
	@Schema(description = "The end of the maintenance period, or null if unspecified")
	private Date maintEnd;

	@Column(name = "facility_id")
	@Schema(description = "The identifier of the facility to use with test messages for this endpoint")
	private String facilityId;

	@Column(name = "MSH3")
	@Schema(description = "The MSH3 value to use with test messages for this endpoint")
	private String msh3;
	
	@Column(name = "MSH4")
	@Schema(description = "The MSH4 value to use with test messages for this endpoint")
	private String msh4;
	
	@Column(name = "MSH5")
	@Schema(description = "The MSH5 value to use with test messages for this endpoint")
	private String msh5;
	
	@Column(name = "MSH6")
	@Schema(description = "The MSH6 value to use with test messages for this endpoint")
	private String msh6;
	
	@Column(name = "MSH22")
	@Schema(description = "The MSH22 value to use with test messages for this endpoint")
	private String msh22;
	
	@Column(name = "RXA11")
	@Schema(description = "The RXA11 value to use with test messages for this endpoint")
	private String rxa11;
	
	/**
	 * Default constructor
	 */
	protected AbstractDestination() {
	}
	
	/**
	 * Create a named destination for the current environment
	 * @param destId	The destination id
	 */
	protected AbstractDestination(String destId) {
		id.setDestId(destId);
		id.setDestType(SystemUtils.getDestType());
	}
	
	/**
	 * Create a destination with the given id
	 * @param that	The destination id
	 */
	protected AbstractDestination(IDestination that) {
		this.id = new DestinationId(that.getId());	// Always make a copy
		this.destUri = that.getDestUri();
		this.destVersion = that.getDestVersion();
		this.username = that.getUsername();
		this.password = that.getPassword();
		this.jurisdictionId = that.getJurisdictionId();
		this.maintReason = that.getMaintReason();
		this.maintStart = that.getMaintStart();
		this.maintEnd = that.getMaintEnd();
		this.facilityId = that.getFacilityId();
		this.msh3 = that.getMsh3();
		this.msh4 = that.getMsh4();
		this.msh5 = that.getMsh5();
		this.msh6 = that.getMsh6();
		this.msh22 = that.getMsh22();
		this.rxa11 = that.getRxa11();
	}

	
	@Schema(description = "The type of destination")
	public String getDestType() {
		return SystemUtils.getDestTypes().get(id.getDestType()-1);
	}

	@JsonIgnore
	public int getDestTypeId() {
		return id.getDestType();
	}

	@Override
	public void setDestTypeId(int destType) {
		this.id.setDestType(destType);
	}
	

	@Schema(description = "The IIS or other name for the jurisdiction")
	public String getJurisdictionName() {
		IJurisdiction j = getJurisdictionService().getJurisdiction(jurisdictionId);
		return j == null ? null : j.getName();
	}

	@Schema(description = "A description of the jurisdiction (typically the state or other name)")
	public String getJurisdictionDesc() {
		IJurisdiction j = getJurisdictionService().getJurisdiction(jurisdictionId);
		return j == null ? null : j.getDescription();
	}
	
	/**
	 * @return The JurisdictionService for this environment
	 */
	public abstract IJurisdictionService getJurisdictionService();

	@Override
	@JsonIgnore
	public boolean isUnderMaintenance() {
		Date now = new Date();
		if (!StringUtils.isEmpty(maintReason)) {
			return (maintStart == null || now.after(maintStart)) && (maintEnd == null || now.before(maintEnd));
		}
		return false;
	}
	
	@Override
	@JsonIgnore
	public String getMaintenanceDetail() {
		String detail = String.format("Destination %s in %s under maintenance from %tc until ", getDestId(),
				SystemUtils.getDestTypeAsString(), getMaintStart());
		if (getMaintEnd() != null) {
			detail += String.format("%tc", getMaintEnd());
		} else {
			detail += " further notice.";
		}
		return detail;
	}

	@Override
	@Schema(description = "The destination identifier")
	public String getDestId() {
		return id.getDestId();
	}

	@JsonIgnore
	@Schema(description = "The destination id", hidden=true)
	@Override
	public String getDestinationId() {
		return id.getDestId();
	}

	@JsonIgnore
	@Schema(description = "The destination uri", hidden=true)
	@Override
	public String getDestinationUri() {
		return getDestUri();
	}	
	
	protected AbstractDestination maskCredentials() {
		setUsername(null);
		setPassword(null);
		return this;
	}

	@Override
	public void setId(IDestinationId id) {
		if (id instanceof DestinationId did) {
			this.id = did;
		} else {
			this.id = new DestinationId(id);
		}
	}
}
