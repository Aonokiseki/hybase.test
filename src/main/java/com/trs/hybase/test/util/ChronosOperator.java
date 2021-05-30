package com.trs.hybase.test.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class ChronosOperator {
	/*防止实例化*/
	private ChronosOperator(){}
	private final static String DEFAULT_LOCALDATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";
	private final static String DEFAULT_LOCALDATE_FORMAT = "yyyy/MM/dd";
	private final static String DEFAULT_LOCALTIME_FORMAT = "HH:mm:ss";
	/**
	 * 返回指定范围内一个随机的LocalDateTme对象
	 * @param time1 LocalDateTime的实例化对象
	 * @param time2 LocalDateTime的实例化对象
	 * @return <code>LocalDateTime<code>的实例化对象,时间在time1和time2之间
	 */
	public static LocalDateTime getARandomLocalDateTime(LocalDateTime dateTime1, LocalDateTime dateTime2){
		LocalDateTime lower = dateTime1;  
		LocalDateTime higher = dateTime2;
		if(dateTime1.isAfter(dateTime2)){ lower = dateTime2; higher = dateTime1; }
		Random random = new Random();
		long timeFix = (long)(random.nextDouble() * Duration.between(lower, higher).toNanos());
		LocalDateTime result = lower.plusNanos(timeFix);
		return result;
	}
	/**
	 * 返回指定范围内的一个随机的LocalTime对象
	 * @param time1
	 * @param time2
	 * @return <code>LocalTime<code>的实例化对象,时间在time1和time2之间
	 */
	public static LocalTime getARandomLocalTime(LocalTime time1, LocalTime time2){
		LocalTime lower = time1;
		LocalTime higher = time2;
		if(time1.isAfter(time2)){ 
			lower = time2; higher = time1;
		}
		Random random = new Random();
		long timeFix = (long)(random.nextDouble() * Duration.between(lower, higher).toNanos());
		LocalTime result = lower.plusNanos(timeFix);
		return result;
	}
	/**
	 * 将LocalDateTime描述的时间转换为字符串
	 * @param localDateTime
	 * @param pattern 描述时间的格式, 空或空串采用默认值 yyyy/MM/dd HH:mm:ss.SSS
	 * @return 
	 */
	public static String LocalDateTimeToString(LocalDateTime localDateTime, String pattern){
		if(pattern == null || pattern.isEmpty())
			return localDateTime.format(DateTimeFormatter.ofPattern(DEFAULT_LOCALDATETIME_FORMAT));
		return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
	}
	/**
	 * 将LocalDate描述的时间转换为字符串
	 * @param localDate
	 * @param pattern 描述时间的格式, 空或空串采用默认值 yyyy/MM/dd
	 * @return
	 */
	public static String LocalDateToString(LocalDate localDate, String pattern){
		if(pattern == null || pattern.isEmpty())
			return localDate.format(DateTimeFormatter.ofPattern(DEFAULT_LOCALDATE_FORMAT));
		return localDate.format(DateTimeFormatter.ofPattern(pattern));
	}
	/**
	 * 将LocalTime描述的时间转换为字符串
	 * @param localTime
	 * @param pattern 描述时间的格式,空或空串采用默认值 HH:mm:ss
	 * @return
	 */
	public static String LocalTimeToString(LocalTime localTime, String pattern){
		if(pattern == null || pattern.isEmpty())
			return localTime.format(DateTimeFormatter.ofPattern(DEFAULT_LOCALTIME_FORMAT));
		return localTime.format(DateTimeFormatter.ofPattern(pattern));
	}
	/**
	 * 将String描述的日期转换为LocalDateTime对象
	 * @param time
	 * @param pattern 描述时间的格式, 空或空串采用默认值 yyyy/MM/dd HH:mm:ss.SSS
	 * @return
	 */
	public static LocalDateTime stringToLocalDateTime(String time, String pattern){
		DateTimeFormatter dateTimeFormatter = null;
		if(pattern == null || pattern.isEmpty())
			dateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_LOCALDATETIME_FORMAT);
		else
			dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
		return LocalDateTime.parse(time, dateTimeFormatter);
	}
	/**
	 * 将String描述的日期转换为LocalDate对象
	 * @param time
	 * @param pattern 描述时间的格式, 空或空串采用默认值 yyyy/MM/dd
	 * @return
	 */
	public static LocalDate stringToLocalDate(String time, String pattern){
		DateTimeFormatter dateTimeFormatter = null;
		if(pattern == null || pattern.isEmpty())
			dateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_LOCALDATE_FORMAT);
		else
			dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
		return LocalDate.parse(time, dateTimeFormatter); 
	}
	/**
	 * 将String描述的时间转换为LocalTime对象
	 * @param time
	 * @param pattern 描述的时间的格式, 空或空串采用默认值 HH:mm:ss
	 * @return
	 */
	public static LocalTime stringToLocalTime(String time, String pattern){
		DateTimeFormatter dateTimeFormatter = null;
		if(pattern == null || pattern.isEmpty())
			dateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_LOCALTIME_FORMAT);
		else
			dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
		return LocalTime.parse(time, dateTimeFormatter);
	}
	/**
	 * <code>Date</code>转换为<code>LocalDateTime</code>
	 * @param date
	 * @return
	 */
	public static LocalDateTime dateToLocalDateTime(Date date){
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	public static LocalDate dateToLocalDate(Date date){
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
	public static LocalTime dateToLocalTime(Date date){
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
	}
	/**
	 * <code>Calendar</code> 转换为<code>LocalDateTime</code>
	 * @param calendar
	 * @return
	 */
	public static LocalDateTime calendarToLocalDateTime(Calendar calendar){
		return dateToLocalDateTime(calendar.getTime());
	}
	public static LocalDate calendarToLocalDate(Calendar calendar){
		return dateToLocalDate(calendar.getTime());
	}
	public static LocalTime calendarToLocalTime(Calendar calendar){
		return dateToLocalTime(calendar.getTime());
	}
	/**
	 * 获取当前时间戳
	 * @return
	 */
	public static long currentTimeMillis(){
		return Instant.now().toEpochMilli();
	}
	/**
	 * 时间戳转换为<code>LocalDateTime</code>, 精度为秒
	 * @param timeMillis
	 * @return
	 */
	public static LocalDateTime timeMillisToLocalDateTime(long timeMillis){
		return LocalDateTime.ofEpochSecond(timeMillis/1000, 0, ZoneOffset.ofHours(8));
	}
	
	/**
	 * 计算两个<code>LocalDateTime</code>对象之间的时间差
	 * @param ldt1
	 * @param ldt2
	 * @return <code>Duration</code>对象
	 */
	public static Duration timeDifference(LocalDateTime ldt1, LocalDateTime ldt2){
		return Duration.between(ldt1, ldt2);
	}
	/**
	 * 计算两个<code>LocalDateTime</code>对象之间的时间差
	 * @param ldt1
	 * @param ldt2
	 * @return 时间差, 单位毫秒
	 */
	public static long timeDifferenceMills(LocalDateTime ldt1, LocalDateTime ldt2){
		return timeDifference(ldt1, ldt2).toMillis();
	}
}
