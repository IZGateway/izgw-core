package gov.cdc.izgateway.logging.event;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import gov.cdc.izgateway.common.Constants;
import gov.cdc.izgateway.utils.SystemUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;

import java.lang.management.ManagementFactory;
import java.util.Date;

@JsonPropertyOrder({ 
	"isHealthy", "statusAt", "lastChangeReason",
	"started", "startupTime",
	"buildName", "serverName", "environment",  
	"lastHealthyDate", "lastUnhealthyDate", "eventCount", 
	"requestVolume", "successVolume" }
)
@Data 
public class Health {
	@JsonProperty 
	@Schema(description="True if the server is healthy")
	private boolean healthy = false;
	
	@JsonProperty @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	@Schema(description="Timestamp of the health report")
    private Date statusAt = null;
	
	@Schema(description="Reason for the last health update")
	private String lastChangeReason = null;

	@JsonProperty @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	@Schema(description="Start timestamp of the server")
	private final Date started;

	@JsonProperty 
	@Schema(description="Milliseconds taken for the server to startup")
	private long startupTime = -1;

	@JsonProperty 
	@Schema(description="Build identifier for the server")
	private String buildName;
	
	@JsonProperty 
	@Schema(description="Server DNS name")
	private String serverName;

	@JsonProperty 
	@Schema(description="Server environment",allowableValues= {"Production", "Testing", "Onboarding", "Staging", "Development" })
	private String environment = null;

	@JsonProperty @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
    @Getter 
	@Schema(description="Timestamp when server was last marked healthy")
    private Date lastHealthyDate = null;

	@JsonProperty @JsonFormat(shape=Shape.STRING, pattern = Constants.TIMESTAMP_FORMAT)
	@Getter 
	@Schema(description="Timestamp when server was last marked unhealthy")
    private Date lastUnhealthyDate = null;

	@JsonProperty 
	@Schema(description="Number of health events")
	private int eventCount = 0;
    
	@JsonIgnore 
	private Throwable lastException = null;
    
	@JsonProperty 
	@Schema(description="Number of requests received")
	AtomicInteger requestVolume = new AtomicInteger(0);
    
	@JsonProperty 
	@Schema(description="Number of successful requests")
	AtomicInteger successVolume = new AtomicInteger(0);
    
	@JsonProperty 
	@Schema(description="Host name as known by the operating system")
	private String hostname;
	
	@JsonProperty
	@Schema(description="The database in use")
	private String database;
	
	@JsonProperty
	@Schema(description="This Host's DNS Address", example="3.232.82.52") // NOSONAR This is an example address
	private String[] ingressDnsAddress;
	
	@JsonProperty
	@Schema(description="This Host's Egress IP Address", example="54.87.148.103") // NOSONAR This is an example address
	private String egressDnsAddress;
	
    public Health() {
        started = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
        environment = SystemUtils.getDestTypeAsString();
        statusAt = new Date();
        hostname = SystemUtils.getHostname();
    }

    private Health(Health that) {
        this.healthy = that.healthy;
        this.statusAt = new Date();
        this.lastChangeReason = that.lastChangeReason;
        
        this.started = that.started;
        this.startupTime = that.startupTime;

        this.buildName = that.buildName;
        this.serverName = that.serverName;
        this.environment = that.environment;
        
        this.lastException = that.lastException;
        this.lastHealthyDate = that.lastHealthyDate;
        this.lastUnhealthyDate = that.lastUnhealthyDate;
        this.eventCount = that.eventCount;
        
        this.requestVolume = that.requestVolume;
        this.successVolume = that.successVolume;
        this.hostname = that.hostname;
        this.database = that.database;
        
        this.ingressDnsAddress = that.ingressDnsAddress;
        this.egressDnsAddress = that.egressDnsAddress;
    }

    public Health copy() {
        return new Health(this);
    }
    
    public void setHealthy(boolean healthy) {
    	this.healthy = healthy;
    	if (healthy) {
        	if (lastHealthyDate == null) {
        		setLastHealthyDate(new Date());
        		startupTime = lastHealthyDate.getTime() - started.getTime();
        	} else if (lastUnhealthyDate != null && lastHealthyDate.before(lastUnhealthyDate)){
        		setLastHealthyDate(new Date());
        	}
    	} else {
    		setLastUnhealthyDate(new Date());
    	}
    }
    /**
     * @return The message associated with the LastHealthException.
     */
    @JsonProperty
	@Schema(description="Last exception causing an unhealthy status")
    public String getLastException() {
        return lastException == null ? null : lastException.getMessage();
    }

    @JsonIgnore
	public Throwable getLastExceptionInternal() {
		return lastException;
	}

	public long getRequestVolume() {
		return requestVolume.get();
	}

	public long getSuccessVolume() {
		return successVolume.get();
	}

	public void incrementRequestVolume() {
		requestVolume.incrementAndGet();
	}

	public void incrementSuccessVolume() {
		successVolume.incrementAndGet();
	}
}