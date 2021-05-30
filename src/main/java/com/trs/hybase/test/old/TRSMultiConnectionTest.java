package com.trs.hybase.test.old;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSFacetColumn;
import com.trs.hybase.client.TRSInputRecord;
import com.trs.hybase.client.TRSMultiConnection;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
import com.trs.hybase.test.util.ChronosOperator;

public class TRSMultiConnectionTest {

	/* 下边这个数组, 一个元素代表一个集群的入口 */
	private final static String[] HOSTS = new String[]{
		"http://192.168.101.238:5555",
		"http://192.168.101.232:5555"
	};
	/* 表名相同 */
	private final static String DATABASE_NAME = "system.multiConnection_"+System.currentTimeMillis();
	private TRSConnection[] conns = null;
	private TRSMultiConnection multiConn = null;
	private ConnectParams cp = null;
	private static Map<String,String> standardSplitMap;
	private static Map<String,String> dateSplitMap;
	private final static Logger LOGGER = Logger.getLogger(TRSMultiConnectionTest.class);
	/*
	 * type字段的值
	 */
	private final static String[] TYPES = new String[]{
		/* 0 */"b",
		/* 1 */"ab",
		/* 2 */"a;b;c",
		/* 3 */"abc",
		/* 4 */"bcd;e",
		/* 5 */"ab;c;d;e",
		/* 6 */"",/* 7 */"",/* 8 */"",/* 9 */"",/* 10 */"",
		/* 11 */"",/* 12 */"",/* 13 */"",/* 14 */"",/* 15 */"",
		/* 16 */"",/* 17 */"",/* 18 */"",/* 19 */"",/* 20 */"",
		/* 21 */"",/* 22 */"",/* 23 */"",/* 24 */"",/* 25 */"",
		/* 26 */"",/* 27 */"",/* 28 */"",/* 29 */"",/* 30 */"",
		/* 31 */"",/* 32 */"",/* 33 */"",/* 34 */""
	};
	/**
	 * 正文字段的值, 前边的编号对应记录id
	 */
	private final static String[] DOCUMENT = new String[]{
		/* 0 */"南朝四百八十寺，多少楼台烟雨中",
		/* 1 */"十有九人堪白眼，百无一用是书生",
		/* 2 */"惨惨柴门风雪夜，此时有子不如无",
		/* 3 */"似此星辰非昨夜，为谁风露立中宵",
		/* 4 */"春风又绿江南岸，明月何时照我还",
		/* 5 */"渐行渐远渐无书，水阔鱼沉何处问",
		/* 6 */"人有悲欢离合，月有阴晴圆缺，此事古难全",
		/* 7 */"古今如梦，何曾梦觉",
		/* 8 */"人面不知何处去 桃花依旧笑春风",
		/* 9 */"到得再相逢，恰经年离别",
		/* 10 */"洛阳城里春光好，洛阳才子他乡老",
		/* 11 */"往事已成空，还如一梦中",
		/* 12 */"正是江南好风景，落花时节又逢君",
		/* 13 */"独在异乡为异客，每逢佳节倍思亲",
		/* 14 */"浮云一别后,流水十年间",
		/* 15 */"此情可待成追忆 只是当时已惘然",
		/* 16 */"曾经沧海难为水，除却巫山不是云",
		/* 17 */"相见时难别亦难，东风无力百花残",
		/* 18 */"酒债寻常行处有，人生七十古来稀",
		/* 19 */"洛阳亲友如相问，一片冰心在玉壶",
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
	/**
	 * 分库字段类型枚举
	 * 0-DATE|1-NUMBER|2-CHAR, 和HYBASE中的字段类型一致
	 */
	private enum SplitColumnType{
		DATE(TRSDatabaseColumn.TYPE_DATE), NUMBER(TRSDatabaseColumn.TYPE_NUMBER), CHAR(TRSDatabaseColumn.TYPE_CHAR);
		
		private int typeId;
		private SplitColumnType(int typeId){
			this.typeId = typeId;
		}
		public int typeId(){
			return this.typeId;
		}
		/**
		 * 随机类型
		 * @return
		 */
		public static SplitColumnType random(){
			int random = (int)(Math.random() * 3);
			if(random == 0)
				return SplitColumnType.DATE;
			if(random == 1)
				return SplitColumnType.NUMBER;
			return SplitColumnType.CHAR;
		}
	}
	
	/**
	 * 在测试类执行开始前
	 * 1. 建立每个集群的连接
	 * 2. 初始化分库参数
	 * @throws TRSException
	 */
	@BeforeClass
	public void beforeClass() throws TRSException{
		PropertyConfigurator.configure("./log4j.properties");
		LOGGER.debug("TRSMultiConnectionTest.class");
		cp = new ConnectParams();
		conns = new TRSConnection[HOSTS.length];
		for(int i=0; i<HOSTS.length; i++)
			conns[i] = new TRSConnection(HOSTS[i], "admin", "trsadmin", cp);
		multiConn = new TRSMultiConnection(conns);
		initializeSplitMap();
		dataPrepared(conns);
		LOGGER.debug("Before class completed");
	}
	/**
	 * 初始化分库参数
	 */
	private void initializeSplitMap(){
		standardSplitMap = new HashMap<String,String>();
		standardSplitMap.put("max.split.num", "10");
		LocalDateTime today = LocalDateTime.now();
		dateSplitMap = new HashMap<String,String>();
		/* split.date.start = 今天的日期 - 14天 */
		dateSplitMap.put("split.date.start", today.minusDays(14).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
		/* split.date.end = 今天的日期 + 7天 */
		dateSplitMap.put("split.date.end", today.plusDays(7).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
		dateSplitMap.put("split.date.level", "day");
		LOGGER.debug("Initializing split map completed.");
	}
	
	@BeforeMethod
	public void beforeMethod(){}
	
	/**
	 * 每个测试方法执行开始前, 对每个集群
	 * 1.建表
	 * 2.数据入库
	 * @throws TRSException
	 */
	private static void dataPrepared(TRSConnection[] conns) throws TRSException{
		SplitColumnType splitColumnType = null;
		boolean containsDateTwo = false;
		for(int i=0; i<conns.length; i++){
			/* 第0, 2, 4, 6 ... 个集群创建的表会带有额外一个DATE类型字段, 名称为dat2, date2的字段值和dat的字段值完全一样*/
			containsDateTwo = (i%2 == 0);
			splitColumnType = SplitColumnType.random();
			createDatabase(conns[i], DATABASE_NAME, (int)(Math.random()*3), splitColumnType, containsDateTwo);
			insertRecords(conns[i], DATABASE_NAME, splitColumnType, containsDateTwo);
		}

	}
	/**
	 * 批量插入记录
	 * @param conn
	 * @param dbName
	 * @param splitColumnType
	 * @throws TRSException
	 */
	private static void insertRecords(TRSConnection conn, String dbName, SplitColumnType splitColumnType, boolean containsDateTwo) throws TRSException{
		List<TRSInputRecord> inputRecordList = new ArrayList<TRSInputRecord>();
		LocalDateTime start = LocalDateTime.now().minusDays(21);
		LocalDateTime end = LocalDateTime.now().plusDays(14);
		int days = (int)ChronosOperator.timeDifference(start, end).toDays();
		LOGGER.debug("start=="+start.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))+
				", end=="+end.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))+
				", days/recordNumber=="+days
				);
		TRSInputRecord inputRecord = null;
		for(int i=0; i<days; i++){
			inputRecord = new TRSInputRecord();
			inputRecord.addColumn("id", i);
			inputRecord.addColumn("part", String.valueOf(i));
			inputRecord.addColumn("type", TYPES[i]);
			if(splitColumnType == SplitColumnType.DATE)
				inputRecord.addColumn("split", start.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
			else
				inputRecord.addColumn("split", String.valueOf(i));
			inputRecord.addColumn("dat", start.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
			inputRecord.addColumn("dat2", start.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
			inputRecord.addColumn("phr", DOCUMENT[i]);
			inputRecord.addColumn("doc", DOCUMENT[i]);
			inputRecordList.add(inputRecord);
		}
		LOGGER.debug(String.format("[%s].executeInsert(%s, inputRecordList(size=%d))", 
				conn.getURL(), dbName, inputRecordList.size()));
		conn.executeInsert(dbName, inputRecordList);
		try {
			Thread.sleep(9000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建数据库
	 * @param conn
	 * @param dbName
	 * @param dbType
	 * @param splitColumnType
	 * @throws TRSException
	 */
	private static void createDatabase(TRSConnection conn, String dbName, int dbType, SplitColumnType splitColumnType, boolean containDateTwo) throws TRSException{
		TRSDatabase db = new TRSDatabase(dbName, 1, TRSDatabase.DBPOLICY.FASTEST);
		db.addColumn(new TRSDatabaseColumn("id", TRSDatabaseColumn.TYPE_NUMBER).setCategoryColumn(true));
		db.addColumn(new TRSDatabaseColumn("type", TRSDatabaseColumn.TYPE_CHAR).setMultivalue(true).setCategoryColumn(true));
		db.addColumn(new TRSDatabaseColumn("part", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("split", splitColumnType.typeId()));
		db.addColumn(new TRSDatabaseColumn("dat", TRSDatabaseColumn.TYPE_DATE).setCategoryColumn(true));
		if(containDateTwo)
			db.addColumn(new TRSDatabaseColumn("dat2", TRSDatabaseColumn.TYPE_DATE).setCategoryColumn(true));
		db.addColumn(new TRSDatabaseColumn("phr", TRSDatabaseColumn.TYPE_PHRASE));
		db.addColumn(new TRSDatabaseColumn("doc", TRSDatabaseColumn.TYPE_DOCUMENT));
		db.addColumn(new TRSDatabaseColumn("bit", TRSDatabaseColumn.TYPE_BIT));
		db.setParter("part");
		switch(splitColumnType){
			case DATE: db.setSplitter("date", "split", dateSplitMap); break;
			case NUMBER: db.setSplitter("number", "split", standardSplitMap); break;
			case CHAR: db.setSplitter("string", "split", standardSplitMap); break;
		}
		db.setParser(TRSDatabase.ANALYZER_TRSCJK);
		boolean success = conn.createDatabase(db);
		LOGGER.debug(conn.getURL()+".createDatabase("+dbName+") == "+success+", containsDateTwo=="+containDateTwo);
	}
	
	@AfterMethod
	public void afterMethod(){}
	
	@AfterClass
	public void afterClass(){
		for(int i=0; i<conns.length; i++){
			try{
				conns[i].deleteDatabase(DATABASE_NAME);
				LOGGER.debug(conns[i].getURL()+" -> deleteDatabase("+DATABASE_NAME+");");
			}catch(TRSException e){
				e.printStackTrace();
			}
		}
		for(int i=0; i<conns.length; i++)
			conns[i].close();
		multiConn.close();
	}
	
	@DataProvider(name="executeSelectDataProvider")
	public Object[][] executeSelectDataProvider(Method method){
		Object[][] result = null;
		if("executeSelect".equals(method.getName()) || "executeSelectNoSort".equals(method.getName())){
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
			result = new Object[][]{
					/* 检索公共日期字段, 检索一条记录, 因为入库记录是一样的, 因此返回2条*/
					new Object[]{0, "dat:"+LocalDateTime.now().format(dtf), 0, 10, new String[]{"21", "21"}},
					/* 检索公共日期字段, 范围检索 dat[ 昨天 TO 明天 ], 这样双表各返回3条记录, 本来应该返回6条, 但是我设置结果集返回4条, 因此第二个集群的今天和明天的记录会被裁剪*/
					new Object[]{1, "dat:[ "+LocalDateTime.now().minusDays(1).format(dtf)+" TO "+LocalDateTime.now().plusDays(1).format(dtf)+" ]", 0, 4, new String[]{"20", "21", "22", "20"}},
					/* 检索非公共的日期字段dat2, 返回检索 dat[ 昨天 TO 明天], 第一个集群的表没有这个字段, 只有第二个集群的表会返回3条记录 */
					new Object[]{2, "dat2:["+LocalDateTime.now().minusDays(1).format(dtf)+" TO "+LocalDateTime.now().plusDays(1).format(dtf)+" ]", 0, 4, new String[]{"20", "21", "22"}},
					/* 多字段组合检索*/
					new Object[]{3, "part:[19 TO 23] AND dat:[ "+LocalDateTime.now().minusDays(2).format(dtf)+" TO "+LocalDateTime.now().plusDays(2).format(dtf)+" ]", 0, 10, 
							new String[]{"19","20","21","22","23","19","20","21","22","23"}},
					/* 多字段组合检索, 既含有公共字段, 又含有独有字段, 中间采用OR相连, 每个集群都能返回记录*/
					new Object[]{4, "part:[20 TO 22] OR dat2:["+LocalDateTime.now().minusDays(2).format(dtf)+" TO "+LocalDateTime.now().plusDays(2).format(dtf)+" ]", 0, 10, 
							new String[]{/* 第一个集群命中 */"19","20","21","22","23",/* 第二个集群命中 */"20","21","22"}},
					/* 默认检索字段 */
					new Object[]{5, "江南", 0, 10, new String[]{"4", "12", "4", "12"}},
					/* 距离查询 */
					new Object[]{6, "#POS:\"春风 明月\"~6", 0, 10, new String[]{"4", "4"}},
					/* 位置查询 */
					new Object[]{7, "phr#PRE:\"春光 洛阳\"~2", 0, 4, new String[]{"10", "10"}},
					/* 简单模糊查询 */
					new Object[]{8, "type:a*", 0, 10, new String[]{"1","2","3","5","1","2","3","5"}},
					/* 多值字段查询 */
					new Object[]{9, "type:c", 0, 10, new String[]{"2","5","2","5"}},
					/* NOT */
					new Object[]{10, "春风 NOT 明月", 0, 6, new String[]{"8", "27", "8", "27"}},
					/* 精确查询 */
					new Object[]{11, "\"古今如梦，何曾梦觉\"", 0, 2, new String[]{"7", "7"}},
					/* LIKE */
					new Object[]{12, "#LIKE:\"谁说书生百无一用\"~3", 0, 2, new String[]{"1", "1"}},
					
					/* 应用层翻页验证: 结果集返回并合并以后,再做翻页; 结果集在返回之前不翻页 */
					/* 注: 不排序检索(executeSelectNoSort), 本质为导出, 不能要求返回的结果集和预期一致, 但结果集大小必须符合预期 */
					
					/* 结果集1 (id从5到16)                        结果集2 (id从5到16)
					 * ↓                                          ↓
					 * 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 
					 *                                     ↑            ↑
					 *                                     |-返回结果集- |									
					 */
					new Object[]{13, "id:[5 TO 16]", 10, 5, new String[]{"15", "16", "5", "6", "7"}},
			};
		}
		return result;
	}
	/**
	 * 不限制长度的检索
	 * @param caseId
	 * @param searchExpression
	 * @param num
	 * @param expectation
	 */
	@Test(dataProvider="executeSelectDataProvider")
	public void executeSelectNoSort(int caseId, String searchExpression, long start, long num, String[] expectation){
		LOGGER.debug("exeuteSelectNoSort(), caseId=="+caseId);		
		/*不能排序, 也就无法检查记录的顺序*/
		SearchParams sp = new SearchParams();
		try{
			LOGGER.debug("("+multiConn.getURL()+").executeSelectNoSort("+DATABASE_NAME+", "+searchExpression+", "+start+", "+num+", sp);");
			TRSResultSet multiResultSet = multiConn.executeSelectNoSort(DATABASE_NAME, searchExpression, start, num, sp);
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			LOGGER.debug("multiResultSet.size()=="+multiResultSet.size());
			/* noSort接口, size()方法无效, getNumFound()方法可能会涉及到比结果集大的情况(被用户裁剪), 不能用于检查*/
			/* 手工设置变量记录结果集大小 */
			int resultSetSize = 0;
			TRSRecord record = null;
			while(multiResultSet.moveNext()){
				resultSetSize++;
				record = multiResultSet.get();
				LOGGER.debug(recordStr(record));
			}
			LOGGER.debug(String.format("actual result set size=%d, expectation=%d", resultSetSize, expectation.length));
			Assert.assertEquals(resultSetSize, expectation.length);
		}catch(TRSException e){
			Assert.fail(e.toString());
		}
	}
	
	@DataProvider(name="executeSelectWithOptionsDataProvider")
	public Object[][] executeSelectWithOptionsDataProvider(Method method){
		Object[][] result = null;
		if(!"executeSelectWithOptions".equals(method.getName()))
			return result;
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		result = new Object[][]{
			/* 多个集群同时检索, 前面的集群检索结果数量不符合要求, 必需检索后续集群, 也看不出同步的效果 */
			new Object[]{0, DATABASE_NAME, "dat:[ "+now.minusDays(1).format(yyyyMMdd)+" TO "+now.plusDays(1).format(yyyyMMdd)+" ]", 0, 4, true, true, 4, -1},
			/* 数据库不存在时抛出异常 */
			new Object[]{1, DATABASE_NAME+";system.xxxx", "id:[5 TO 20]", 10, 10, true, false, 0, 9918025},
			/* 多个集群同时检索, 前面的集群检索数量足够, 跳过后续集群。看不出来跳过后续集群这个动作。 */
			new Object[]{2, DATABASE_NAME, "dat:["+now.minusDays(5).format(yyyyMMdd)+" TO "+now.plusDays(5).format(yyyyMMdd)+" ] ", 0, 10, true, false, 10, -1},
			/* 数据库不存在时跳过异常, 就当没有这个数据库; 同时附加应用层翻页验证
			 * 
			 * 两个库分别使用 id:[10 TO 25] ，+id排序，检索的综合结果是id为以下的记录：
			 * 
			 * |----------------集群1-----------------------|  |--------------------集群2--------------------|
			 * ↓                                            ↓  ↓                                            ↓
			 * 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25
			 * 								 ↑                                                        ↑
			 *                               |------------------------结果集--------------------------|           
			 *                                                                                                 
			 * 设置了从第10条开始裁剪记录，共截取20条记录，所以最终返回的结果是：
			 * 20 21 22 23 24 25 10 11 12 13 14 15 16 17 18 19 20 21 22 23
			 * 
			 * */
			new Object[]{3, DATABASE_NAME+"", "id:[10 TO 25]", 10, 20, true, true, 20, -1}
			
			/* multiconnSearchAsyn = false 的情况, 也就是顺序检索的情况见 executeSelect() */
		};
		return result;
	}
	
	/**
	 * 带options参数的集群检索 
	 * @param caseId
	 * @param dbName
	 * @param searchExpression
	 * @param start
	 * @param num
	 * @param multiconnSearchAsyn multiconn.search.asyn 为true时，多个集群同时检索；默认false，按顺序检索
	 * @param multiconnSkipNotexistdb 默认为true,表示跳过数据库不存在的异常
	 * @param expectHitNum
	 * @param expectErrorCode
	 */
	@Test(dataProvider="executeSelectWithOptionsDataProvider")
	public void executeSelectWithOptions(int caseId, 
										  	 String dbName, 
										  	 String searchExpression, 
										  	 long start, 
										  	 long num, 
										  	 boolean multiconnSearchAsyn, 
										  	 boolean multiconnSkipNotexistdb, 
										  	 int expectHitNum,
										  	 int expectErrorCode){
		LOGGER.debug("executeSelectWithOptions(), caseId=="+caseId);
		SearchParams sp = new SearchParams();
		sp.setProperty("multiconn.search.asyn", String.valueOf(multiconnSearchAsyn));
		sp.setProperty("multiconn.skip.notexistdb", String.valueOf(multiconnSkipNotexistdb));
		sp.setSortMethod("+id");
		LOGGER.debug("multiconn.search.asyn="+sp.getProperty("multiconn.search.asyn")+", multiconn.skip.notexistdb="+sp.getProperty("multiconn.skip.notexistdb"));
		try{
			LOGGER.debug("("+multiConn.getURL()+").executeSelect("+dbName+", "+searchExpression+", "+start+", "+num+", sp);");
			TRSResultSet resultSet = multiConn.executeSelect(dbName, searchExpression, start, num, sp);
			LOGGER.debug("resultSet.size()=="+resultSet.size()+", expectHitNum=="+expectHitNum);
			TRSRecord record = null;
			for(int i=0; i<resultSet.size(); i++){
				resultSet.moveNext();
				record = resultSet.get();
				LOGGER.debug(recordStr(record));
			}
			Assert.assertEquals(expectHitNum, resultSet.size());
		}catch(TRSException e){
			LOGGER.debug(String.format("errorCode=%d, errorString=%s%s%s", 
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), com.trs.hybase.test.util.Other.stackTraceToString(e)));
			/* 遇到不存在的数据库在这里检查异常, 和预期的异常号一致就return */
			if(!multiconnSkipNotexistdb){
				Assert.assertEquals(e.getErrorCode(),expectErrorCode);
				return;
			}
			Assert.fail(e.toString());
		}
	}
	
	/**
	 * 多集群检索, 带有字段排序
	 * @param caseId
	 * @param searchExpression
	 * @param num
	 * @param expectation
	 */
	@Test(dataProvider="executeSelectDataProvider")
	public void executeSelect(int caseId, String searchExpression, long start, long num, String[] expectation){
//		if(caseId != 13 ){	return;	}
		LOGGER.debug("executeSelect(), caseId=="+caseId);
		SearchParams sp = new SearchParams();
		/*不排序没法检查*/
		sp.setSortMethod("+id");
		LOGGER.debug("sp.setSortMethod(\"+id\")");
		try {
			LOGGER.debug("("+multiConn.getURL()+").executeSelect("+DATABASE_NAME+", "+searchExpression+", "+start+", "+num+", sp);");
			TRSResultSet multiResultSet = multiConn.executeSelect(DATABASE_NAME, searchExpression, start, num, sp);
			TRSRecord record = null;
			LOGGER.debug("multiResultSet.size()=="+multiResultSet.size()+", expectation.length=="+expectation.length);
			Assert.assertEquals(expectation.length, multiResultSet.size());
			for(int i=0; i<multiResultSet.size(); i++){
				multiResultSet.moveNext();
				record = multiResultSet.get();
				LOGGER.debug(recordStr(record));
				LOGGER.debug("expectation["+i+"]=="+expectation[i]+", record.getString(\"id\")=="+record.getString("id"));
				Assert.assertEquals(expectation[i], record.getString("id"));
			}
		} catch (TRSException e) {
			Assert.fail(e.toString());
		}
	}
	
	@DataProvider(name="categoryQueryDataProvider")
	public Object[][] categoryQueryDataProvider(Method method){
		Object[][] result = null;
		if(!"categoryQuery".equals(method.getName()))
			return result;
		LocalDateTime today = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
		DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		
		Map<String,Long> caseOne = new HashMap<String,Long>();
		caseOne.put(today.format(dtf2), 2L);
		Map<String,Long> caseTwo = new HashMap<String, Long>();
		caseTwo.put("21", 2L); caseTwo.put("20", 2L); caseTwo.put("22", 2L);
		Map<String,Long> caseThree = new HashMap<String,Long>();
		caseThree.put("20", 1L); caseThree.put("21", 1L); caseThree.put("22", 1L);
		Map<String,Long> caseFour = new HashMap<String,Long>();
		caseFour.put("b", 4L); caseFour.put("ab", 4L); caseFour.put("c", 4L); caseFour.put("e", 4L);
		Map<String,Long> caseFive = new HashMap<String,Long>();
		caseFive.put("1", 2L); caseFive.put("2", 2L); caseFive.put("5", 2L);
		
		result = new Object[][]{
				new Object[]{0, "dat:"+today.format(dtf), "dat", 10, caseOne},
				new Object[]{1, "dat:["+today.minusDays(1).format(dtf)+" TO "+today.plusDays(1).format(dtf)+"]", "id", 6, caseTwo},
				new Object[]{2, "dat2:["+today.minusDays(1).format(dtf)+" TO "+today.plusDays(1).format(dtf)+"]", "id", 6, caseThree},
				new Object[]{3, "*:*", "type", 4, caseFour},
				new Object[]{4, "type:a$", "id", 10, caseFive}
		};
		return result;
	}
	
	/**
	 * 分类统计
	 * @param caseId
	 * @param searchExpression
	 * @param categoryColumn
	 * @param topNum
	 * @param expectation
	 */
	@Test(dataProvider="categoryQueryDataProvider")
	public void categoryQuery(int caseId, String searchExpression, String categoryColumn, long topNum, Map<String,Long> expectation){
		LOGGER.debug("categoryQuery(), caseId=="+caseId);
		try {
			LOGGER.debug("("+multiConn.getURL()+").categoryQuery("+DATABASE_NAME+"), "+searchExpression+", \"doc\", "+categoryColumn+", "+topNum+");");
			TRSResultSet multiResultSet = multiConn.categoryQuery(DATABASE_NAME, searchExpression, "doc", categoryColumn, topNum);
			LOGGER.debug("multiResultSet.getCategoryMap().size()=="+multiResultSet.getCategoryMap().size()+", expectation.size()=="+expectation.size());
			for(Entry<String, Long> e: multiResultSet.getCategoryMap().entrySet())
				System.out.println(e.getKey()+" : "+e.getValue());
			Assert.assertEquals(expectation, multiResultSet.getCategoryMap());
		} catch (TRSException e) {
			Assert.fail(e.toString());
		}
	}
	@DataProvider(name="facetQueryDataProvider")
	public Object[][] facetQueryDataProvider(Method method){
		Object[][] result = null;
		if(!"facetQuery".equals(method.getName()))
			return result;
		/* 第一条用例的预期…… */
		Map<String, Map<String, Double>> caseOne = new HashMap<String, Map<String, Double>>();
		Map<String, Double> firstType = new HashMap<String, Double>();
		firstType.put("1", 2.0); firstType.put("5", 2.0);
		caseOne.put("ab", firstType);
		Map<String, Double> secondType = new HashMap<String, Double>();
		secondType.put("0", 2.0); secondType.put("2", 2.0);
		caseOne.put("b", secondType);
		Map<String, Double> thirdType = new HashMap<String, Double>();
		thirdType.put("2", 2.0); thirdType.put("5", 2.0);
		caseOne.put("c", thirdType);
		Map<String, Double> fourthType = new HashMap<String, Double>();
		fourthType.put("4", 2.0); fourthType.put("5", 2.0);
		caseOne.put("e", fourthType);
		Map<String, Double> fifthType = new HashMap<String, Double>();
		fifthType.put("2", 2.0);
		caseOne.put("a", fifthType);
		Map<String,Double> sixthType = new HashMap<String,Double>();
		sixthType.put("4", 2.0);
		caseOne.put("bcd", sixthType);
		Map<String, Double> seventhType = new HashMap<String, Double>();
		seventhType.put("3", 2.0);
		caseOne.put("abc", seventhType);
		Map<String, Double> eighthType = new HashMap<String, Double>();
		eighthType.put("5", 2.0);
		caseOne.put("d", eighthType);
		
		result = new Object[][]{
			new Object[]{0, "*:*", "type", "id", "count", caseOne},
		};
		return result;
	}
	
	@Test(dataProvider="facetQueryDataProvider")
	public void facetQuery(int caseId, String searchExpression, String firstLayerColumn, String secondLayerColumn, String statisticFunction, Map<String,Map<String, Long>> expectation){
		LOGGER.debug("facetQuery, caseId=="+caseId);
		TRSFacetColumn facet1 = new TRSFacetColumn(firstLayerColumn, firstLayerColumn, statisticFunction, "", 100, null);
		TRSFacetColumn facet2 = new TRSFacetColumn(secondLayerColumn, secondLayerColumn, statisticFunction, "", 100, null);
		List<TRSFacetColumn> facetColList = new ArrayList<TRSFacetColumn>();
		facetColList.add(facet1);
		facetColList.add(facet2);
		Map<String, Map<String,Double>> finalMap = new HashMap<String, Map<String, Double>>();
		try {
			TRSResultSet resultSet = multiConn.facetQuery(DATABASE_NAME, searchExpression, null, facetColList, 10, new SearchParams());
			List<String> prevFacetValue = new ArrayList<String>();
			Map<String, Double> firstLayerMap = resultSet.getFacetInfo(prevFacetValue, firstLayerColumn);
			Map<String, Double> facetInfo = null;
		    for(String firstLayerKey : firstLayerMap.keySet()){
		    	prevFacetValue.add(firstLayerKey);
		    	facetInfo = resultSet.getFacetInfo(prevFacetValue, secondLayerColumn);
		    	finalMap.put(firstLayerKey, facetInfo);
		    	LOGGER.debug(firstLayerKey+" -> actual: "+facetInfo+", expectation=="+expectation.get(firstLayerKey));
		    	Assert.assertEquals(facetInfo, expectation.get(firstLayerKey));
		    	prevFacetValue.remove(firstLayerKey);
		    }
		} catch (TRSException e) {
			Assert.fail(e.toString());
		}
	}
	
	/**
	 * 将 TRSRecord 的各个字段值展开为 String 类型
	 * @param record
	 * @return
	 * @throws TRSException
	 */
	private static String recordStr(TRSRecord record) throws TRSException {
		StringBuilder sb = new StringBuilder();
		String[] columnNames = record.getColumnNames();
		sb.append("Record [");
		for(int i=0; i<columnNames.length; i++) {
			sb.append(columnNames[i]).append("=").append(record.getString(columnNames[i]));
			if(i < columnNames.length - 1)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}
}
