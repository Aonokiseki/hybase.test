package com.trs.hybase.test.common.trsconnection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.trs.hybase.client.APIVersion;
import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSInputRecord;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.SearchParams;
import com.trs.hybase.test.annotation.DatabaseAnalyzer;
import com.trs.hybase.test.bean.Configurer;
import com.trs.hybase.test.bean.Constants;
import com.trs.hybase.test.bean.GlobalSetting;
import com.trs.hybase.test.util.Other;

public class ExecuteSelectTest {
	private final static Logger logger = LogManager.getLogger(ExecuteSelectTest.class);
	private final static Gson GSON = new Gson();
	private TRSConnection conn;
	private GlobalSetting globalSetting;
	
	@BeforeClass
	public void beforeClass() {
		Constants.reconfigureLog4j2();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configurer.class);
		conn = context.getBean("first", TRSConnection.class);
		globalSetting = context.getBean(GlobalSetting.class);
		context.close();
		logger.info(String.format("TRSConnectionTest, conn=%s, APIVersion=%s", conn.getURL(), APIVersion.getVersion()));
	}
	
	@DataProvider(name="executeInsertDataProvider")
	public Object[][] executeInsertDataProvider(Method method) throws JsonSyntaxException, JsonIOException, FileNotFoundException{
		if(!"executeInsert".equals(method.getName()))
			return null;
		
		return new Object[][] {
			new Object[] {1, "这是用例说明", 0, TRSDatabase.DBPOLICY.NORMAL, "版次:*", "版次", new String[] {"4", "3", "2", "1"}},
		};
	}
	/**
	 * 验证 executeInsert() 有效
	 * @param caseId 
	 * @param dbType 数据库类型
	 * @param policy 数据库策略
	 * @param query 检索表达式
	 * @param checkingColumn 欲检查的字段名
	 * @param expectedIds 预期返回的记录数组, 数组的每个元素均为"欲检查的字段名"的值
	 */
	@Test(dataProvider="executeInsertDataProvider")
	public void executeInsert(
			int caseId, String description, int dbType, TRSDatabase.DBPOLICY policy, String query, String checkingColumn, String[] expectedIds) {
		String methodName = Other.getMethodName();
		logger.info(String.format("%s, caseId=%d, mission=%s", methodName, caseId, description));
		String dbName = "system.executeInsert_"+System.currentTimeMillis();
		try {
			/* 建表 */
			TRSDatabase db = DatabaseAnalyzer.parse("com.trs.hybase.test.pojo.SimpleView");
			db.setName(dbName);
			conn.createDatabase(db);
			/* 记录入库 */
			@SuppressWarnings("unchecked")
			List<Map<String,String>> jsons = GSON.fromJson(new InputStreamReader(
					new FileInputStream("./data/TRSConnection/executeInsert.json"), "UTF-8"),  java.util.List.class);
			List<TRSInputRecord> inputRecords = preparingSimpleDataForInsertion(jsons);
		    conn.executeInsert(dbName, inputRecords);
		    Constants.sleep(globalSetting.sleepTimeMillis());
		    /* 检索 */
		    SearchParams sp = new SearchParams();
		    sp.setSortMethod("-版次");
		    queryChecking(conn, dbName, query, sp, checkingColumn, expectedIds);
		    logger.debug(String.format("%s, caseId=%d, succeed", methodName, caseId));
		}catch(ClassNotFoundException | JsonSyntaxException | JsonIOException | UnsupportedEncodingException | FileNotFoundException e) {
			String failureLog = String.format("%s, caseId=%d failed%s%s", methodName, caseId, Constants.LS, Other.stackTraceToString(e));
			logger.error(failureLog);
			fail(failureLog);
		}catch(TRSException e) {
			String failureLog = String.format("%s, caseId=%d failed. errorCode=%d, errorString=%s%s%s",
					methodName, caseId, e.getErrorCode(), e.getErrorString(), Constants.LS, Other.stackTraceToString(e));
			logger.error(failureLog);
			fail(failureLog);
		}finally {
			try {
				conn.deleteDatabase(dbName);
			}catch(TRSException e) {}
		}
	}
	
	private static List<TRSInputRecord> preparingSimpleDataForInsertion(List<Map<String, String>> testdatas) throws TRSException{
		List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>(testdatas.size());
		TRSInputRecord inputRecord = null;
		for(int i=0, size=testdatas.size(); i<size; i++) {
			inputRecord = new TRSInputRecord();
			for(Entry<String,String> data : testdatas.get(i).entrySet())
				inputRecord.addColumn(data.getKey(), data.getValue());
			inputRecords.add(inputRecord);
		}
		return inputRecords;
	}
	/**
	 * 检索并断言结果
	 * @param conn
	 * @param dbName
	 * @param query
	 * @param sp
	 * @param chekcingColumn
	 * @param expectation
	 * @throws TRSException
	 */
	private static void queryChecking(TRSConnection conn, String dbName, String query, SearchParams sp, String chekcingColumn, String[] expectation) throws TRSException {
		TRSResultSet resultSet = conn.executeSelect(dbName, query, 0, 10, sp);
		assertEquals(resultSet.getNumFound(), expectation.length);
		TRSRecord record = null;
		for(int i=0; i<resultSet.size(); i++) {
			record = resultSet.get();
			assertEquals(record.getString(chekcingColumn), expectation[i]);
			resultSet.moveNext();
		}
	}
}
