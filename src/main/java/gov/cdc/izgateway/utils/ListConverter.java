package gov.cdc.izgateway.utils;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Assuming a function "convert", that maps from "From" to "To", this class will provide a list interface to 
 * allow one to iterate over "To" items without creating a bunch of new objects.  This is especially helpful
 * to translate between interface classes.  It is used in Logging to convert from the LogBack IListEvent class
 * to the LogEvent class in IZ Gateway mostly to serve as a way to document logging events in Swagger. 
 * @param <From> The class to convert from
 * @param <To> The class to convert to
 */
public final class ListConverter<From, To> extends AbstractList<To> {
	private final List<From> events;
	private final Function<From, To> convert;

	public ListConverter(List<From> events, Function<From, To> convert) {
		this.events = events;
		this.convert = convert;
	}

	@Override
	public int size() {
		return events.size();
	}

	@Override
	public boolean addAll(Collection<? extends To> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public To get(int index) {
		return convert.apply(events.get(index));
	}
}