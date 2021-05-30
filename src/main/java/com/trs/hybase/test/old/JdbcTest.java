package com.trs.hybase.test.old;

import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import com.trs.hybase.client.APIVersion;
import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSInputRecord;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
import com.trs.hybase.test.util.Other;

public class JdbcTest {
	private final static Logger logger = Logger.getLogger(JdbcTest.class);
	private TRSConnection conn;
	private final static String targetClass = "org.apache.hive.jdbc.HiveDriver";
	private final static int MAX_LOOP_SIZE = 60;
	private final static long WAITINGTIME_EACH_LOOP = 1000;
	
	@BeforeMethod
	public void beforeMethod(){
		ConnectParams cp = new ConnectParams();
		conn = new TRSConnection("http://127.0.0.1:5555", "admin", "trsadmin", cp);
	}
	
	@AfterMethod
	public void afterMothod() throws TRSException{
		if(conn != null)
			conn.close();
	}
	
	@BeforeClass
	public void beforeClass(){
		PropertyConfigurator.configure("./log4j.properties");
		logger.debug("JdbcTest.class, API Version="+APIVersion.getVersion());
	}
	
	/**
	 *  DriverManager类的getConnection()方法访问hybase测试
	 */
	@Test
	public void connectionTest(){
		Exception exception = null;
		try{
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection(
					"jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			connection.close();
		}catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
			exception = e;
		}
		Assert.assertNull(exception);
	}
	
