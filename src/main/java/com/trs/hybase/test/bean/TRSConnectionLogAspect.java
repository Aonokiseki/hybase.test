package com.trs.hybase.test.bean;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
//import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSInputRecord;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
//import com.trs.hybase.test.util.Other;
import com.trs.hybase.test.util.Tools;

@Aspect
@Component
public class TRSConnectionLogAspect {
	
	private final static Logger logger = LogManager.getLogger(LogManager.getLogger());
	
	public static String argsToString(Object... args) {
		if(args == null)
			return "";
		StringBuilder stringBuilder = new StringBuilder();
		for(int i=0; i<args.length; i++) {
			if(args[i] != null)
				stringBuilder.append(args[i].toString());
			else
				stringBuilder.append("null");
			if(i < args.length - 1)
				stringBuilder.append(", ");
		}
		return stringBuilder.toString();
	}
	/*
	 * createDatabase 切面
	 */
	@Before(value="execution(* com.trs.hybase.client.TRSConnection.createDatabase(com.trs.hybase.client.TRSDatabase))")
	public void beforeCreateDatabase(JoinPoint point) {
		String method = point.getSignature().getName();
		Object[] args = point.getArgs();
		logger.info(String.format("%s(%s)", method, ((TRSDatabase)args[0]).getName()));
	}
//	@AfterThrowing(
//		value="execution(* com.trs.hybase.client.TRSConnection.createDatabase(com.trs.hybase.client.TRSDatabase))", 
//		throwing="e")
//	public void afterCreateDatabaseThrowing(JoinPoint point, Throwable e) {
//		String method = point.getSignature().getName();
//		Object[] args= point.getArgs();
//		logger.error(String.format("%s(%s)%s%s", 
//				method, ((TRSDatabase)args[0]).getName(), Constants.LS, Other.stackTraceToString(e)));
//	}
	/*
	 * executeInsert 切面
	 */
	@Before(value = "execution(* com.trs.hybase.client.TRSConnection.executeInsert(String, java.util.List,..))")
	public void beforeExecuteInsert(JoinPoint point) {
		String method = point.getSignature().getName();
		Object[] args = point.getArgs();
		logger.info(String.format("%s(%s)", method, handleExecuteInsertParameters(args)));
	}
//	@AfterThrowing(
//		value="execution(void com.trs.hybase.client.TRSConnection.executeInsert(String, java.util.List,..))", 
//		throwing="e")
//	public void afterExecuteInsertThrowing(JoinPoint point, Throwable e) {
//		String method = point.getSignature().getName();
//		Object[] args = point.getArgs();
//		logger.info(String.format("%s(%s)%s%s", method, handleExecuteInsertParameters(args), Constants.LS,
//				Other.stackTraceToString(e)));
//	}
	@SuppressWarnings("unchecked")
	private String handleExecuteInsertParameters(Object[] args) {
		StringBuilder sb = new StringBuilder();
		List<TRSInputRecord> inputRecords = (java.util.List<TRSInputRecord>)args[1]; 
		sb.append(args[0]).append(", inputRecords(size=").append(inputRecords.size()).append(")");
		if(args.length == 2)
			return sb.toString();
		sb.append(", ").append(args[2]);
		if(args.length == 3)
			return sb.toString();
		sb.append(", ").append(args[3]);
		return sb.toString();
	}
	/*
	 * executeSelect 切面
	 */
	@Before(value="execution(* com.trs.hybase.client.TRSConnection.executeSelect(..))")
	public void beforeExecuteSelect(JoinPoint point) {
		String method = point.getSignature().getName();
		Object[] args = point.getArgs();
		logger.info(String.format("%s(%s)", method, argsToString(args)));
	}
	@AfterReturning(
		value="execution(com.trs.hybase.client.TRSResultSet com.trs.hybase.client.TRSConnection.executeSelect(..))", 
		returning="resultSet")
	public void afterExecuteSelect(JoinPoint point, TRSResultSet resultSet) throws TRSException {
		logger.info("resultSet.size="+resultSet.size()+", getNumFound="+resultSet.getNumFound());
		logger.debug(resultSetToString(resultSet));
	}
//	@AfterThrowing(value="execution(* com.trs.hybase.client.TRSConnection.executeSelect(..))", throwing="e")
//	public void afterExecuteSelectThrowing(JoinPoint point, Throwable e) {
//		String method = point.getSignature().getName();
//		Object[] args = point.getArgs();
//		logger.error(String.format("%s(%s)%s%s", 
//				method, argsToString(args), Constants.LS, Other.stackTraceToString(e)));
//	}
	private static String resultSetToString(TRSResultSet resultSet) throws TRSException {
		TRSRecord record = null;
		StringBuilder resultSetStringBuilder = new StringBuilder();
		for(int i=0, size=resultSet.size(); i<size; i++) {
			resultSet.moveNext();
			resultSetStringBuilder.append(Constants.LS);
			record = resultSet.get();
			resultSetStringBuilder.append(Tools.recordToString(record));
		}
		/* 结果集在测试方法中还需要做断言,这里必须将指针挪到第一条记录的位置 */
		resultSet.moveFirst();
		return resultSetStringBuilder.toString();
	}
	/*
	 * loadRecords 切面
	 */
	@Before(value="execution(long com.trs.hybase.client.TRSConnection.loadRecords(..))")
	public void beforeLoadRecords(JoinPoint point) {
		String method = point.getSignature().getName();
		Object[] args = point.getArgs();
		logger.info(String.format("%s(%s)", method, argsToString(args)));
	}
	@AfterReturning(value="execution(long com.trs.hybase.client.TRSConnection.loadRecords(..))", returning="number")
	public void afterLoadRecords(JoinPoint point, long number) {
		logger.info("number="+number);
	}
//	@AfterThrowing(value="execution(long com.trs.hybase.client.TRSConnection.loadRecords(..))", throwing="e")
//	public void afterLoadRecordsThrowing(JoinPoint point, Throwable e) {
//		String method = point.getSignature().getName();
//		Object[] args = point.getArgs();
//		logger.error(String.format("%s(%s)%s%s", 
//				method, argsToString(args), Constants.LS, Other.stackTraceToString(e)));
//	}
}