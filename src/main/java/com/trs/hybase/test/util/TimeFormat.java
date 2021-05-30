package com.trs.hybase.test.util;
/**
 * 枚举,罗列常用的日期解析的格式<br><br>
 * 
 * <code>YYYYMMDD<code> == yyyyMMdd <br><br>
 * <code>YYYYMMDD_WITH_PERIOD<code> == yyyy.MM.dd <br><br>
 * <code>YYYYMMDD_WITH_FORWARD_SLASH<code> == yyyy/MM/dd <br><br>
 * <code>YYYYMMDD_WITH_BACK_SLANT<code> == yyyy\\MM\\dd <br><br>
 * <code>YYYYMMDD_WITH_HYPHEN<code> == yyyy-MM-dd <br><br>
 * 
 * <code>YYYYMMDD_HHMMSS<code> == yyyyMMdd HH:mm:ss <br><br>
 * <code>YYYYMMDD_HHMMSS_WITH_PERIOD<code> == yyyy.MM.dd HH:mm:ss <br><br>
 * <code>YYYYMMDD_HHMMSS_WITH_FORWARD_SLASH<code> == yyyy/MM/dd HH:mm:ss <br><br>
 * <code>YYYYMMDD_HHMMSS_WITH_BACK_SLANT<code> == yyyy\\MM\\dd HH:mm:ss <br><br>
 * <code>YYYYMMDD_HHMMSS_WITH_HYPHEN<code> == yyyy-MM-dd HH:mm:ss <br><br>
 * 
 * <code>YYYYMMDD_HHMMSS_SSS<code> == yyyyMMdd HH:mm:ss.SSS <br><br>
 * <code>YYYYMMDD_HHMMSS_SSS_WITH_PERIOD<code> == yyyy.MM.dd HH:mm:ss.SSS <br><br>
 * <code>YYYYMMDD_HHMMSS_SSS_WITH_FORWARD_SLASH<code> == yyyy/MM/dd HH:mm:ss.SSS <br><br>
 * <code>YYYYMMDD_HHMMSS_SSS_WITH_BACK_SLANT<code> == yyyy\\MM\\dd HH:mm:ss.SSS <br><br>
 * <code>YYYYMMDD_HHMMSS_SSS_WITH_HYPHEN<code> == yyyy-MM-dd HH:mm:ss.SSS <br><br>
 */
public enum TimeFormat {
	
	YYYYMMDD("yyyyMMdd"),
	YYYYMMDD_WITH_PERIOD("yyyy.MM.dd"),
	YYYYMMDD_WITH_FORWARD_SLASH("yyyy/MM/dd"),
	YYYYMMDD_WITH_BACK_SLANT("yyyy\\MM\\dd"),
	YYYYMMDD_WITH_HYPHEN("yyyy-MM-dd"),
	
	YYYYMMDD_HHMMSS("yyyyMMdd HH:mm:ss"),
	YYYYMMDD_HHMMSS_WITH_PERIOD("yyyy.MM.dd HH:mm:ss"),
	YYYYMMDD_HHMMSS_WITH_FORWARD_SLASH("yyyy/MM/dd HH:mm:ss"),
	YYYYMMDD_HHMMSS_WITH_BACK_SLANT("yyyy\\MM\\dd HH:mm:ss"),
	YYYYMMDD_HHMMSS_WITH_HYPHEN("yyyy-MM-dd HH:mm:ss"),
	
	YYYYMMDD_HHMMSS_SSS("yyyyMMdd HH:mm:ss.SSS"),
	YYYYMMDD_HHMMSS_SSS_WITH_PERIOD("yyyy.MM.dd HH:mm:ss.SSS"),
	YYYYMMDD_HHMMSS_SSS_WITH_FORWARD_SLASH("yyyy/MM/dd HH:mm:ss.SSS"),
	YYYYMMDD_HHMMSS_SSS_WITH_BACK_SLANT("yyyy\\MM\\dd HH:mm:ss.SSS"),
	YYYYMMDD_HHMMSS_SSS_WITH_HYPHEN("yyyy-MM-dd HH:mm:ss.SSS");
	
	private String pattern;
	private TimeFormat(String pattern){
		this.pattern = pattern;
	}
	@Override
	public String toString(){
		return this.pattern;
	}
}
