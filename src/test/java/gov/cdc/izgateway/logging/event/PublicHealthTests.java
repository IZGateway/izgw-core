package gov.cdc.izgateway.logging.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Verifies the public view of a health report discloses nothing beyond the liveness answer.
 *
 * @see Health#publicView()
 */
class PublicHealthTests {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** The only keys an unauthenticated caller may receive. Changing this changes what we disclose. */
	private static final Set<String> ALLOWED_KEYS = Set.of("healthy", "statusAt");

	private static Health populatedHealth() {
		Health h = new Health();
		h.setHealthy(true);
		h.setLastChangeReason("Database izgateway-prod.example.internal came back");
		h.setBuildName("3.8.0-1234");
		h.setServerName("phiz-web-ws-7.prod.internal");
		h.setDatabase("jdbc:mysql://izgateway-prod.cluster-abc123.us-east-1.rds.amazonaws.com:3306/izgw");
		h.setIngressDnsAddress(new String[] { "3.232.82.52" });
		h.setEgressDnsAddress("54.87.148.103");
		h.setAccessControlAction("warn");
		h.setLastException(new IllegalStateException("connection refused to 10.0.3.44:3306"));
		h.incrementRequestVolume();
		h.incrementSuccessVolume();
		return h;
	}

	/**
	 * The guard that matters: the serialized public view carries exactly the allowed keys.
	 * A new field added to Health does not reach an unauthenticated caller, and if someone
	 * adds one to PublicHealth instead, this fails rather than leaking quietly.
	 */
	@Test
	void publicViewSerializesOnlyTheAllowedKeys() throws Exception {
		JsonNode json = MAPPER.valueToTree(populatedHealth().publicView());

		Set<String> actual = new TreeSet<>();
		json.fieldNames().forEachRemaining(actual::add);

		assertEquals(new TreeSet<>(ALLOWED_KEYS), actual,
				"The public health view must disclose exactly " + ALLOWED_KEYS);
	}

	/** None of the infrastructure detail appears anywhere in the public payload, under any key. */
	@Test
	void publicViewDisclosesNoInfrastructureDetail() throws Exception {
		String json = MAPPER.writeValueAsString(populatedHealth().publicView());

		for (String secret : new String[] {
				"phiz-web-ws-7.prod.internal",
				"izgateway-prod.cluster-abc123.us-east-1.rds.amazonaws.com",
				"3.232.82.52",
				"54.87.148.103",
				"3.8.0-1234",
				"warn",
				"Database izgateway-prod.example.internal came back",
				"connection refused",
				"10.0.3.44" }) {
			assertFalse(json.contains(secret), "Public health view disclosed: " + secret);
		}
	}

	/**
	 * The exception message is the easiest of these to miss: {@code Health}'s {@code lastException}
	 * field is {@code @JsonIgnore}d, which makes it look withheld, but the hand-written
	 * {@link Health#getLastException()} is {@code @JsonProperty} and puts the message on the wire.
	 * Pinned against the full report as well so that the premise behind this test stays true.
	 */
	@Test
	void theFullReportIsWhatDisclosesTheExceptionMessage() throws Exception {
		Health h = populatedHealth();

		assertTrue(MAPPER.writeValueAsString(h).contains("connection refused"),
				"If the full report stops carrying the exception message, this allowlist entry is moot");
		assertFalse(MAPPER.writeValueAsString(h.publicView()).contains("connection refused"));
	}

	/** The full report is unchanged for authenticated callers; publicView() does not mutate it. */
	@Test
	void publicViewLeavesTheFullReportIntact() throws Exception {
		Health h = populatedHealth();
		String before = MAPPER.writeValueAsString(h);

		h.publicView();

		assertEquals(before, MAPPER.writeValueAsString(h));
		assertTrue(before.contains("phiz-web-ws-7.prod.internal"),
				"The full report should still carry the detail an authenticated caller expects");
	}

	/** The liveness answer itself survives, since that is what the endpoint is for. */
	@Test
	void publicViewCarriesTheLivenessAnswer() {
		Health unhealthy = new Health();
		unhealthy.setHealthy(false);

		assertTrue(populatedHealth().publicView().isHealthy());
		assertFalse(unhealthy.publicView().isHealthy());
	}

	/** The timestamp is copied, so a caller cannot reach back into the live report through it. */
	@Test
	void statusAtIsDefensivelyCopied() {
		Health h = populatedHealth();
		PublicHealth view = h.publicView();

		assertNotSame(h.getStatusAt(), view.getStatusAt());
		assertEquals(h.getStatusAt(), view.getStatusAt());
		assertNotSame(view.getStatusAt(), view.getStatusAt());
	}
}
