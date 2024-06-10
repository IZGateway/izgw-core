package gov.cdc.izgateway.model;

import java.util.TreeMap;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a base class for maps returned by methods documented in OpenAPI documentation.
 * It simply suppresses the documentation of isEmpty from map.
 */
@SuppressWarnings("serial")
public class MappableEntity<T> extends TreeMap<String, T> {
	@Schema(hidden=true)
	@Override
	public boolean isEmpty() {
		return super.isEmpty();
	}
}
