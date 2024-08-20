package gov.cdc.izgateway.utils;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * DateUtil arose around frustration every time a date needed to be parsed with varying potential precision or formats.
 * It won't parse all known date formats, but it will generally get anything that would be expected, and has some useful
 * shortcuts.
 * 
 * NOW = the current date and time.
 * TODAY = Today at midnight.
 * TODAY 09:00 AM or 9:00 AM today mean today at 09:00 AM.
 * 
 * Other keywords work including YESTERDAY, TOMORROW, and the Day of the Week (which always corresponds to today or a day in the future).
 * 
 */
public class DateUtil {
    private static final String[] MONTHS = { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };
    private static final String[] DAYS = { "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY" };
    private static final String[] AMPM = { "AM", "PM" };
    private enum PartType {
    	Month, Day, Digits, Punct, AmPm, Unknown, Empty, Year, Hour, Minute, Second, Millis, TZ 
    };
    
    private static final String SIMPLE_FORMAT = "yyyyMMddhhmmss";
    
    public static Date safeParseDate(String date) {
		try {
			return parseDate(date);
		} catch (ParseException e) {
			return null;
		}
    }
    public static class ParseState {
    	int year = -1;
    	int month = -1;
    	int day = -1;
    	int hour = -1;
    	int minutes = -1;
    	int seconds = -1;
    	int nanos = -1;
    	int dow = -1;
    	int ampm = -1;
    	TimeZone tz = null;
		public void setFromCalendar(Calendar cal) {
			year = cal.get(Calendar.YEAR);
			month = cal.get(Calendar.MONTH);
			day = cal.get(Calendar.DAY_OF_MONTH);
			hour = cal.get(Calendar.HOUR_OF_DAY);
			minutes = cal.get(Calendar.MINUTE);
			seconds = cal.get(Calendar.SECOND);
			nanos = cal.get(Calendar.MILLISECOND) * 1000;
		}
		public Calendar getCalendar() {
	    	cleanupHours();
	    	Calendar cal = new GregorianCalendar();
	    	setDefaultDateValues(cal);
	    	
	    	if (tz != null) {
	    		cal.setTimeZone(tz);
	    	}

	    	cal.set(Calendar.YEAR, year);
	    	cal.set(Calendar.MONTH, month);
	    	cal.set(Calendar.DAY_OF_MONTH, day);
	    	cal.set(Calendar.HOUR_OF_DAY, hour >= 0 ? hour : 0);
			cal.set(Calendar.MINUTE, minutes >= 0 ? minutes : 0);
	    	cal.set(Calendar.SECOND, seconds >= 0 ? seconds : 0);
	    	cal.set(Calendar.MILLISECOND, nanos >= 0 ? nanos / 1000 : 0);

	    	if (dow != 0 && dow != cal.get(Calendar.DAY_OF_WEEK)) {
	    		// Day of Week doesn't match
	    	}
	    	return cal;
		}
		private void setDefaultDateValues(Calendar cal) {
			// Assume current date time for any missing date components
	    	if (year == -1) {
	    		year = cal.get(Calendar.YEAR);
	    		if (month == -1) {
	    			month = cal.get(Calendar.MONTH);
	    			if (day == -1) {
	    				day = cal.get(Calendar.DATE);
	    			}
	    		}
	    	}
		}
		private void cleanupHours() {
			if (hour > 0 && hour < 12 && ampm == 1) {
	    		hour += 12;
	    	} else if (hour == 12 && ampm == 0) {
	    		hour = 0;
	    	} else if (hour > 12 && ampm == 1) {
	    		// We have an AMPM Indicator, and an hour greater than 12
	    	}
		}
    }
	public static Date parseDate(String date) throws ParseException {
    	// Simple forms for computers. 
    	Date d = simpleParse(date);
    	if (d != null) {
    		return d;
    	}
    	
    	// Case doesn't matter
    	date = date.toUpperCase();
    	List<String> parts = new ArrayList<>();
    	List<PartType> partTypes = new ArrayList<>();
    	List<Integer> offsets = new ArrayList<>();
    	
    	parseDatePass1(date, parts, partTypes, offsets);
    	ParseState state = new ParseState();

    	// Second scan, determine content of alpha and digit strings
    	for (int i = 0; i < parts.size(); i++) {
        	int offset = offsets.get(i);
    		try {
	    		PartType type = partTypes.get(i);
	    		String s = parts.get(i).toUpperCase();
	    		if (type == PartType.Unknown) {
	    			parseUnknownDateParts(state, offset, s);
	    		} else if (type == PartType.Digits) {
	    			type = parseDateDigits(parts, partTypes, state, i, s);
					partTypes.set(i, type);
	    		}
    		} catch (Exception e) {
    			throw new ParseException(e.getMessage(), offset);
    		}
    	}
    	int[] counts = new int[PartType.values().length];
    	checkForDuplication(partTypes, counts);
    		
    	return state.getCalendar().getTime();
    }

