package com.trs.hybase.test.bean;

import java.io.File;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

public class Constants {
	/** log4j2.xml 默认路径 */
	public final static String DEFAULT_LOG4J2_XML_PATH = "./config/log4j2.xml";
	/** 换行符 */
	public final static String LS = System.lineSeparator();
	/** 默认DateTimeFormatter  */
	public final static DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	/** JDBC驱动类 */
	public final static String JDBC_HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
	/** 循环等待时, 最大等待轮数 */
	public final static int MAX_LOOP_SIZE = 60;
	/** 每次循环等待时间间隔, 单位毫秒 */
	public final static long WAITINGTIME_EACH_LOOP = 1000;
	
	public static class LogModule {
		/** 内层try-catch(也就是要断言异常号和预期异常号是否相等的try-catch块) 的日志模板 */
		public final static String INNER_EXCEPTION = "errorCode=%d, expected=%d, errorString=%s%s%s";
		/** 建表日志模板 */
		public final static String CREATE_DATABASE = "[%s].createDatabase(%s)";
		/** 插入记录日志模板 */
		public final static String INSERT_RECORDS = "[%s].insertRecords(%s, inputRecords(size=%d))";
		/** 检索日志模板 */
		public final static String QUERY = "resultSet = [%s].executeSelect(%s, %s, %d, %d, %s)";
		/** 获取结果集大小和命中记录数的日志模板 */
		public final static String RESULTSET_SIZE_NUMFOUND = "resultSet.getSize=%d, resultSet.getNumFound=%d";
	}
	
	/**
	 * 重新配置 log4j2 的配置
	 */
	public static void reconfigureLog4j2() {
		reconfigureLog4j2(Constants.DEFAULT_LOG4J2_XML_PATH);
	}
	/**
	 * 指定 log4j2 的配置文件位置并重新配置
	 * @param path
	 */
	public static void reconfigureLog4j2(String path) {
		LoggerContext logContext = (LoggerContext) LogManager.getContext(false);
		logContext.setConfigLocation(new File(path).getAbsoluteFile().toURI());
		logContext.reconfigure();
	}
	/**
	 * 线程挂起
	 * @param waitTime
	 */
	public static void sleep(Long waitTime) {
		if(waitTime == null || waitTime < 0)
			return;
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
