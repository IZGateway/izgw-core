package gov.cdc.izgateway.logging;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * In memory appender to support log testing.  This Appender keeps the last 50 log messages in memory so that they
 * can be retrieved primarily for testing log outputs during integration testing.  However, log events are not turned
 * off in production, so they may also be of some small diagnostic value. 
 * 
 * See https://www.baeldung.com/junit-asserting-logs
 */
public class MemoryAppender extends ListAppender<ILoggingEvent> {
    /** For now, just keep track of all MemoryAppenders in a map, using softReferences so that they can be GC'd */
    private static final Map<String, SoftReference<MemoryAppender>> appenders = new TreeMap<>();

    /**
     * A RingBuffer is a first in first out queue that removes older entries when the queue reaches 
     * the maximum length.  
     *
     * @param <T> The type of object to keep in the queue.
     */
    static class RingBuffer<T> extends LinkedList<T> {
        private static final long serialVersionUID = 1L;
        private final int maxSize;
        RingBuffer(int maxSize) {
            this.maxSize = maxSize;
        }
        @Override
        public
        boolean add(T e) {
            super.add(e);
            while (size() > maxSize) {
                removeFirst();
            }
            return true;
        }
        @Override
        public boolean equals(Object that) {
        	return super.equals(that);
        }
        @Override
        public int hashCode() {
        	return super.hashCode();
        }
    }

    public MemoryAppender() {
        super();
        // 50 is sufficient for log testing
        list = new RingBuffer<ILoggingEvent>(50);
    }

    public static MemoryAppender getInstance(String name) {
        SoftReference<MemoryAppender> ref = appenders.get(name);
        if (ref == null) {
            return null;
        }
        MemoryAppender value = ref.get();
        if (value == null) {
            // Remove this useless key to a GC-ed Appender
            appenders.remove(name);
        }
        return value;
    }

    @Override
    public void setName(String name) {
        String oldName = this.name;
        super.setName(name);
        // Keep track of the appender by name
        if (!Objects.equals(oldName, name)) {
            if (oldName != null) {
                appenders.remove(oldName);
            }
            appenders.put(name, new SoftReference<>(this));
        }
    }

    public void reset() {
        this.list.clear();
    }

    public boolean contains(String string, Level level) {
        return this.list.stream()
          .anyMatch(event -> event.toString().contains(string)
            && event.getLevel().equals(level));
    }

    public int countEventsForLogger(String loggerName) {
        return (int) this.list.stream()
          .filter(event -> event.getLoggerName().contains(loggerName))
          .count();
    }

    public List<ILoggingEvent> search(String string) {
        return this.list.stream()
          .filter(event -> event.toString().contains(string)).toList();
    }

    public List<ILoggingEvent> search(String string, Level level) {
        return this.list.stream()
          .filter(event -> event.toString().contains(string)
            && event.getLevel().equals(level)).toList();
    }

    public int getSize() {
        return this.list.size();
    }

    public List<ILoggingEvent> getLoggedEvents() {
        return new ArrayList<>(this.list);
    }
}
