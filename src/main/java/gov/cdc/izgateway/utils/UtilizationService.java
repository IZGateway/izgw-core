package gov.cdc.izgateway.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import gov.cdc.izgateway.logging.markers.Markers2;

// import gov.cdc.izgateway.logging.Markers2;

/**
 * A set of gauges for operating system settings.  See http://blog.progs.be/679/log-jvm-os-metrics-spring
 * upon which this code is based.
 */
@Slf4j
@Data
public class UtilizationService {
	private static Utilization mostRecent = null;
	private UtilizationService() {}
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    @SuppressWarnings("unused")
	private static final Optional<Method> processCpuTime = getMethod(osBean, "getProcessCpuTime");
    private static final Optional<Method> processCpuLoad = getMethod(osBean, "getProcessCpuLoad");
    private static final Optional<Method> systemCpuLoad = getMethod(osBean, "getSystemCpuLoad");
    private static final File STORAGE = new File(".");
    
	@Data
	@AllArgsConstructor
	public static class Utilization {
	    private final double processCpuLoad;
	    private final double systemCpuLoad;
	    private final MemoryUsage heapMemoryUsage;
	    private final MemoryUsage nonHeapMemoryUsage;
	    private final long freeDiskSpace;
	    private final long totalDiskSpace;
	    public long getUsedDiskSpace() {
	    	return totalDiskSpace - freeDiskSpace;
	    }
	}
	
    private static Optional<Method> getMethod(Object osBean, String name) {
        try {
            final Method method = osBean.getClass().getDeclaredMethod(name);
            // This must be called to make the subclass method accessible, just in case.
            method.setAccessible(true);  // NOSONAR Yes, thank you, we know this is implementation dependent
            return Optional.of(method);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        } catch (InaccessibleObjectException ex) {
            // Protect from modules access violations in JDK 9 or later
            log.error(Markers2.append(ex), "{}", ex.getMessage());
            return Optional.empty();
        }
    }

    public static Utilization getUtilization() {
    	mostRecent = new Utilization(
    		getProcessCpuLoad(),
    		getSystemCpuLoad(),
    		memoryBean.getHeapMemoryUsage(),
    		memoryBean.getNonHeapMemoryUsage(),
    		STORAGE.getFreeSpace(),
    		STORAGE.getTotalSpace()
    	);
    	return mostRecent;
    }
    
    public static Utilization getMostRecent() {
    	return mostRecent;
    }
    
    private static double getProcessCpuLoad() {
    	return  Math.max(invokeDouble(processCpuLoad), 0.0);
    }
    
    private static double getSystemCpuLoad() {
    	return  Math.max(invokeDouble(systemCpuLoad), 0.0);
    }
    
    @SuppressWarnings("unused")
	private static long invokeLong(Optional<Method> method) {
        if (method.isPresent()) {
            try {
                return (long) method.get().invoke(osBean);
            } catch (IllegalAccessException | InvocationTargetException ite) {
                return 0L;
            }
        }
        return 0L;
    }

    private static double invokeDouble(Optional<Method> method) {
        if (method.isPresent()) {
            try {
                return (double) method.get().invoke(osBean);
            } catch (IllegalAccessException | InvocationTargetException ite) {
                return 0.0;
            }
        }
        return 0.0;
    }
}