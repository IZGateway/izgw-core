package gov.cdc.izgateway.utils;

import javax.security.auth.x500.X500Principal;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;



/**
 * A collection of static methods to operate on X500 names (commonNames in certificates).  This class
 * is used to extract just the Common Name or the Organization from a X509 Certificate.
 */
public class X500Utils {
	public static final String ORGANIZATION = "O";
	public static final String ORGANIZATION_UNIT = "OU";
	public static final String COMMON_NAME = "CN";
	public static final String COUNTRY = "C";
	public static final String STATE = "S"; 
    private X500Utils() {}

    public static Map<String, String> getParts(String name) {
    	if (name == null) {
    		return Collections.emptyMap();
    	}

    	boolean onKey = true;
    	StringBuilder key = new StringBuilder();
    	StringBuilder value = new StringBuilder(); 
    	StringReader r = new StringReader(name);
        Map<String, String> result = new TreeMap<>();
    	int c;
    	try {
			while ((c = r.read()) >= 0) {
				if (onKey) { 
					onKey = readKey(onKey, key, value, c);
				} else {
					onKey = readValue(value, r, c);
					if (onKey) {
						saveKeyAndValue(key, value, result);
					}
				}
			}
			saveKeyAndValue(key, value, result);
		} catch (IOException e) {
			// StringReader doesn't throw here.
		}
    	
        return result;
    }

	private static boolean readValue(StringBuilder value, StringReader r, int c) throws IOException {
		switch (c) {
		case '\\':
			// Escape following special character
			c = r.read();
			if (c >= 0) {
				value.append((char)c);
			}
			return false;
		case ',':
			return true;
		default:
			// NOT end of stream or end of value
			value.append((char)c);
			return false;
		}
	}

	private static boolean readKey(boolean onKey, StringBuilder key, StringBuilder value, int c) {
		if (c == '=') {
			onKey = false;
		} else if (c == ',') {
			// Premature end of key=value, skip this key
			key.setLength(0);
			value.setLength(0);
		} else {
			key.append((char)c);
		}
		return onKey;
	}

	private static void saveKeyAndValue(StringBuilder key, StringBuilder value, Map<String, String> result) {
		// End of stream or end of value
		if (key.length() != 0 && value.length() != 0) {
			// If there is a value, set it
			result.put(key.toString(), value.toString());
		}
		key.setLength(0);
		value.setLength(0);
	}

    public static Map<String, String> getParts(X500Principal subject) {
    	if (subject == null) {
    		return Collections.emptyMap();
    	}
        return getParts(subject.getName());
    }
    
    public static String getPart(X500Principal subject, String part) {
    	if (subject == null) {
    		return null;
    	}
        return getParts(subject).get(part.toUpperCase());
    }

	public static String getCommonName(X509Certificate cert) {
		if (cert == null) {
			return null;
		}
		return getPart(cert.getSubjectX500Principal(), COMMON_NAME);
	}
	public static String getCommonName(X500Principal principal) {
		return getPart(principal, COMMON_NAME);
	}
	public static String getCommonName(String principal) {
		return getParts(principal).get(COMMON_NAME);
	}
    public static BigInteger[] buildSerialNumbers(X509Certificate[] certs) {
        return mapToArray(certs, X509Certificate::getSerialNumber, BigInteger[]::new);
    }

    public static String[] buildIssuerDnNames(X509Certificate[] certs) {
        return buildDnNames(certs, X509Certificate::getIssuerX500Principal);
    }

    public static X500Principal[] buildIssuerDns(X509Certificate[] certs) {
        return buildDns(certs, X509Certificate::getIssuerX500Principal);
    }

    public static String[] buildSubjectDnNames(X509Certificate[] certs) {
        return buildDnNames(certs, X509Certificate::getSubjectX500Principal);
    }

    public static X500Principal[] buildSubjectDns(X509Certificate[] certs) {
        return buildDns(certs, X509Certificate::getSubjectX500Principal);
    }

    public static String[] buildDnNames(X509Certificate[] certs, Function<X509Certificate, X500Principal> certDnMapper) {
        return mapToArray(certs, certDnMapper.andThen(X500Principal::getName), String[]::new);
    }

    public static X500Principal[] buildDns(X509Certificate[] certs, Function<X509Certificate, X500Principal> certDnMapper) {
        return mapToArray(certs, certDnMapper, X500Principal[]::new);
    }
    
    public static <T, U> U[] mapToArray(T[] inArr, Function<T, U> mapper, IntFunction<U[]> outArrGen) {
        return Stream.of(inArr).map(mapper).toArray(outArrGen);
    }
}
