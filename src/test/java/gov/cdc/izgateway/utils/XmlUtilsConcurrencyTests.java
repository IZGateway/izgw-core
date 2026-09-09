package gov.cdc.izgateway.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * DocumentBuilder is not guaranteed thread-safe by the JAXP spec. XmlUtils used to share a
 * single static DocumentBuilder across all callers; concurrent submitSingleMessage/fault
 * handling on the Hub could corrupt its internal DOM-building state, occasionally producing a
 * Document that "successfully" parsed but had no children (see HubClientFaultTests).
 *
 * This hammers parseDocument() from many threads at once and checks that every thread got back
 * exactly the document it asked for, with no cross-contamination or missing content.
 */
class XmlUtilsConcurrencyTests {

	private static final int THREAD_COUNT = 32;
	private static final int ITERATIONS_PER_THREAD = 200;

	@Test
	void parseDocumentIsThreadSafeUnderConcurrentLoad() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
		CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
		List<Callable<String>> tasks = new ArrayList<>();

		for (int t = 0; t < THREAD_COUNT; t++) {
			final int threadId = t;
			tasks.add(() -> {
				barrier.await(); // maximize actual concurrent overlap in parse() calls
				for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
					final int iteration = i;
					String tag = "Root" + threadId;
					String value = "value-" + threadId + "-" + iteration;
					String xml = "<" + tag + ">" + value + "</" + tag + ">";

					Document doc = XmlUtils.parseDocument(xml);
					assertNotNull(doc, () -> "parseDocument returned null for thread " + threadId + " iteration " + iteration);

					Element root = doc.getDocumentElement();
					assertNotNull(root, () -> "document had no root element for thread " + threadId + " iteration " + iteration);
					assertEquals(tag, root.getTagName());
					assertEquals(value, root.getTextContent());
				}
				return null;
			});
		}

		List<Future<String>> results = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
		pool.shutdown();
		assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

		for (Future<String> result : results) {
			result.get(); // rethrows any AssertionError/exception raised inside the task
		}
	}
}