	private static void parseUnknownDateParts(ParseState state, int offset, String s) throws ParseException {
		int pos;
		if ((pos = matchesAny(s, MONTHS, 3)) >= 0) {
			state.month = pos;
		} else if ((pos = matchesAny(s, AMPM, 1)) >= 0) {
			state.ampm = pos;
		} else if (isTimeZone(s)) {
			state.tz = TimeZone.getTimeZone(s);
		} else {
			Calendar cal = parseDateName(state, offset, s);
			if (cal != null) {
				state.setFromCalendar(cal);
			}
		}
	}

	private static void checkForDuplication(List<PartType> partTypes, int[] counts) {
		for (int i = 0; i < partTypes.size(); i++) {
    		PartType t = partTypes.get(i);
    		if (++counts[t.ordinal()] > 1) {
    			// TODO: Duplicated part type
    		}
    	}
	}

	private static PartType parseDateDigits(List<String> parts, List<PartType> partTypes, ParseState state, int i,
			String s) {
		PartType type;
		type = getDigitPartType(parts, partTypes, i);
		switch (type) {
		case Year:
			if (s.length() < 3) {
				state.year = 2000 + Integer.parseInt(s);
			} else {
				state.year = Integer.parseInt(s);
			}
			break;
		case Month:
			state.month = Integer.parseInt(s);
			break;
		case Day:
			state.day = Integer.parseInt(s);
			break;
		case Hour:
			state.hour = Integer.parseInt(s);
			break;
		case Minute:
			state.minutes = Integer.parseInt(s);
			break;
		case Second:
			state.seconds = Integer.parseInt(s);
			break;
		case Millis:
			state.nanos = Integer.parseInt((s + "000000").substring(0, 6));
			break;
		case TZ:
			parseTimeZone(parts, state, i, s);
			break;
		default:
			type = parseYear(state, s, type); 
		}
		return type;
	}

	private static PartType parseYear(ParseState state, String s, PartType type) {
		int value = Integer.parseInt(s);
		// If still unknown but greater than 60, likely a year
		if (value > 60) {
			type = PartType.Year;
			if (s.length() < 3) {
				state.year = 2000 + value;
			} else {
				state.year = value;
			}
		}
		return type;
	}

	private static void parseTimeZone(List<String> parts, ParseState state, int i, String s) {
		if (i + 1 < parts.size()) {
			if (":".equals(parts.get(i+1))) {
				String nextPart = i + 2 < parts.size() ? parts.get(i+2) : null;
				if (nextPart != null) {
					s += ":" + nextPart;
				}
				s = parts.get(i - 1) + s;
				state.tz = TimeZone.getTimeZone("GMT" + s);
			}
		} else if (s.length() > 2) {
			s = s.substring(0, 2) + ":" + s.substring(2);
			state.tz = TimeZone.getTimeZone("GMT" + s);
		}
	}

	private static Calendar parseDateName(ParseState state, int offset, String s) throws ParseException {
		int pos;
		Calendar cal = Calendar.getInstance();
		switch (s) {
		case "Z":
			state.tz = TimeZone.getTimeZone("GMT");
			cal = null;
			break;
		case "NOW":
			break;
		case "TODAY":
			// If the hour hasn't been set, set time to midnight, otherwise use existing time that was set
			if (state.hour < 0) setToMidnight(cal);  
			break;
		case "YESTERDAY":
			cal.add(Calendar.DAY_OF_MONTH, -1);
			if (state.hour < 0) setToMidnight(cal);
			break;
		case "TOMORROW":
			cal.add(Calendar.DAY_OF_MONTH, 1);
			if (state.hour < 0) setToMidnight(cal);
			break;
		default:
			// MONDAY, TUESDAY, et cetera, in this context means "this Monday", the nearest day (including today).
			if ((pos = matchesAny(s, DAYS, 3)) >= 0) {
				if (state.hour < 0) setToMidnight(cal);
				state.dow = pos + 1;
				int add = state.dow - (cal.get(Calendar.DAY_OF_WEEK) + 1);
				if (add < 0) {
					add += DAYS.length;
				}
				cal.add(Calendar.DAY_OF_MONTH, add);
				break;
			} 
			throw new ParseException("Unrecognized date part: " + s, offset);
		}
		return cal;
	}
	
