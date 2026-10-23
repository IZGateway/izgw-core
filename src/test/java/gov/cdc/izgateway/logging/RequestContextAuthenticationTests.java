package gov.cdc.izgateway.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gov.cdc.izgateway.security.IzgPrincipal;
import gov.cdc.izgateway.security.UnauthenticatedPrincipal;

/**
 * Verifies the authentication test used to decide how much detail a response may carry.
 *
 * @see RequestContext#isAuthenticated()
 */
class RequestContextAuthenticationTests {

	/** Stands in for a principal produced by a real authentication mechanism. */
	private static class AuthenticatedTestPrincipal extends IzgPrincipal {
		AuthenticatedTestPrincipal() {
			setName("cn=test.example.org");
		}

		@Override
		public String getSerialNumberHex() {
			return "00";
		}
	}

	@AfterEach
	void clearContext() {
		RequestContext.clear();
	}

	/** No principal on the thread counts as unauthenticated rather than throwing. */
	@Test
	void missingPrincipalIsNotAuthenticated() {
		assertFalse(RequestContext.isAuthenticated());
	}

	@Test
	void unauthenticatedPrincipalIsNotAuthenticated() {
		RequestContext.setPrincipal(new UnauthenticatedPrincipal());
		assertFalse(RequestContext.isAuthenticated());
		assertFalse(new UnauthenticatedPrincipal().isAuthenticated());
	}

	/**
	 * A principal type that does not opt out is authenticated. This is the default that keeps
	 * certificate and JWT callers seeing the detail they see today.
	 */
	@Test
	void otherPrincipalsAreAuthenticated() {
		RequestContext.setPrincipal(new AuthenticatedTestPrincipal());
		assertTrue(RequestContext.isAuthenticated());
	}
}
