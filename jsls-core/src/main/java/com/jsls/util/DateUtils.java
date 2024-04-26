package com.jsls.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 日期工具类
 * 
 * @author YM10177
 *
 */
public class DateUtils {

	public static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

	public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
	public static final String DATE_FORMAT_YYYYMD = "yyyy/M/d";
	public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
	public static final String DATE_FORMAT_DATE = DATE_FORMAT_YYYY_MM_DD;
	public static final String DATE_FORMAT_DATETIME = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss,SSS";
	public static final String DATE_FORMAT_TIMESTAMP2 = "yyyyMMddHHmmssSSS";

	public static String normalDate(String str) {
		if (!org.springframework.util.StringUtils.hasText(str)) {
			return str;
		}
		String ustr = str.replaceAll("/", "").replaceAll("T", "").replaceAll(":", "").replaceAll(" ", "");
		String day = dosSplit(ustr.substring(0, 8), "-", 4, 6, 8);
		if (ustr.length() > 8) {
			String time = dosSplit(ustr.substring(8), ":", 2, 4, 6);
			return day + " " + time;
		}
		return day;
	}

	public static String normalTime(String str) {
		if (!org.springframework.util.StringUtils.hasText(str)) {
			return str;
		}
		return dosSplit(str.replaceAll(":", ""), ":", 2, 4, 6);
	}

	private static String dosSplit(String str, String spt, int... dos) {
		if (!org.springframework.util.StringUtils.hasText(str)) {
			return str;
		}
		int li = 0;
		String ustr = "";
		for (int ddi : dos) {
			if (str.length() < ddi) {
				break;
			}
			String item = str.substring(li, ddi);
			if (li > 0) {
				ustr += spt + item;
			} else {
				ustr = item;
			}
			li = ddi;
		}
		return ustr;
	}

	public static int getAge(Date birthDay) {
		Calendar cal = Calendar.getInstance();
		if (cal.before(birthDay)) {
			throw new IllegalArgumentException("The birthDay is before Now.It's unbelievable!");
		}
		int yearNow = cal.get(Calendar.YEAR);
		int monthNow = cal.get(Calendar.MONTH);
		int dayOfMonthNow = cal.get(Calendar.DAY_OF_MONTH);
		cal.setTime(birthDay);
		int yearBirth = cal.get(Calendar.YEAR);
		int monthBirth = cal.get(Calendar.MONTH);
		int dayOfMonthBirth = cal.get(Calendar.DAY_OF_MONTH);
		int age = yearNow - yearBirth;
		if (monthNow <= monthBirth) {
			if (monthNow < monthBirth || dayOfMonthNow < dayOfMonthBirth) {
				age--;
			}
		}
		return age;
	}

	public static Date smartParseDate(String source) {
		if (StringUtils.hasText(source)) {
			String useSource = source.replace("T", " ");
			String timeZone = null;
			if (source.endsWith("Z")) {
				timeZone = " UTC";
				useSource = useSource.substring(0, useSource.length() - 1);
			}
			String datePart = useSource;
			String dateFmt = DATE_FORMAT_DATE;
			String timeFmt = null;
			int idx = useSource.indexOf(" ");
			if (idx > 0) {
				timeFmt = "HH:mm:ss";
				datePart = useSource.substring(0, idx);
				String timePart = useSource.substring(idx + 1);
				if (timePart.length() > timeFmt.length()) {
					if (timePart.charAt(timePart.length() - 4) == '.') {
						timeFmt = "HH:mm:ss.SSS";
					} else {
						timeFmt = "HH:mm:ss,SSS";
					}
				}
			}
			if (datePart.contains("/")) {
				dateFmt = DATE_FORMAT_YYYYMD;
			} else if (!datePart.contains("-")) {
				dateFmt = DATE_FORMAT_YYYYMMDD;
			}
			String format = dateFmt;
			if (timeFmt != null) {
				format += " " + timeFmt;
			}
			if (timeZone != null) {
				useSource += timeZone;
				format += " Z";
			}
			return parseDate(useSource, format);
		}
		return null;
	}

