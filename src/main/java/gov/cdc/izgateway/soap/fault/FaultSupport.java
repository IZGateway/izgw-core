package gov.cdc.izgateway.soap.fault;

import gov.cdc.izgateway.model.RetryStrategy;

/**
 * Access to this interface is supported by Faults created in this package to enable
 * structured diagnostics and logging with Faults.
 */
public interface FaultSupport {
    /** Name for Summary content in fault */
    public static final String SUMMARY = "Summary"; 
    /** Name for Retry content in fault */
    public static final String RETRY = "Retry";
    /** Name for Original content in fault */
    public static final String ORIGINAL = "Original";
    /** Diagnostics for the fault */
    public static final String DIAGNOSTICS = "Diagnostics";
    public static final String EVENTID = "EventID";
    /** The summary of the fault, used in logging to classify errors. This should be a short (< 20 characters)
     * descriptive summary of the fault.  Each fault should support just a few summary values and they
     * should be determined by the constructor rather than user provided string values.
     *
     * @return A summary of what went wrong.
     */
    String getSummary();
    /**
     * The details associated with what went wrong. This allows users to providee variation in fault details,
     * such as parameter causing the problem, or a more detailed description of the error.
     *
     * @return The details associated with the fault.
     */
    String getDetail();
    /**
     * The error message associated with the fault.  By convention, this is constructed in the form
     * Summary: Detail
     *
     * @return The error message.
     */
    String getMessage();
    /**
     * A human readable explanation of how to address the fault or what it means.  Explain in words that
     * would be meaningful to an end user.
     *
     * @return  The diagnostics associated with this fault.
     */
    String getDiagnostics();

    /**
     * The unique error code associated with this message.  This is a short numeric code suiteable
     * for automated interpretation that embodies the type of fault, the subtype and the retry strategy
     * to apply.  It is a 3 digit code in which the first digit indicate the fault type, the second
     * provides the subtype, and the third, the retry strategy.
     * @return The code for error
     */
    String getCode();

    /**
     * The retry strategy that should be applied by application software before retrying the communiication.
     *
     * @return  The retry strategy.
     */
    RetryStrategy getRetry();

    /**
     * The name of the fault to report in logging.
     * @return  The fault type
     */
    String getFaultName();
}
