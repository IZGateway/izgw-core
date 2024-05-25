package gov.cdc.izgateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Quick and dirty converter of POJOs to JSON Strings.
 */
public class JsonUtils {
	public static String toString(Object o) {
	    // Create a json object from the data elements, and convert that to a string.
	    ObjectMapper om = new ObjectMapper();
	    try {
	        return om.writeValueAsString(o);
	    } catch (JsonProcessingException e) {
	        return "Error converting " + o.getClass().getSimpleName() + " data to JSON: " + e.getMessage();
	    }
	}
}
