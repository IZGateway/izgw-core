package gov.cdc.izgateway.model;

import org.springframework.http.HttpStatus;

public enum RetryStrategy {
    /**
     * A possible transient error. The normal retry strategy would be to retry ONCE immediately,
     * then fall back ~ 5 minutes before trying again, then again at 10 minutes.  If after 2 to 3
     * attempts the message still fails, it should be queued somewhere for a final retry attempt
     * performed several hours in the future (e.g., a nightly retry job, or a retry queue that attempts
     * to send messages no more frequently than three times spread out through the day).
     *
     * On multiple occurences of the same error to the same destination, move to the CHECK_IIS_STATUS
     * strategy.
     */
    NORMAL("Use normal retry strategy", HttpStatus.SERVICE_UNAVAILABLE),
    /**
     * The message content itself is the cause of the error.  It must be corrected, and this usually
     * involves human intervention.  The message should be queued somewhere for a person to inspect and
     * correct before it is retried.
     */
    CORRECT_MESSAGE("Correct message before retrying again", HttpStatus.BAD_REQUEST),
    /**
     * The IIS is not responsive for some reason. This may be due to networking infrastructure (Internet)
     * failures between the IZ Gateway and the IIS, or it may be related to routine or emergency IIS
     * maintenance.  Check the IIS Status before attempting a retry.  Some errors (e.g. DNS not found,
     * expired certificates, or invalid response data) will not disappear without human intervention.
     */
    CHECK_IIS_STATUS("Check IIS Status before Retry", HttpStatus.BAD_GATEWAY),

    /**
     * Some times it doesn't matter what the sender or the receiver does, the problem won't be fixed until the problem
     * is addressed in the IZ Gateway application or configuration. For these problems, contact IZ Gateway
     * support, and retry the message after support has addressed the issue.
     */
    CONTACT_SUPPORT("Contact support before retrying", HttpStatus.INTERNAL_SERVER_ERROR);
    private final String message;
	private final HttpStatus status;
    RetryStrategy(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
    public String getRetryCode() {
        return Integer.toString(ordinal());
    }
    
    public HttpStatus getStatus() {
    	return status;
    }
    @Override
    public String toString() {
        return super.toString() + ": " + message;
    }
    
    /**
     * @return true if this message could be retried successfully
     */
    public boolean isRetryable() {
    	return this == NORMAL || this == CHECK_IIS_STATUS;
    }
}