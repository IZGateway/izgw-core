package gov.cdc.izgateway.service;

import ca.uhn.fhir.rest.param.DateRangeParam;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

/**
 * Contract for back-end query implementations used by the FHIR query entry point.
 * <p>
 * Each implementation handles a distinct class of destinations (e.g., IZ Gateway hub,
 * SQL database). {@code FhirController} selects the first registered backend whose
 * {@link #supports(String)} returns {@code true} for the requested destination.
 * </p>
 */
public interface IQueryBackend {

    /**
     * Returns {@code true} if this backend handles the given destination identifier.
     */
    boolean supports(String destinationId);

    /**
     * Executes a single-patient immunization query against this backend.
     *
     * @param searchPatient  FHIR Patient populated with search demographics
     * @param destinationId  destination identifier selected by the caller
     * @param lastUpdated    optional {@code _lastUpdated} date range; {@code null} when absent;
     *                       implementations MUST apply this as a server-side predicate, not a post-filter
     * @param req            originating HTTP request (for security context and header access)
     * @return FHIR Bundle of type searchset; empty bundle when no match found
     * @throws Exception implementations may throw any checked exception;
     *                   {@code FhirController} maps these to appropriate HTTP responses
     */
    Bundle query(Patient searchPatient, String destinationId, DateRangeParam lastUpdated,
                 HttpServletRequest req) throws Exception;
}
