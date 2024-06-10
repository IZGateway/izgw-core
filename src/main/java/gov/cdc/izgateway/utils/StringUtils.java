package gov.cdc.izgateway.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.ConfigurableApplicationContext;

public final class StringUtils {
    public static final String UNDERSCORE = "_";

    private StringUtils() {
    }

    public static String joinCamelCase(String ... strParts) {
        for (int a = 0; a < strParts.length; a++) {
            strParts[a] = strParts[a].toLowerCase();

            if (a > 0) {
                strParts[a] = org.apache.commons.lang3.StringUtils.capitalize(strParts[a]);
            }
        }

        return org.apache.commons.lang3.StringUtils.join(strParts, org.apache.commons.lang3.StringUtils.EMPTY);
    }

    public static String[] tokenize(String str) {
        return tokenize(str, null);
    }

    public static String[] tokenize(String str, String defaultStr) {
        return ObjectUtils.defaultIfNull(org.springframework.util.StringUtils.tokenizeToStringArray(ObjectUtils.defaultIfNull(str, defaultStr),
            ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS), ArrayUtils.EMPTY_STRING_ARRAY);
    }
}
