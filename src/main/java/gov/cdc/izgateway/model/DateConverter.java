package gov.cdc.izgateway.model;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * @author Audacious Inquiry
 * A converter for the java.util.Date time which converts dates to and from
 * strings in the a ISO 8601 timestamp format using the UTC timezone.
 */
@Slf4j
public class DateConverter implements AttributeConverter<Date> { // NOSONAR, singleton OK here.
	// This is the correct ISO 8601 format with timezone offset that supports Z at the end
	// as is used by JavaScript Date.toISOString()
	private static FastDateFormat ft = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
	
	private static final DateConverter INSTANCE = new DateConverter();
	
	@Override
	public AttributeValue transformFrom(Date input) {
		return AttributeValue.fromS(convert(input));
	}

	@Override
	public Date transformTo(AttributeValue input) {
		String s = input.s();
		try {
			return ft.parse(s);
		} catch (ParseException e) {
			// Log this so we find it, but return null so that things mostly work as expected
			log.error("Error parsing date string {}", input.s(), e);
			return null;
		}
	}

	@Override
	public EnhancedType<Date> type() {
		return EnhancedType.of(Date.class);
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}
	
	/**
	 * @param <T> The enhanced type
	 * @param enhancedType	A type to attempt conversion on
	 * @return	A converter to use for that class.
	 */
	public static <T> AttributeConverter<T> provider(EnhancedType<T> enhancedType) {
		if (!Date.class.isAssignableFrom(enhancedType.rawClass())) {
			return null;
		}
		@SuppressWarnings("unchecked")
		AttributeConverter<T> result = (AttributeConverter<T>) INSTANCE;
		return result;
	}

	/**
	 * Convert a date to an ISO 8601 String
	 * @param date The date to convert
	 * @return	The string in ISO 8601 format YYYY-MM-DDThh:mm:ss.SSSZ
	 */
	public static String convert(Date date) {
		return ft.format(date);
	}
}
