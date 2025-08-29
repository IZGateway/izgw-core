package gov.cdc.izgateway.repository;

import java.util.List;
import java.util.Map;

/**
 * The HostRepository retrieves a list of all hosts that have reported in the last few minutes.
 * @author Audacious Inquiry
 *
 */
public interface IHostRepository {
	/**
	 * Find all hosts that have reported in the last few minutes.
	 * @return A list of host names
	 */
	List<String> findAll();
	/**
	 * Get a map of hosts and their regions that have reported in the last few minutes.
	 * @return A map of host names to their regions
	 */
	Map<String, String> getHostsAndRegion();

}