package gov.cdc.izgateway.common;

import java.util.Collections;
import java.util.List;

/**
 * A runtime exception that aggregates multiple exceptions thrown during a bulk operation.
 * <p>
 * Use this when a loop must continue processing all items regardless of per-item failures,
 * collecting each failure and throwing a single combined exception at the end so no errors
 * are silently discarded.
 * </p>
 *
 * <pre>
 * List&lt;RuntimeException&gt; errors = new ArrayList&lt;&gt;();
 * for (Item item : items) {
 *     try {
 *         process(item);
 *     } catch (RuntimeException e) {
 *         errors.add(e);
 *     }
 * }
 * AggregateException.throwIfAny(errors, "processing items");
 * </pre>
 */
@SuppressWarnings("serial")
public class AggregateException extends RuntimeException {

    /** The individual exceptions collected during the bulk operation. */
    private final List<RuntimeException> causes;

    /**
     * Creates an {@code AggregateException} from a list of collected exceptions.
     *
     * @param message a description of the bulk operation that produced the errors
     * @param causes  the individual exceptions; must not be {@code null} or empty
     */
    public AggregateException(String message, List<RuntimeException> causes) {
        super(buildMessage(message, causes == null ? java.util.List.of() : causes),
              (causes == null || causes.isEmpty()) ? null : causes.get(0));
        this.causes = java.util.List.copyOf(causes == null ? java.util.List.of() : causes);
        for (int i = 1; i < this.causes.size(); i++) {
            addSuppressed(this.causes.get(i));
        }
    }

    /**
     * Returns the unmodifiable list of individual exceptions collected during the operation.
     *
     * @return the collected exceptions; never {@code null}, never empty
     */
    public List<RuntimeException> getCauses() {
        return causes;
    }

    /**
     * Throws an {@code AggregateException} if {@code errors} is non-empty; otherwise returns
     * silently. Intended as a convenient end-of-loop guard.
     *
     * @param errors      the exceptions collected during a bulk operation
     * @param operationDescription a short label used in the exception message (e.g. {@code "resetting circuit breakers"})
     * @throws AggregateException if {@code errors} is non-empty
     */
    public static void throwIfAny(List<RuntimeException> errors, String operationDescription) {
        if (errors != null && !errors.isEmpty()) {
            throw new AggregateException(operationDescription, errors);
        }
    }

    private static String buildMessage(String operation, List<RuntimeException> causes) {
        StringBuilder sb = new StringBuilder();
        sb.append(causes.size())
          .append(" error(s) occurred while ")
          .append(operation)
          .append(':');
        for (int i = 0; i < causes.size(); i++) {
            sb.append('\n')
              .append(i + 1)
              .append(". ")
              .append(causes.get(i).getMessage());
        }
        return sb.toString();
    }
}
