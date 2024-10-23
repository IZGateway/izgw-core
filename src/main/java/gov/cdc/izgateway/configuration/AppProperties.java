package gov.cdc.izgateway.configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
public class AppProperties {

	public static final String PROD_MODE_VALUE = "prod";
	public static final String DEV_MODE_VALUE = "dev";
	private static AppProperties instance;
	
	@Getter
	@Value("${server.hostname:dev.izgateway.org}")
	private String serverName;
	@Getter
	@Value("${server.mode:prod}")
	private String serverMode;
	
	@Getter
	@Value("${spring.database:jpa}")
	private String databaseType;

	@Getter
	private final ScheduledExecutorService scheduler = 
		Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Background-Scheduler"));
	
	public AppProperties() {
		setInstance(this);
	}
	private static void setInstance(AppProperties instance) {
		AppProperties.instance = instance;
	}
	public boolean isProd() {
		return !"dev".equals(instance.serverMode);
	}
	public static boolean isProduction() {
		// The only time data is NOT treated as production with PHI masking is when the serverMode is
		// explicitly set to dev.
		return instance.isProd();
	}
}
