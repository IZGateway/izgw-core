package gov.cdc.izgateway.utils;

public final class IZGatewayProperties {
    public static final String PREFIX = "phiz.";
    public static final String APP_PREFIX = PREFIX + "app.";
    public static final String FILE_PREFIX = "file.";
    public static final String CONSOLE_PREFIX = "console.";
    public static final String LOGGING_PREFIX = PREFIX + "logging.";
    public static final String LOGGING_CONSOLE_PREFIX = LOGGING_PREFIX + CONSOLE_PREFIX;
    public static final String LOGGING_FILE_PREFIX = LOGGING_PREFIX + FILE_PREFIX;
	public static final String LOGGING_ADDITIONAL_IIS_PREFIX = PREFIX + "additional.iis.";
    public static final String LOGGING_ADDITIONAL_IIS_FILE_PREFIX = LOGGING_ADDITIONAL_IIS_PREFIX + FILE_PREFIX;
    public static final String LOGGING_LOGSTASH_PREFIX = LOGGING_PREFIX + "logstash.";
    public static final String LOGGING_LOGSTASH_CONSOLE_PREFIX = LOGGING_LOGSTASH_PREFIX + CONSOLE_PREFIX;
    public static final String LOGGING_LOGSTASH_FILE_PREFIX = LOGGING_LOGSTASH_PREFIX + FILE_PREFIX;
    public static final String WRAPPER_PREFIX = "wrapper.";

    public static final String DIR_SUFFIX = "dir";
    public static final String ENABLED_SUFFIX = "enabled";
    public static final String NAME_SUFFIX = "name";

    public static final String APP_NAME_NAME = APP_PREFIX + NAME_SUFFIX;
    public static final String APP_PID_NAME = APP_PREFIX + "pid";

    public static final String LOGGING_CONSOLE_ENABLED_NAME = LOGGING_CONSOLE_PREFIX + ENABLED_SUFFIX;
    public static final String LOGGING_CONSOLE_TTY_NAME = LOGGING_CONSOLE_PREFIX + "tty";

    public static final String LOGGING_FILE_DIR_NAME = LOGGING_FILE_PREFIX + DIR_SUFFIX;
    public static final String LOGGING_FILE_ENABLED_NAME = LOGGING_FILE_PREFIX + ENABLED_SUFFIX;
    public static final String LOGGING_FILE_NAME_NAME = LOGGING_FILE_PREFIX + NAME_SUFFIX;
    public static final String LOGGING_FILE_ADDITIONAL_IIS_NAME = LOGGING_ADDITIONAL_IIS_FILE_PREFIX + NAME_SUFFIX;

    public static final String LOGGING_LOGSTASH_CONSOLE_ENABLED_NAME = LOGGING_LOGSTASH_CONSOLE_PREFIX + ENABLED_SUFFIX;
    public static final String LOGGING_LOGSTASH_CONSOLE_PRETTY_NAME = LOGGING_LOGSTASH_CONSOLE_PREFIX + "pretty";
    public static final String LOGGING_LOGSTASH_TESTING_ENABLED_NAME = LOGGING_LOGSTASH_PREFIX + "testing." + ENABLED_SUFFIX;
    public static final String LOGGING_LOGSTASH_FILE_DIR_NAME = LOGGING_LOGSTASH_FILE_PREFIX + DIR_SUFFIX;
    public static final String LOGGING_LOGSTASH_FILE_ENABLED_NAME = LOGGING_LOGSTASH_FILE_PREFIX + ENABLED_SUFFIX;
    public static final String LOGGING_LOGSTASH_FILE_NAME_NAME = LOGGING_LOGSTASH_FILE_PREFIX + NAME_SUFFIX;

    public static final String MODE_NAME = PREFIX + "mode";
    public static final String DEV_MODE_VALUE = "dev";
    public static final String PROD_MODE_VALUE = "prod";

    public static final String WRAPPER_DAEMON_NAME = WRAPPER_PREFIX + "daemon";

    private IZGatewayProperties() {
    }
}