	private static boolean isTimeZone(String s) {
		// Respond from most to least common cases.
		if (ZoneId.getAvailableZoneIds().contains(s)) {
			return true;
		}
		if ("Z".equals(s) || s.startsWith("+") || s.startsWith("-")) {
			return true;
		}
		return s.startsWith("UT") || s.startsWith("GMT") || s.startsWith("UTC");
	}
	private static void setToMidnight(Calendar cal) {
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

    public static String createTimestamp() {
        FastDateFormat fdf = FastDateFormat.getInstance("yyyyMMddHHmmssZ");

        return fdf.format(new Date());
    }

    public static class SimpleParseState {
		String front = null;
		String back = "";
		String zone = "";
    }
    private static Date simpleParse(String date) throws ParseException {
		String s = date.trim().replaceAll("[-:Tt]","");
		if (!s.matches("\\d+(\\.\\d+)?(Z|[+-][01]\\d([0-4]\\d)?)?")) {
			return null;
		}
		SimpleParseState state = new SimpleParseState();
		// It's almost all digits
		// Could be one of two forms YYYYMMDDHHMMss.SSS[+-]ZZZZ, or Unix Epoch
		if (StringUtils.containsAny(s, '+', '-', '.', 'Z')) {
			parseSimpleLongForm(s, state);
		} else {
			state.front = s;
			// Looks like an epoch date 
			long l = Long.parseLong(s);
			if (l > 30000000l && l < 399999999999l) {
				return new Date(l);
			}
		}
		if (state.front.length() > SIMPLE_FORMAT.length()) {
			// Should bail on this.
		}
		String format = SIMPLE_FORMAT.substring(0, Math.min(state.front.length(), SIMPLE_FORMAT.length()));
		if (state.back.length() > 0) {
			format += "." + StringUtils.repeat('S', state.back.length());
		}
		if (state.zone != null && state.zone.length() != 0) {
			format += "X";
		}
		FastDateFormat fdf = FastDateFormat.getInstance(format);
		return fdf.parse(s);
    }

	private static void parseSimpleLongForm(String s, SimpleParseState state) {
		// Guaranteed to be of the first form.
		state.front = StringUtils.substringBefore(s, ".");
		state.back = StringUtils.substringAfter(s, ".");
		state.zone = null;
		String h = state.back.length() == 0 ? state.front : state.back;
		
		for (int i = 0; i < h.length(); i++) {
			char c = h.charAt(i); 
			if (c == '-' || c == '+' || c == 'Z') {
				state.zone = h.substring(i);
				h = h.substring(0, i);
				break;
			}
		}
		if (state.back.length() == 0) {
			state.front = h;
		} else {
			state.back = h;
		}
	}	

	private static PartType getDigitPartType(List<String> parts, List<PartType> partTypes, int i) {
		String s;
		
		if (i == 0) {
			if (i + 1 < parts.size()) {
				s = parts.get(i + 1);
				return parseDelimited(s);
			}
			return PartType.Unknown;
		}
		
		s = parts.get(i-1);
		// preceded by a colon it could be minutes or seconds
		if ("+-".contains(s)) {
			return PartType.TZ;
		} else if (":".equals(s)) {
			return parseMinutes(parts, i);
		} else if ("/\\".contains(s)) {
			return parseSlashedDate(parts, i);
		} else if ("-".contains(s)) {
			return parseHyphenatedDate(parts, partTypes, i);
		} else if (i + 1 < parts.size()) {
			return parseByFollowingDelimiter(parts, i);
		} 
		return PartType.Unknown;
	}

	private static PartType parseByFollowingDelimiter(List<String> parts, int i) {
		String s;
		s = parts.get(i+1);
		if (":".equals(s)) {
			// follows by a : but not preceded by it, must be Hour
			return PartType.Hour;
		} else if ("/\\".contains(s)) {
			// Followed by a / but not preceded by it
			if (Locale.getDefault().getCountry().equalsIgnoreCase("US")) {
				return PartType.Month;
			}
			return PartType.Day;
		}
		return PartType.Unknown;
	}

	private static PartType parseHyphenatedDate(List<String> parts, List<PartType> partTypes, int i) {
		String s;
		// preceded by a dash, it could be in YYYY-MM-DD form, so month or day, 
		// or it could be DD-MMM-YYYY or DD-MMM-YY form, so YEAR
		// Or ich, DD-MM-YYYY form
		if (i + 1 < parts.size() && "-".equals(parts.get(i+1))) {
			// Preceded and followed by a dash, gonna be Month
			return PartType.Month;
		}
		// Only preceded by a dash.  YYYY[-MM], or YYYY-MM[-DD] or DD-MM[-YYYY] or ich, DD-MM[-YY]
		s = parts.get(i);
		if (s.length() > 2 && s.length() < 4) {
			return PartType.Year;
		}
		// Only preceded by a dash.  YYYY[-MM], or YYYY-MM[-DD] or ich, DD-MM[-YY]
		if (i - 2 > 0) {
			switch (partTypes.get(i-2)) {
			case Year:
				return PartType.Month;
			case Month:
				return PartType.Day;
			default:
				break;
			}
		}
		return PartType.Unknown;
	}

	private static PartType parseSlashedDate(List<String> parts, int i) {
		// preceded by a slash it could be year, day or month
		if (i + 1 < parts.size() && parts.get(i+1).equals("/")) {
			// mm/dd/yyyy or dd/mm/yyyy
			if (Locale.getDefault().getCountry().equalsIgnoreCase("US")) {
				return PartType.Day;
			}
			return PartType.Month;
		}
		return PartType.Unknown;
	}

	private static PartType parseMinutes(List<String> parts, int i) {
		// if followed by a colon, it is definitely minutes.
		if (i + 1 < parts.size() && parts.get(i+1).equals(":")) {
			return PartType.Minute;
		}
		// If there are no other colons preceding, then it's likely hh:mm form.
		for (int j = i - 2; j > 0; j--) {
			if (":".equals(parts.get(j))) {
				return PartType.Second;	// There were two such animals, it's seconds
			}
		}
		return PartType.Minute;	// Assume hh:mm form
	}

	private static PartType parseDelimited(String s) {
		switch (s) {
		case ":":
			return PartType.Hour;
		case "/", "\\":
			return Locale.getDefault().getCountry().equals("US") ? PartType.Month : PartType.Day;
		case "-":
			if (s.length() >= 3 && s.length() <= 4) {
				return PartType.Year;
			}
			return PartType.Unknown;
		default:
			return PartType.Unknown;
		}
	}

	private static int matchesAny(String s, String[] list, int minLength) {
		for (int pos = 0; pos < list.length; pos++) {
			String m = list[pos];
			if (m.startsWith(s) && s.length() >= minLength) {
				return pos;
			}
		}
		return -1;
	}

	private static void parseDatePass1(String date, List<String> parts, List<PartType> partTypes, List<Integer> offsets) {
    	PartType type = PartType.Empty;
    	StringBuilder b = new StringBuilder();
    	int offset = 0;
		// First scan, break into strings of digits, alpha, and punctuation
    	for (int i = 0; i < date.length(); i++) {
    		char c = date.charAt(i);
    		
    		if (Character.isDigit(c)) {
    			switch (type) {
    			case Unknown:
    				parts.add(b.toString());
    				offsets.add(offset);
    				offset = i;
    				b.setLength(0);
    				// Fall through
    			case Empty:
    				type = PartType.Digits;
    				partTypes.add(type);
    				// Fall through
    			case Digits:
    				b.append(c);
    				break;
    			default:
    				break;
    			}
    		} else if (Character.isAlphabetic(c)) {
    			switch (type) {
    			case Digits:
    				parts.add(b.toString());
    				offsets.add(offset);
    				offset = i;
    				b.setLength(0);
    				// Fall through
    			case Empty:
    				type = PartType.Unknown;
    				partTypes.add(type);
    				// Fall through
    			case Unknown:
    				b.append(c);
    				break;
    			default:
    				break;
    			}
    			
    		} else {
    			if (type != PartType.Empty) {
    				parts.add(b.toString());
    				offsets.add(offset);
    				offset = i;
    				b.setLength(0);
    			}
				partTypes.add(PartType.Punct);
				parts.add(b.append(c).toString());
				offsets.add(offset);
				offset = i;
				b.setLength(0);
				type = PartType.Empty;
    		}
    	}
    	if (b.length() != 0) {
    		offsets.add(offset);
    		parts.add(b.toString());
    	}
	}
}