	public static int calcDistDays(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			return 0;
		} else {
			return (int) ((date1.getTime() - date2.getTime()) / 3600000 / 24);
		}
	}

	/**
	 * date2比date1多的天数
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static int differentDays(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		int day1 = cal1.get(Calendar.DAY_OF_YEAR);
		int day2 = cal2.get(Calendar.DAY_OF_YEAR);
		int year1 = cal1.get(Calendar.YEAR);
		int year2 = cal2.get(Calendar.YEAR);
		if (year1 != year2) {// 不同年
			int timeDistance = 0;
			for (int i = year1; i < year2; i++) {
				if (i % 4 == 0 && i % 100 != 0 || i % 400 == 0) {// 闰年
					timeDistance += 366;
				} else { // 不是闰年
					timeDistance += 365;
				}
			}
			return timeDistance + (day2 - day1);
		} else {// 同一年
			return day2 - day1;
		}
	}

	public static Date asDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date asDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static LocalDate asLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static LocalDateTime asLocalDateTime(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public static String formatDate(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static Date parseDate(String strDate, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try {
			return sdf.parse(strDate);
		} catch (ParseException e) {
			logger.error("字符串转日期失败：" + e.getMessage(), e);
			throw new RuntimeException("字符串转日期失败：" + e.getMessage(), e);
		}
	}

	/**
	 * 这一天结束
	 * 
	 * @param date
	 * @return
	 */
	public static Date dayEnd(Date date, boolean include) {
		Date dateEnd = date;
		if (dateEnd != null) {
			if (include) {
				dateEnd = setMilliseconds(dateEnd, 999);
				dateEnd = DateUtils.setMinutes(dateEnd, 59);
				dateEnd = DateUtils.setSeconds(dateEnd, 59);
				return DateUtils.setHours(dateEnd, 23);
			}
			return addDay(dayStart(dateEnd), 1);
		}
		return dateEnd;
	}

	/**
	 * 这一天开始
	 * 
	 * @param date
	 * @return
	 */
	public static Date dayStart(Date date) {
		Date dateStart = date;
		if (dateStart != null) {
			dateStart = setMilliseconds(dateStart, 0);
			dateStart = DateUtils.setMinutes(dateStart, 0);
			dateStart = DateUtils.setSeconds(dateStart, 0);
			return DateUtils.setHours(dateStart, 0);
		}
		return dateStart;
	}

	/**
	 * 这个月结束
	 * 
	 * @param date
	 * @return
	 */
	public static Date monthEnd(Date date, boolean include) {
		Date mthEnd = dayStart(date);
		if (mthEnd != null) {
			Calendar c = Calendar.getInstance();
			c.setLenient(false);
			c.setTime(mthEnd);
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.add(Calendar.MONTH, 1);
			Date temp = c.getTime();
			if (include) {
				return new Date(temp.getTime() - 1);
			}
			return temp;
		}
		return mthEnd;
	}

	/**
	 * 这个月开始
	 * 
	 * @param date
	 * @return
	 */
	public static Date monthStart(Date date) {
		Date mthStart = dayStart(date);
		if (mthStart != null) {
			mthStart = set(mthStart, Calendar.DAY_OF_MONTH, 1);
		}
		return mthStart;
	}

	/**
	 * 这年开始
	 * 
	 * @param date
	 * @return
	 */
	public static Date yearStart(Date date) {
		if (date != null) {
			String year = formatDate(date, "yyyy");
			return yearStart(Integer.valueOf(year));
		}
		return date;
	}

	/**
	 * 这年结束
	 * 
	 * @param date
	 * @return
	 */
	public static Date yearEnd(Date date, boolean include) {
		if (date != null) {
			String year = formatDate(date, "yyyy");
			return yearEnd(Integer.valueOf(year), include);
		}
		return date;
	}

	/**
	 * 这年开始
	 * 
	 * @param date
	 * @return
	 */
	public static Date yearStart(int year) {
		return parseDate(year + "-01-01", DATE_FORMAT_DATE);
	}

	/**
	 * 这年结束
	 * 
	 * @param date
	 * @return
	 */
	public static Date yearEnd(int year, boolean include) {
		Date temp = parseDate((year + 1) + "-01-01", DATE_FORMAT_DATE);
		if (include) {
			return new Date(temp.getTime() - 1);
		}
		return temp;
	}

	public static Date weekDay(Date date, int firstDayOfWeek) {
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		c.setTime(date);
		int day_of_week = c.get(Calendar.DAY_OF_WEEK) - 1;
		if (day_of_week == 0) {
			day_of_week = 7;
		}
		c.add(Calendar.DATE, -day_of_week + firstDayOfWeek);
		return c.getTime();
	}

	public static Date addYear(Date date, int year) {
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		c.setTime(date);
		c.add(Calendar.YEAR, year);
		return c.getTime();
	}

	public static Date addMonth(Date date, int month) {
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		c.setTime(date);
		c.add(Calendar.MONTH, month);
		return c.getTime();
	}

	public static String addDay(String date, int day, String pattern) {
		return addDay(date, day, pattern, pattern);
	}

	public static String addDay(String date, int day, String pattern, String pattern2) {
		return addDay(parseDate(date, pattern), day, pattern2);
	}

	public static String addDay(Date date, int day, String pattern) {
		Date ndate = addDay(date, day);
		return formatDate(ndate, pattern);
	}

	public static Date addDay(Date date, int day) {
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		c.setTime(date);
		c.add(Calendar.DAY_OF_MONTH, day);
		return c.getTime();
	}

	public static Date addHour(Date date, int hour) {
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		c.setTime(date);
		c.add(Calendar.HOUR_OF_DAY, hour);
		return c.getTime();
	}

	public static Date setMilliseconds(Date date, int amount) {
		return set(date, Calendar.MILLISECOND, amount);
	}

	public static Date setMinutes(Date date, int amount) {
		return set(date, Calendar.MINUTE, amount);
	}

	public static Date setSeconds(Date date, int amount) {
		return set(date, Calendar.SECOND, amount);
	}

	public static Date setHours(Date date, int amount) {
		return set(date, Calendar.HOUR_OF_DAY, amount);
	}

	private static Date set(Date date, int calendarField, int amount) {
		if (date == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		Calendar c = Calendar.getInstance();
		c.setLenient(false);
		c.setTime(date);
		c.set(calendarField, amount);
		return c.getTime();
	}
}
