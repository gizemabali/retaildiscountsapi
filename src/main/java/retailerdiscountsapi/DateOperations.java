package retailerdiscountsapi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateOperations {

	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String GMT_3 = "GMT+3";

	/**
	 * Singleton utility class instance.
	 */
	private static final DateOperations instance = new DateOperations();

	/**
	 * Default constructor made private to prevent outside access. Use singleton method getter {@link #getInstance()}
	 * instead.
	 *
	 * @see #getInstance()
	 */
	private DateOperations() {
	}

	/**
	 * Getter for the singleton instance.
	 */
	public static DateOperations getInstance() {
		return instance;
	}

	/**
	 * This method returns current date in the DATE_FORMAT string.
	 * 
	 * @return
	 * @param format
	 */
	public String getCurrentDate() {
		Calendar calendar = getCalendar();
		Date date = calendar.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		dateFormat.setTimeZone(TimeZone.getTimeZone(GMT_3));
		return dateFormat.format(date);
	}

	public Calendar getCalendar() {
		return Calendar.getInstance(TimeZone.getTimeZone(GMT_3));
	}
}
