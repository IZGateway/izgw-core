package test.gov.cdc.izgateway.regression;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import gov.cdc.izgateway.soap.fault.HubClientFault;
/**
 * Regression tests for issues that have been fixed and which can be verified via unit tests.
 *  
 * @author Audacious Inquiry
 *
 */
class RegressionTest {
	@Test
	void testIGDD_2392_UnexpectedNullPointerException() {
		// Test code to reproduce and verify the fix for IGDD-2392
		// None of these should throw NPEs
		assertDoesNotThrow(() -> {
			HubClientFault hcf = HubClientFault.clientThrewFault(null, null, 0, null, null, null);
			HubClientFault.getFaultName(hcf, null, null);
			HubClientFault.getFaultName(hcf, "", null);
			HubClientFault.firstChildIsFault(null);
			HubClientFault.documentElementIsError(null);
		});
	}
}