	/**
	 * 验证prepareStatement(sql)的用法<br>
	 * 验证PrearedStatement类executeQuery(sql)和execute（sql）方法<br>
	 * 验证ResultSet类的：getRow（）、next()、getString（int col）、getMetaData()、getColumnCount()<br>
	 */
	@Test
	public void prepareStatement(){
		logger.debug(Other.getMethodName());
		String dbName = "system.demo"+System.currentTimeMillis();
		try{
			TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_VIEW, TRSDatabase.DBPOLICY.FASTEST);
			db.addColumn(new TRSDatabaseColumn("rowid", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("日期", TRSDatabaseColumn.TYPE_DATE));
			db.addColumn(new TRSDatabaseColumn("版次", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("版名", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("标题", TRSDatabaseColumn.TYPE_PHRASE));
			db.addColumn(new TRSDatabaseColumn("作者", TRSDatabaseColumn.TYPE_CHAR).setMultivalue(true));
			db.addColumn(new TRSDatabaseColumn("正文", TRSDatabaseColumn.TYPE_DOCUMENT));
			db.setParter("rowid");
			db.setSplitter("number", "版次", null);
			conn.createDatabase(db);
			long number = conn.loadRecords(dbName, "."+"/JDBC/demo.trs", 0);
			Assert.assertEquals(5079, number);
			sleep();
			/*
			 * 先用hybase自带的检索接口获取检索结果
			 */
			TRSResultSet trsResultSet = conn.executeSelect(dbName, "版次:1", 0, 5, new SearchParams());
			TRSRecord record = null;
			/*
			 * 再用JDBC插件获取检索结果, 二者检索结果的值应该一一对应
			 */
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = connection.prepareStatement("select * from "+dbName+" where 版次=? limit 0,5");
			preparedStatement.setString(1, "1");
			Assert.assertTrue(preparedStatement.execute());
			ResultSet sqlResultSet = null;
			int loop = 0;
			while(sqlResultSet == null && loop < MAX_LOOP_SIZE){
				logger.debug("loop == "+loop);
				sqlResultSet = preparedStatement.executeQuery();
				try {
					Thread.sleep(WAITINGTIME_EACH_LOOP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				loop++;
			}
			ResultSetMetaData metaData = null;
			/*
			 * 同时向后挪动指针, 比较两个结果集每条记录的rowid字段, 每条记录的字段数
			 */
			while(trsResultSet.moveNext()){
				sqlResultSet.next();
				record = trsResultSet.get();
				/*
				 * 比较rowid字段的值
				 */
				Assert.assertEquals(sqlResultSet.getString(1), record.getString("rowid"));
				metaData = sqlResultSet.getMetaData();
				/*
				 * 比较每条记录的字段数, 这里两个结果集应该都返回全部字段
				 */
				Assert.assertEquals(metaData.getColumnCount(), record.getColumnNames().length);
			}
			/*
			 * 验证JDBC接口获取到的结果集也是最后一条记录
			 */
			Assert.assertEquals(false, sqlResultSet.next());
			connection.close();		
		} catch (SQLException | ClassNotFoundException | TRSException e) {
			logger.error(System.lineSeparator()+Other.stackTraceToString(e));
			Assert.fail(e.toString());
		} finally{
			try{
				conn.deleteDatabase(dbName);
			}catch(TRSException ex){
				ex.printStackTrace();
			}
			sleep();
		}
	}
	
	/**
	 * 
	 * 验证<code>Connection.createStatement()</code><br>
	 * Statement   executeQuery(sql);<br>
	 * Statement   execute(sql);<br>
	 * 
	 * 做法同理, 相同检索表达式, 同时使用Hybase的接口和JDBC接口执行检索<br>, 依次比较每条记录的特定字段的值
	 */
	@Test
	public void statementTest(){
		logger.debug(Other.getMethodName());
		String dbName = "system.demo"+System.currentTimeMillis();
		try {
			TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_VIEW, TRSDatabase.DBPOLICY.FASTEST);
			db.addColumn(new TRSDatabaseColumn("rowid", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("日期", TRSDatabaseColumn.TYPE_DATE));
			db.addColumn(new TRSDatabaseColumn("版次", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("版名", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("标题", TRSDatabaseColumn.TYPE_PHRASE));
			db.addColumn(new TRSDatabaseColumn("作者", TRSDatabaseColumn.TYPE_CHAR).setMultivalue(true));
			db.addColumn(new TRSDatabaseColumn("正文", TRSDatabaseColumn.TYPE_DOCUMENT));
			db.setParter("rowid");
			db.setSplitter("number", "版次", null);
			conn.createDatabase(db);
			long number = conn.loadRecords(dbName, "."+"/JDBC/demo.trs", 0);
			Assert.assertEquals(5079, number);
			sleep();
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			Statement statement = connection.createStatement();
			Assert.assertTrue(statement.execute("select * from "+dbName+" limit 10000"));
			ResultSet sqlResultSet = null;
			int loop = 0;
			while(sqlResultSet == null && loop < MAX_LOOP_SIZE){
				logger.debug("loop == "+loop);
				sqlResultSet = statement.executeQuery("select * from "+dbName+" limit 10000");
				try {
					Thread.sleep(WAITINGTIME_EACH_LOOP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				loop++;
			}
			TRSResultSet trsResultSet = conn.executeSelect(dbName, "*:*", 0, 10000, new SearchParams());
			TRSRecord trsRecord = null;
			List<String> rowidList = new ArrayList<String>();
			String rowid = null;
			int indexOfSqlResultSet = 0;
			/*
			 * 将SQL命中的所有记录中，非空的rowid值放入一个List中
			 */
			while(sqlResultSet.next()){
				indexOfSqlResultSet++;
				rowid = sqlResultSet.getString("rowid").trim();
				if(rowid == null || "".equals(rowid))
					continue;
				rowidList.add(rowid);
			}
			Assert.assertEquals(trsResultSet.size(), indexOfSqlResultSet);
			/*
			 * Hybase的接口检索，检查命中的所有记录，非空的rowid是否均位于List内
			 */
			while(trsResultSet.moveNext()){
				trsRecord = trsResultSet.get();
				rowid = trsRecord.getString("rowid");
				if(rowid == null || "".equals(rowid))
					continue;
				Assert.assertTrue(rowidList.contains(rowid));
			}
		} catch (ClassNotFoundException | SQLException | TRSException e) {
			logger.error(System.lineSeparator() + Other.stackTraceToString(e));
			Assert.fail(e.toString());
		} finally{
			try{
				conn.deleteDatabase(dbName);
			}catch(TRSException ex){
				ex.printStackTrace();
			}
			sleep();
		}
	}
	
	@DataProvider(name = "SqlDqlTestData")
	public Object[][] SqlDqlTestData(Method m){
		Object[][] result = null;
		if("SqlDqlTest".equals(m.getName().trim())){
			result = new Object[][]{
				/*精确检索*/
				new Object[]{0, "select * from system.SQL1 where id=1", "id", new String[]{"1"}, -1},
				/*CHAR字段模糊检索*/
				new Object[]{1, "select * from system.SQL1 where 字符='北*' order by id asc", "id", new String[]{"1", "10"}, -1},
				new Object[]{2, "select * from system.SQL1 where 字符='*公园' order by id asc", "id", new String[]{"1","11","12","2","3","7","8"}, -1},
				/*全文字段, 正则表达式*/
				new Object[]{3, "select * from system.SQL1 where 正文=full_text(\"/ABC[CD]D[EF]*/\")", "id", new String[]{"12"}, -1},
				/*正文字段, 相关度排序*/
				new Object[]{4, "select * from system.SQL1 where 正文=full_text(\"中心^50\") OR 标题='中国' order by fulltext asc ", "id", new String[]{"9","8"}, -1},
				/*全文字段, 短语检索*/
				new Object[]{5, "select * from system.SQL1 where 正文=full_text(\"深入学习\")", "id", new String[]{"7"}, -1},
				/*全文字段 位置关系*/
				new Object[]{6, "select * from system.SQL1 where 正文=full_text(\"\\\"深入 实践\\\" ~2\")", "id", new String[]{}, -1},
				/*全文检索, 通配符*/
				new Object[]{7, "select * from system.SQL1 where 正文=full_text(\"关于*\") order by id asc", "id", new String[]{"6","9"}, -1},
				/*范围检索,注意id字段类型是CHAR, 按照字典排序, 10 就应该在 3 前边*/
				new Object[]{8, "select * from system.SQL1 where 日期=20170501 - 20170531 order by id asc", "id", new String[]{"1", "10","11","12", "3"}, -1},
				/*雷达检索*/
				new Object[]{9, "select * from system.SQL1 where GEO_DISTANCE(Point, '10km', 116.450615, 39.980540) order by id asc", "id", new String[]{"1","2","3","4","5","6","7","8","9"}, -1},
				new Object[]{10, "select * from system.SQL1 where GEO_DISTANCE(Point, '10500m', 116.450615, 39.980540) order by id asc", "id", new String[]{"1","2","3","4","5","6","7","8","9"}, -1},
				/*多边形检索*/
				new Object[]{11, "select * from system.SQL1 where GEO_POLYGON(Point, 116.297150,39.947121,  116.393623,39.890509,  116.400490,39.926325) order by id asc", "id", new String[]{"1", "10", "11", "4", "6", "8"}, -1},
				/*边界查询*/
				new Object[]{12, "SELECT * FROM system.SQL1 WHERE GEO_BOUNDING_BOX(Point, 116.311891,39.942186,  116.333950,39.937974) order by id asc", "id", new String[]{"10","11"}, -1},
				
			};
		}
		return result;
	}
	/**
	 * 检查SQL的各种检索语句
	 * @param SQL SQL表达式
	 * @param expectationColumn 检查字段
	 * @param expectation 各命中记录检查字段的预期结果
	 * @param expectationErrorCode 预期错误码
	 */
	@Test(dataProvider = "SqlDqlTestData")
	public void SqlDqlTest(int caseID, String SQL, String expectationColumn, String[] expectation, int expectationErrorCode){
		logger.debug(Other.getMethodName()+", caseId=="+caseID);
		String dbName = "system.SQL1";
		try{
			TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_DATABASE, TRSDatabase.DBPOLICY.FASTEST);
			db.addColumn(new TRSDatabaseColumn("id", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("字符", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("数值", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("数值2", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("Point", TRSDatabaseColumn.TYPE_CHAR).setProperty("index.analyzer", "geo_point"));
			db.addColumn(new TRSDatabaseColumn("日期", TRSDatabaseColumn.TYPE_DATE));
			db.addColumn(new TRSDatabaseColumn("短语", TRSDatabaseColumn.TYPE_PHRASE));
			db.addColumn(new TRSDatabaseColumn("正文", TRSDatabaseColumn.TYPE_DOCUMENT));
			db.setSplitter("string", "id", null);
			db.setParser(TRSDatabase.ANALYZER_TRSCJK);
			conn.createDatabase(db);
			Map<String,String> loadOptions = new HashMap<String,String>();
			loadOptions.put("parser.trs.charset", "UTF-8");
			long number = conn.loadRecords(dbName, "."+"/JDBC/system.SQL.trs", null, loadOptions);
			logger.debug("conn.loadRecords("+dbName+", "+"."+"/JDBC/system.SQL.trs"+", null, loadOptions)=="+number+", expectation=="+12);
			sleep();
			Assert.assertEquals(12, number);
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = null;
			ResultSet sqlResultSet = null;
			int loop = 0;
			try{
				preparedStatement = connection.prepareStatement(SQL);
				logger.debug("preparedStatment = connection.prepareStatement("+SQL+");");
				TRSDatabase[] dbs = null;
				while(sqlResultSet == null && loop < MAX_LOOP_SIZE){
					logger.debug("loop == "+loop);
					dbs = conn.getDatabases(dbName);
					if(dbs != null && dbs.length == 1)
						logger.debug("conn.getDatabase("+dbName+")[0]=="+dbs[0].getName());
					else
						logger.debug("conn.getDatabase("+dbName+")==null || length == 0");
					sqlResultSet = preparedStatement.executeQuery();
					try {
						Thread.sleep(WAITINGTIME_EACH_LOOP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					loop++;
				}
			}catch(SQLException se){
				logger.debug(Other.stackTraceToString(se));
				Assert.assertEquals(se.getErrorCode(), expectationErrorCode);
				return;
			}
			sleep();
			if(sqlResultSet == null){
				logger.error("sqlResultSet: NullPointerException.");
				Assert.fail("sqlResultSet: NullPointerException.");
			}
			int indexOfExpectation = -1;
			while(sqlResultSet.next()){
				indexOfExpectation++;
				/**
				 * 旧代码上面等待了4倍时间，现改为1倍，case4可能运行不过，待赵阳调整
				 * */
				logger.debug("indexOfExpectation=="+indexOfExpectation);
				if(sqlResultSet.getString(expectationColumn) != null && !"".equals(sqlResultSet.getString(expectationColumn)))
					logger.debug("sqlResultSet.getString("+expectationColumn+")=="+sqlResultSet.getString(expectationColumn));
				if(indexOfExpectation < expectation.length)
					logger.debug("expectation["+indexOfExpectation+"]=="+expectation[indexOfExpectation]);
				else
					logger.debug("indexOfException >= expectation.length");
//				Assert.assertEquals(sqlResultSet.getString(expectationColumn), expectation[indexOfExpectation]);
			}
			Assert.assertEquals(expectation.length-1, indexOfExpectation);
		}catch(TRSException | SQLException | ClassNotFoundException ex){
			logger.error(System.lineSeparator() + Other.stackTraceToString(ex));
			Assert.fail(ex.toString());
		}finally{
			try{
				conn.deleteDatabase(dbName);
			}catch(TRSException e){
				e.printStackTrace();
			}
			sleep();
		}
	}
	
	@DataProvider(name = "SqlDqlNumberData")
	public Object[][] sqlDqlNumberData(Method m){
		Object[][] result = null;
		if("sqlDqlNumber".equals(m.getName().trim())){
			result = new Object[][]{
					new Object[]{0, "select count(数值) from system.SQL2 ",new String[]{"12"}},
					new Object[]{1, "select max(数值2) from system.SQL2 ",new String[]{"4096.0"}},
					new Object[]{2, "select min(数值) from system.SQL2 ",new String[]{"233.0"}},
					new Object[]{3, "select avg(数值2) from system.SQL2 ",new String[]{"682.5"}},
					new Object[]{4, "select pow(数值, 0.5)+pow(数值2, id) from SQL2 order by id asc",
							new String[]{
								"93.67333309092672","1.2676506002282294E30","2.6584559915698317E36","2.2300745198530623E43","114.58498871532116",
								"598.0639297266863","65604.93475175845","3.355450244856279E7","6.871947677434058E10","5.629499534213965E14"
							}
					}
			};
		}
		return result;
	}
	/**
	 * 计算函数, 包含count,max,min,avg等数学公式
	 * @param SQL
	 * @param expectation
	 */
	@Test(dataProvider = "SqlDqlNumberData")
	public void sqlDqlNumber(int caseId, String SQL, String[] expectation){
		logger.debug(Other.getMethodName()+", caseId=="+caseId);
		String dbName = "system.SQL2";
		try{
			TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_VIEW, TRSDatabase.DBPOLICY.FASTEST);
			db.addColumn(new TRSDatabaseColumn("id", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("字符", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("数值", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("数值2", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("Point", TRSDatabaseColumn.TYPE_CHAR).setProperty("index.analyzer", "geo_point"));
			db.addColumn(new TRSDatabaseColumn("日期", TRSDatabaseColumn.TYPE_DATE));
			db.addColumn(new TRSDatabaseColumn("短语", TRSDatabaseColumn.TYPE_PHRASE));
			db.addColumn(new TRSDatabaseColumn("正文", TRSDatabaseColumn.TYPE_DOCUMENT));
			db.setParter("id");
			db.setSplitter("string", "id", null);
			db.setParser(TRSDatabase.ANALYZER_TRSCJK);
			conn.createDatabase(db);
			Map<String,String> loadOptions = new HashMap<String,String>();
			loadOptions.put("parser.trs.charset", "UTF-8");
			long number = conn.loadRecords(dbName, "."+"/JDBC/system.SQL.trs", null, loadOptions);
			sleep();
			Assert.assertEquals(12, number);
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = null;
			ResultSet sqlResultSet = null;
			preparedStatement = connection.prepareStatement(SQL);
			int loop = 0;
			while(sqlResultSet == null && loop < MAX_LOOP_SIZE){
				System.out.println("loop == "+loop);
				sqlResultSet = preparedStatement.executeQuery();
				try {
					Thread.sleep(WAITINGTIME_EACH_LOOP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				loop++;
			}
			if(sqlResultSet == null){
				logger.error("sqlResultSet: NullPointerException.");
				Assert.fail("sqlResultSet: NullPointerException.");
			}
			int indexOfException = -1;
			while(sqlResultSet.next()){
				indexOfException++;
				Assert.assertEquals(sqlResultSet.getString(1), expectation[indexOfException]);
			}
			Assert.assertEquals(expectation.length-1, indexOfException);
		}catch(TRSException | SQLException | ClassNotFoundException ex){
			logger.error(System.lineSeparator() + Other.stackTraceToString(ex));
			Assert.fail(ex.toString());
		}finally{
			try{
				conn.deleteDatabase(dbName);
			}catch(TRSException e){
				e.printStackTrace();
			}
			sleep();
		}
	}
	/**
	 * 建表
	 */
	@Test
	public void createTable(){
		logger.debug(Other.getMethodName());
		String createSentence = 
				"create table test(" +
						"num int, " +
						"title varchar(256)," +
						"doc1 text)";
		try{
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = connection.prepareStatement(createSentence);
			preparedStatement.executeUpdate();
			TRSDatabase[] dbArray = conn.getDatabases("test");
			TRSDatabaseColumn[] columns = dbArray[0].getAllColumns();
			Assert.assertEquals(columns[0].getName(), "num");
			Assert.assertEquals(columns[0].getColType(), 1);
			Assert.assertEquals(columns[1].getName(), "title");
			Assert.assertEquals(columns[1].getColType(), 2);
			Assert.assertEquals(columns[2].getName(), "doc1");
			Assert.assertEquals(columns[2].getColType(), 4);
		}catch(SQLException | TRSException | ClassNotFoundException e){
			logger.error(System.lineSeparator() + Other.stackTraceToString(e));
			Assert.fail(e.toString());
		}finally{
			try{
				conn.deleteDatabase("test");
			}catch(TRSException e){
				e.printStackTrace();
			}
			sleep();
		}
	}
	
	/**
	 * 插入记录和删表
	 */
	@Test
	public void insertAndDrop(){
		logger.debug(Other.getMethodName());
		String createSentence = "create table test1(" +
				"num int,"+
				"title varchar(256)"+
				")";
		try{
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = connection.prepareStatement(createSentence);
			preparedStatement.executeUpdate();
			preparedStatement.executeUpdate("insert into test1 values(1, test)");
			preparedStatement.executeUpdate("insert into test1 values(2, 测试)");
			sleep();
			//TRSDatabase[] dbArray = conn.getDatabases("test1");
			//Assert.assertEquals(dbArray[0].getRecordNum64(), 2);
			
			TRSResultSet ResultSet = conn.executeSelect("test1", "*:*", 0, 2,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),2);
			preparedStatement.executeUpdate("drop table test1");
		}catch(SQLException | ClassNotFoundException | TRSException e){
			logger.error(System.lineSeparator() + Other.stackTraceToString(e));
			Assert.fail(e.toString());
		}
	}
	/**
	 * 保留表结构,删除数据
	 */
	@Test
	public void truncateDelete(){
		logger.debug(Other.getMethodName());
		String createSentence = "create table test2(" +
				"num int, "+
				"title varchar(256)"+
				")";
		try{
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = connection.prepareStatement(createSentence);
			preparedStatement.executeUpdate();
			preparedStatement.executeUpdate("insert into test2 values(1, test)");
			preparedStatement.executeUpdate("insert into test2 values(2, 测试)");
			sleep();
			//TRSDatabase[] dbArray = conn.getDatabases("system.test2");
			//Assert.assertEquals(dbArray[0].getRecordNum64(), 2);
			
			TRSResultSet ResultSet = conn.executeSelect("system.test2", "*:*", 0, 2,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),2);
			preparedStatement.executeUpdate("delete from test2 where num=1");
			sleep();
			//dbArray = conn.getDatabases("test2");
			//Assert.assertEquals(dbArray[0].getRecordNum64(), 1);
			
			ResultSet = conn.executeSelect("test2", "*:*", 0, 1,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),1);
			preparedStatement.executeUpdate("truncate test2");
			sleep();
			//dbArray = conn.getDatabases("test2");
			//Assert.assertEquals(dbArray[0].getRecordNum64(), 0);
			
			ResultSet = conn.executeSelect("test2", "*:*", 0, 2,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),0);
			preparedStatement.executeUpdate("drop table test2");
			sleep();
			//dbArray = conn.getDatabases("test2");
			//Assert.assertEquals(dbArray.length, 0);
			
			ResultSet = conn.executeSelect("test2", "*:*", 0, 0,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),0);
		}catch(SQLException | ClassNotFoundException | TRSException e){
			logger.error(System.lineSeparator() + Other.stackTraceToString(e));
		}finally{
			try{
				conn.deleteDatabase("test2");
			}catch(TRSException e){
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void alter(){
		logger.debug(Other.getMethodName());
		try {
			Class.forName(targetClass);
			Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
			PreparedStatement preparedStatement = connection.prepareStatement("create table test3(num int,title varchar(256))");
			preparedStatement.executeUpdate();
			preparedStatement.executeUpdate("insert into test3 values(1,test)");
			preparedStatement.executeUpdate("insert into test3 values(2,测试)");
			sleep();
			//TRSDatabase[] dbArray = conn.getDatabases("test3");
	      	//Assert.assertEquals(dbArray[0].getRecordNum64(), 2);
	      	
	      	TRSResultSet ResultSet = conn.executeSelect("test3", "*:*", 0, 2,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),2);
	        preparedStatement.executeUpdate("alter table test3 add column 日期 date");
	      	preparedStatement.executeUpdate("insert into test3 values(2,测试,20171110)");
	      	sleep();
	      	//dbArray = conn.getDatabases("test3");
	      	//Assert.assertEquals(dbArray[0].getRecordNum64(), 3);
	      	
	      	ResultSet = conn.executeSelect("test3", "*:*", 0, 3,new SearchParams());
			logger.debug("ResultSet.size() == "+ResultSet.size());
	 	    assertEquals(ResultSet.size(),3);
	        preparedStatement.executeUpdate("drop table test3");
		} catch (ClassNotFoundException | SQLException | TRSException e) {
			logger.error(System.lineSeparator() + Other.stackTraceToString(e));
			Assert.fail(e.toString());
		}
	}
	
	@DataProvider(name = "searchParamsDataProvider")
	public Object[][] searchParamsDataProvider(Method method){
		if(!"searchParams".equals(method.getName()))
			return null;
		
		LocalDateTime now = LocalDateTime.now();
		String yesterday = now.plusDays(-1).format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")).substring(0, 8);
		String tomorrow = now.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")).substring(0, 8);
		/*
		 * 在设计预期时注意子库打开数为3, 有冷数据存在, 如果检索不到, 加上检索参数search.include.uncache
		 */
		return new Object[][] {
			/* search.include.uncache 检索冷数据 */
			new Object[] {1, "select * from %s where number >=0 and number <= 9 and PARAMS='search.include.uncache=true' order by number asc", 
					"number", new String[] {"0","1","2","3","4","5","6","7","8","9"}},
			/* search.range.filter 子库过滤 */
			/* 2021.4.15 当前有bug, 检索参数同时设置多个无效, 下版修复, 用 "&" 分隔多个检索参数 */
			/* 2021.5.8 parter和date之间用半角分号分隔(这是同一个检索参数离的值) */
			new Object[] {2, "select * from %s where number >=0 and PARAMS='search.range.filter=parter:\"\";date:["+yesterday+" TO "+tomorrow+"]' order by number asc", "number", new String[] {"6", "7", "8"}},
			/* search.ideo.single 检索冗余按字索引 */
			new Object[] {3, "select * from %s where phrase='再相' and PARAMS='search.ideo.single=true'", "number", new String[] {"9"}},
			/* READCOLUMNS */
			/* 2021.4.16 有bug, 提交至禅道*/
			/* 2021.5.8 研发组说READCOLUMN 通过SQL语句支持 */
			/* CUTSIZE */
			new Object[] {4, "select document from %s where number=10 and PARAMS='CUTSIZE=3'", "document", new String[] {"洛阳城"}}
			/* search.match.max 和 search.match.rate 和 TimeOut 和性能相关, 这里不验证了 */
		};
	}
	/**
	 * JDBC的检索参数功能<br>
	 * 2021.4.16 研发组列出了要求支持的参数, 其它参数可以不支持<br>
	 * <ul>
	 * 	<li>search.range.filter</li>
	 * 	<li>search.ideo.single</li>
	 *  <li>CUTSIZE</li>
	 *  <li>TimeOut</li>
	 * 	<li>virtualrule.express.prior</li>
	 * 	<li>search.match.max</li>
	 * 	<li>search.match.rate</li>
	 * 	<li>search.include.uncache</li>
	 * </ul>
	 * @param caseId
	 * @param sqlFormat
	 * @param checkingColumn
	 * @param expectations
	 */
	@Test(dataProvider = "searchParamsDataProvider")
	public void searchParams(int caseId, String sqlFormat, String checkingColumn, String[] expectations) {
		logger.debug(String.format("JdbcTest.searchParams, caseId=%d", caseId));
		String dbName = "system.searchParams_"+System.currentTimeMillis();
		LocalDateTime splitDateStart = LocalDateTime.now().plusDays(-7);
		LocalDateTime splitDateEnd = LocalDateTime.now().plusDays(7);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		String sql = String.format(sqlFormat, dbName);
		try {
			/* 建表 */
			logger.debug(String.format("createTestingFifoView(conn, %s, %s, %s)", dbName, splitDateStart.format(formatter), splitDateEnd.format(formatter)));
			createTestingFifoView(conn, dbName, splitDateStart.format(formatter), splitDateEnd.format(formatter));
			sleep();
			/* 插入记录 */
			List<TRSInputRecord> inputRecords = preparingTestData(splitDateStart, splitDateEnd, formatter);
			logger.debug(String.format("conn.executeInsert(%s, inputRecords(size=%d))", dbName, inputRecords.size()));
			conn.executeInsert(dbName, inputRecords);
			sleep();
			/* 检索 */
			logger.debug(String.format("resultSet = executeQueryUntilResultOrTimeOut(%s)", sql));
			ResultSet resultSet = executeQueryUntilResultOrTimeout(sql);
			/* 检查 */
			checkingResultSet(resultSet, checkingColumn, expectations);
		} catch (TRSException | ClassNotFoundException | SQLException e) {
			String failureMessage = Other.stackTraceToString(e);
			logger.error(failureMessage);
			fail(failureMessage);
		} finally {
			try {
				conn.deleteDatabase(dbName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 检查 virtualrule.express.prior, 需要特殊测试数据, 拆出来<br> 
	 * 注意中间的 AND 一定是大写
	 */
	@Test
	public void searchParamVirtualExpressPrior() {
		logger.debug(String.format("searchParamVirtualExpressPrior"));
		String dbName = "system.searchParams_"+System.currentTimeMillis();
		String sql = String.format("select * from %s where bit='(红颜 AND 花落)' and PARAMS='virtualrule.express.prior=true'", dbName);
		String[] phrases = new String[] {
			"一朝春尽红颜老",
			"一朝春尽红颜老,花落人亡两不知"
		};
		String[] documents = new String[] {
			"花落人亡两不知",
			""
		};
		TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_VIEW, TRSDatabase.DBPOLICY.FASTEST);
		try {
			db.addColumn(new TRSDatabaseColumn("parter", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("number", TRSDatabaseColumn.TYPE_NUMBER));
			db.addColumn(new TRSDatabaseColumn("char", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("date", TRSDatabaseColumn.TYPE_DATE));
			db.addColumn(new TRSDatabaseColumn("phrase", TRSDatabaseColumn.TYPE_PHRASE));
			db.addColumn(new TRSDatabaseColumn("document", TRSDatabaseColumn.TYPE_DOCUMENT));
			db.addColumn(new TRSDatabaseColumn("bit", TRSDatabaseColumn.TYPE_BIT).setProperty("index.virtual.columns", "phrase,document"));
			db.setParser(TRSDatabase.ANALYZER_TRSCJK);
			db.setParter("parter");
			db.setSplitter("number", "number", null);
			logger.debug(String.format("conn.createDatabase(%s)", dbName));
			conn.createDatabase(db);
			sleep();
			List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>();
			TRSInputRecord inputRecord = null;
			for(int i=0; i<phrases.length; i++) {
				inputRecord = new TRSInputRecord();
				inputRecord.addColumn("parter", "");
				inputRecord.addColumn("number", String.valueOf(i));
				inputRecord.addColumn("phrase", phrases[i]);
				inputRecord.addColumn("document", documents[i]);
				logger.debug(String.format("i=%d, phrase=%s, document=%s", i, phrases[i], documents[i]));
				inputRecords.add(inputRecord);
			}
			logger.debug(String.format("conn.executeInsert(%s, inputRecords(size=%d))", dbName, inputRecords.size()));
			conn.executeInsert(dbName, inputRecords);
			sleep();
			logger.debug(String.format("resultSet = executeQueryUntilResultOrTimeout(%s)", sql));
			ResultSet resultSet = executeQueryUntilResultOrTimeout(sql);
			checkingResultSet(resultSet, "number", new String[] {"1"});
		} catch(TRSException | ClassNotFoundException | SQLException e) {
			logger.error(Other.stackTraceToString(e));
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				conn.deleteDatabase(dbName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void sleep() {
		sleep(null);
	}
	
	private static void sleep(Long timeMillis) {
		long waitTime = 6000L;
		if(timeMillis != null)
			waitTime = timeMillis;
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建一个测试用的分时归档视图, 用于验证检索参数
	 * @param conn
	 * @param dbName
	 * @param splitDateStart
	 * @param splitDateEnd
	 * @return
	 * @throws TRSException
	 */
	private static boolean createTestingFifoView(
			TRSConnection conn, String dbName, String splitDateStart, String splitDateEnd) throws TRSException {
		TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_FIFO, TRSDatabase.DBPOLICY.NORMAL);
		db.addColumn(new TRSDatabaseColumn("parter", TRSDatabaseColumn.TYPE_CHAR).setAlias("分区"));
		db.addColumn(new TRSDatabaseColumn("char", TRSDatabaseColumn.TYPE_CHAR).setProperty("index.pinyin", "true").setProperty("index.pinyin.first", "true"));
		db.addColumn(new TRSDatabaseColumn("number", TRSDatabaseColumn.TYPE_NUMBER));
		db.addColumn(new TRSDatabaseColumn("date", TRSDatabaseColumn.TYPE_DATE).setAlias("日期"));
		db.addColumn(new TRSDatabaseColumn("phrase", TRSDatabaseColumn.TYPE_PHRASE).setProperty("index.pinyin", "true").setProperty("index.redund.ideo", "true").setAlias("短语"));
		db.addColumn(new TRSDatabaseColumn("document", TRSDatabaseColumn.TYPE_DOCUMENT).setProperty("index.pinyin", "true").setProperty("index.redund.ideo", "true").setAlias("正文"));
		db.addColumn(new TRSDatabaseColumn("bit", TRSDatabaseColumn.TYPE_BIT).setProperty("index.virtual.columns", "phrase,document"));
		db.setParser(TRSDatabase.ANALYZER_TRSSTD);
		db.setParter("parter");
		Map<String,String> splitMap = new HashMap<String,String>();
		splitMap.put("split.date.start", splitDateStart);
		splitMap.put("split.date.end", splitDateEnd);
		splitMap.put("split.date.level", "day");
		db.setSplitter("date", "date", splitMap);
		db.setProperty("open.subdb.num", "3");
		return conn.createDatabase(db);
	}
	/**
	 * 为分时归档视图准备测试记录
	 * @param startDateTime
	 * @param endDateTime
	 * @param dateTimeFormatter
	 * @return
	 * @throws TRSException
	 */
	private static List<TRSInputRecord> preparingTestData(
			LocalDateTime startDateTime, LocalDateTime endDateTime, DateTimeFormatter dateTimeFormatter) throws TRSException{
		List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>();
		TRSInputRecord inputRecord = null;
		Duration duration = Duration.between(startDateTime, endDateTime);
		long days = duration.toDays();
		LocalDateTime dateTime = startDateTime;
		for(int i=0; i<=days; i++) {
			inputRecord = new TRSInputRecord();
			inputRecord.addColumn("parter", "");
			inputRecord.addColumn("char", DOCUMENT[i]);
			inputRecord.addColumn("number", String.valueOf(i));
			inputRecord.addColumn("date", dateTime.plusDays(i).format(dateTimeFormatter));
			inputRecord.addColumn("phrase", DOCUMENT[i]);
			inputRecord.addColumn("document", DOCUMENT[i]);
			inputRecords.add(inputRecord);
		}
		return inputRecords;
	}
	/**
	 * 执行检索, 获取结果集或者直至超时
	 * @param sql
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static ResultSet executeQueryUntilResultOrTimeout(String sql) throws ClassNotFoundException, SQLException {
		Class.forName(targetClass);
		Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.105.190:10001/default", "admin", "trsadmin");
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		ResultSet sqlResultSet = preparedStatement.executeQuery();
		int loop = MAX_LOOP_SIZE;
		while(sqlResultSet == null && loop -- > 0) {
			sleep(WAITINGTIME_EACH_LOOP);
			sqlResultSet = preparedStatement.executeQuery();
			logger.debug(String.format("loop=%d, (sqlResultSet==null)? is %b", loop, sqlResultSet==null));
		}
		return sqlResultSet;
	}
	/**
	 * 逐条记录和预期对比
	 * @param resultSet
	 * @param expectations
	 * @throws SQLException
	 */
	private static void checkingResultSet(ResultSet resultSet, String checkingColumn, String[] expectations) throws SQLException {
		if(resultSet == null) {
			logger.error("Case failure, resultSet == null");
			fail("Case failure, resultSet == null");
		}
		int offset = 0;
		String actual = null;
		String expected = null;
		while(resultSet.next()) {
			logger.debug(getRecordStr(resultSet));
			actual = resultSet.getString(checkingColumn);
			expected = expectations[offset++];
			logger.debug(String.format("actual=%s, expected=%s", actual, expected));
			assertEquals(actual, expected);
		}
		assertEquals(offset, expectations.length);
	}
	/**
	 * 从结果集中提取出一条记录所有返回字段的值
	 * @param resultSet
	 * @return
	 * @throws SQLException
	 */
	private static String getRecordStr(ResultSet resultSet) throws SQLException {
		int columnCount = resultSet.getMetaData().getColumnCount();
		String columnName = null;
		StringBuilder result = new StringBuilder();
		result.append("Record [");
		for(int i=1; i<=columnCount; i++) {
			columnName = resultSet.getMetaData().getColumnName(i);
			result.append(columnName).append("=").append(resultSet.getString(columnName));
			if(i < columnCount) {
				result.append(", ");
			}
		}
		result.append("]");
		return result.toString();
	}
	
	private final static String[] DOCUMENT = new String[]{
			/* 0 */"南朝四百八十寺，多少楼台烟雨中",
			/* 1 */"十有九人堪白眼，百无一用是书生",
			/* 2 */"惨惨柴门风雪夜，此时有子不如无",
			/* 3 */"似此星辰非昨夜，为谁风露立中宵",
			/* 4 */"春风又绿江南岸，明月何时照我还",
			/* 5 */"渐行渐远渐无书，水阔鱼沉何处问",
			/* 6 */"人有悲欢离合，月有阴晴圆缺，此事古难全",
			/* 7 */"古今如梦，何曾梦觉",
			/* 8 */"人面不知何处去桃花依旧笑春风",
			/* 9 */"到得再相逢，恰经年离别",
			/* 10 */"洛阳城里春光好，洛阳才子他乡老",
			/* 11 */"往事已成空，还如一梦中",
			/* 12 */"正是江南好风景，落花时节又逢君",
			/* 13 */"独在异乡为异客，每逢佳节倍思亲",
			/* 14 */"洛阳亲友如相问，一片冰心在玉壶",
			/* 15 */"此情可待成追忆 只是当时已惘然",
			/* 16 */"曾经沧海难为水，除却巫山不是云",
			/* 17 */"相见时难别亦难，东风无力百花残",
			/* 18 */"酒债寻常行处有，人生七十古来稀",
			/* 19 */"浮云一别后,流水十年间",
			/* 20 */"最是人间留不住，朱颜辞镜花辞树",
			/* 21 */"孤舟蓑笠翁，独钓寒江雪",
			/* 22 */"夜来幽梦忽还乡，小轩窗，正梳妆",
			/* 23 */"枯藤老树昏鸦，小桥流水人家，古道西风瘦马",
			/* 24 */"天生我材必有用，千金散尽还复来",
			/* 25 */"物是人非事事休，欲语泪先流",
			/* 26 */"问君能有几多愁？恰似一江春水向东流",
			/* 27 */"我与春风皆过客，你携秋水揽星河",
			/* 28 */"渐行渐远渐无书，水阔鱼沉何处问",
			/* 29 */"只缘感君一回顾，使我思君朝与暮",
			/* 30 */"可怜无定河边骨，犹是春闺梦里人",
			/* 31 */"落霞与孤鹜齐飞，秋水共长天一色",
			/* 32 */"庭有枇杷树，吾妻死之年所手植也，今已亭亭如盖矣",
			/* 33 */"在天愿作比翼鸟，在地愿为连理枝",
			/* 34 */"愿君多采撷，此物最相思",
			/* 35 */"笑渐不闻声渐悄，多情却被无情恼"
		}; 
}
