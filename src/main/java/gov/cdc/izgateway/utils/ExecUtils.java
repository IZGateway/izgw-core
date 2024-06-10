package gov.cdc.izgateway.utils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ExecUtils {

	private ExecUtils() {}

	public static boolean waitFor(long maxDelay, BooleanSupplier t) {
		long stopTime = System.currentTimeMillis() + maxDelay;
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} while (System.currentTimeMillis() < stopTime && !Boolean.TRUE.equals(t.getAsBoolean()));
		return Boolean.TRUE.equals(t.getAsBoolean());
	}
	
    
	public static <T> boolean execAll(List<T> l, Consumer<T> consumer, int wait, TimeUnit units) {
		ExecutorService exec = Executors.newFixedThreadPool(Math.max(4, wait));
		l.forEach(consumer::accept);
		exec.shutdown();
		boolean allComplete = false;
		try {
			allComplete = exec.awaitTermination(wait, units);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return allComplete;
	}
}
