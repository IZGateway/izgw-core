package gov.cdc.izgateway.security;

import gov.cdc.izgateway.utils.ExecUtils;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

@RestController
@CrossOrigin
@RolesAllowed({Roles.OPEN, Roles.ADMIN})
@RequestMapping({"/rest"})
@Lazy(false)
public class TrustController {
	private final ClientTlsConfiguration tlsConfig;
	private final ClientTlsSupport tlsSupport;
	
	@Autowired
	public TrustController(AccessControlRegistry registry, ClientTlsSupport tlsSupport) {
		registry.register(this);
		this.tlsSupport = tlsSupport;
		this.tlsConfig = tlsSupport.getConfig();
	}

	/**
	 * Report on trust parameters status.
	 * 
	 * @param req    The request
	 * @param resp   The response
	 * @param test   If true, test parameter reloading.
	 * @param reload If true, force parameter reloading (for admin purposes).
	 * @return An array of objects reporting status of trust material.
	 */
	@GetMapping("/trust")
	public Map<String, Object> getTrust(
			@RequestParam(defaultValue = "false") boolean test, @RequestParam(defaultValue = "false") boolean reload) {
		boolean success = true;
		int numKeyStoreReloads = tlsConfig.getClientKeyStore().getReloadCount();
		int numTrustStoreReloads = tlsConfig.getClientTrustStore().getReloadCount();
		Predicate<ClientTlsConfiguration> p = t -> 
			t.getClientKeyStore().getReloadCount() > numKeyStoreReloads && 
			t.getClientTrustStore().getReloadCount() > numTrustStoreReloads; 
		if (test || reload) {
			// Touch the files to force a reload of the content.
			touch(tlsConfig.getClientKeyStore().getFile());
			touch(tlsConfig.getClientTrustStore().getFile());
			if (reload) {
				// Force an immediate reload
				tlsSupport.updateTrust();
				success = p.test(tlsConfig);
			} else {
				// For test, wait just a little longer than than the polling interval to address timing issues.
				success = ExecUtils.waitFor(
					Duration.ofSeconds(tlsConfig.getMonitoringPeriod() + 2l).toMillis(),
					() -> p.test(tlsConfig)
				);
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("Server", new TrustData(tlsConfig.getClientKeyStore()));
		result.put("Client", new TrustData(tlsConfig.getClientTrustStore()));

		if (test || reload) {
			result.put("status", success ? "Reloaded"
					: ("Key or Trust Material not reloaded after " + tlsConfig.getMonitoringPeriod() + " seconds"));
		}
		return result;
	}

	private void touch(String filename) {
		File f = new File(filename);
		f.setLastModified(System.currentTimeMillis()); // NOSONAR We don't care about return value 
	}

}