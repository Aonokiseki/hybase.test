package com.trs.hybase.test.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class DateOperator {
	
	public static final long THE_NUMBER_OF_MILLISECONDS_IN_ONE_DAY = 86400000;
	public static final long THE_NUMBER_OF_MILLISECONDS_IN_ONE_HOUR = 3600000;
	/*
	 * 防止实例化
	 */
	private DateOperator(){}
	
	/**
	 * 以[standard]为基准日期,获取该日期[dateFix]之后(或前)的日期
	 * 
	 * @param standard 日期基准值, 空或空串视为今天
	 * @param timeZone 时区, 空或空串视为 Asia/ShangHai
	 * @param dateFix 日期修正值
	 * @param timeFormat 日期解析格式
	 * @return String 目标日期
	 * @throws ParseException
	 */
	public static String getDate(String standard, String timeZone, int dateFix, TimeFormat timeFormat) throws ParseException{
		String result = null;
		if(timeZone == null || "".equals(timeZone)){
			timeZone = "Asia/ShangHai";
		}
		Calendar calendar = Calendar.getInstance();
		if(standard != null && !"".equals(standard.trim())){
			calendar = stringToCalendar(standard, timeFormat);
		}
		TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
		calendar.add(Calendar.DATE, dateFix);
		result = calendarToString(calendar, timeFormat);
		return result;
	}
	/**
	 * 获取两天之间的间隔天数
	 * @param date1 日期1, 必须和timeFormat描述的格式一致
	 * @param date2 日期2, 和日期1的先后顺序随意,必须和timeFormat描述的格式一致
	 * @param timeFormat 日期解析格式
	 * @return long 间隔天数
	 * @throws ParseException 两个日期和日期的范式不匹配时或字符串描述的日期错误时
	 */
	public static long spaceBetweenTwoDays(String date1, String date2, TimeFormat timeFormat) throws ParseException{
		Calendar calendarStart = stringToCalendar(date1, timeFormat);
		Calendar calendarEnd = stringToCalendar(date2, timeFormat);
		long result = Math.abs(calendarEnd.getTimeInMillis() - calendarStart.getTimeInMillis());
		return result/THE_NUMBER_OF_MILLISECONDS_IN_ONE_DAY;
	}
	public static long hoursBetweenTwoTimes(String time1, String time2) throws ParseException{
		Calendar calendar1 = stringToCalendar(time1, TimeFormat.YYYYMMDD_HHMMSS_WITH_FORWARD_SLASH);
		Calendar calendar2 = stringToCalendar(time2, TimeFormat.YYYYMMDD_HHMMSS_WITH_FORWARD_SLASH);
		long result = Math.abs(calendar1.getTimeInMillis() - calendar2.getTimeInMillis());
		return result/THE_NUMBER_OF_MILLISECONDS_IN_ONE_HOUR;
	}
	/**
	 * 返回指定日期/时间范围内的一个日期/时间
	 * @param date1 日期1, 必须和timeFormat描述的格式一致
	 * @param date2 日期2, 和日期1的先后顺序随意, 必须和timeFormat描述的格式一致
	 * @param timeFormat 日期解析格式
	 * @return 随机的日期和时间, 格式由timeFormat参数决定
	 * @throws ParseException 两个日期和日期的范式不匹配时或字符串描述的日期错误时
	 */
	public static String getRandomDateTime(String date1, String date2, TimeFormat timeFormat) throws ParseException{
		long millis1 = stringToCalendar(date1, timeFormat).getTimeInMillis();
		long millis2 = stringToCalendar(date2, timeFormat).getTimeInMillis();
		if(millis1 > millis2){
			long temp = millis1;millis1 = millis2;millis2 = temp;
		}
		long difference = millis2 - millis1;
		long randomDateInMillis = (long)(Math.random() * difference) + millis1;
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(randomDateInMillis);
		SimpleDateFormat dfsimple = new SimpleDateFormat(timeFormat.toString());
		return dfsimple.format(calendar.getTime());
	}
	/**
	 * String类型的日期/时间转换为Calendar类型的日期/时间
	 * @param dateString 日期/时间的字符串, 必须和timeFormat描述的格式一致
	 * @param timeFormat 日期解析格式
	 * @return Calendar
	 * @throws ParseException 当dateString或timeFormat不匹配时或字符串描述的日期错误时
	 */
	public static Calendar stringToCalendar(String dateString, TimeFormat timeFormat) throws ParseException{
		if(dateString == null || "".equals(dateString)){
			return Calendar.getInstance();
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeFormat.toString());
		Date date =simpleDateFormat.parse(dateString);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}
	/**
	 * String类型的日期/时间转换为Date类型的日期/时间
	 * @param dateString 源日期/时间的字符串
	 * @param timeFormat 日期解析格式
	 * @return Date
	 * @throws ParseException 当字符串和日期范式不匹配时或字符串描述的日期错误时
	 */
	public static Date stringToDate(String dateString, TimeFormat timeFormat) throws ParseException{
		if(dateString == null || "".equals(dateString)){
			Calendar calendar = Calendar.getInstance();
			return calendar.getTime();
		}
		return new SimpleDateFormat(timeFormat.toString()).parse(dateString);
	}
	/**
	 * 将Calendar对象描述的日期转换为String类型, 字符串格式由timeFormat指定
	 * 
	 * @param calendar
	 * @param timeFormat 日期解析格式
	 * @return String 以字符串方式描述的日期
	 */
	public static String calendarToString(Calendar calendar, TimeFormat timeFormat){
		String result = null;
		SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.toString());
		result = sdf.format(calendar.getTime());
		return result;
	}
	/**
	 * 将Date类型对象描述的日期转换为String类型, 字符串格式由timeFormat指定
	 * @param date
	 * @param timeFormat 日期解析格式
	 * @return String 以字符串方式描述的日期
	 */
	public static String dateToString(Date date, TimeFormat timeFormat){
		String result = null;
		SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.toString());
		result = sdf.format(date);
		return result;
	}
}
