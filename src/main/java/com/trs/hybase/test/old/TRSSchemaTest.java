package com.trs.hybase.test.old;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.trs.hybase.client.APIVersion;
import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabase.DBPOLICY;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSInputRecord;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.TRSSchema;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
import com.trs.hybase.test.util.Other;
import com.trs.hybase.test.util.StringOperator;

public class TRSSchemaTest {
	private final static Logger LOGGER = Logger.getLogger(TRSSchemaTest.class);
	private TRSConnection conn;
	private TRSPermissionClient trspermission = null;
	
	/* 配置文件里获取一个host即可, 但模式管理需要多个, 暂时没想好怎么改, 先给个定值 */
	private final static String[] SERVERS = new String[] {
			"192.168.105.190:5555",
			"192.168.105.191:5555",
			"192.168.105.192:5555"
			/* 注意, 有些测试方法需要手工主副分区表, 用例设计时认为集群最大3个节点, 没有提供新的节点的分区表
			 * 这里如果任意扩容, 会导致这些方法failure */
	};
	
	private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	
	private void sleep() {
		sleep(null);
	}
	private void sleep(Long waitTime) {
		if (waitTime == null)
			waitTime = 6000L;
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@BeforeMethod
	public void befordMethod() {
		ConnectParams cp = new ConnectParams();
		conn = new TRSConnection("http://" + SERVERS[0], "admin", "trsadmin", cp);
		trspermission = new TRSPermissionClient(conn);
	}
	
	@AfterMethod
	public void afterMethod() {
		if(conn != null)
			conn.close();
		if(trspermission != null)
			trspermission.close();
	}
	
	@BeforeClass
	public void beforeClass() {
		PropertyConfigurator.configure("./log4j.properties");
		LOGGER.debug("TRSSchemaTest, APIVersion="+APIVersion.getVersion());
	}
	/**
	 * String 类型参数, array 转 list
	 * @param array
	 * @return
	 */
	private static List<String> arrayToList(String[] array){
		if(array == null || array.length == 0)
			return new ArrayList<String>(0);
		List<String> list = new ArrayList<String>(array.length);
		for(String element : array)
			list.add(element);
		return list;
	}
	/**
	 * String 类型参数, array 转 string(间隔符为英文半角分号)
	 * @param array
	 * @return
	 */
	private static String arrayToString(String[] array) {
		StringBuilder sb = new StringBuilder();
		for(String element : array)
			sb.append(element).append(";");
		return sb.toString();
	}
	/**
	 * String 类型参数, array 转 set(顺便排重)
	 * @param array
	 * @return
	 */
	private static Set<String> arrayToSet(String[] array){
		Set<String> set = new HashSet<String>();
		if(array == null || array.length == 0)
			return set;
		for(String element : array)
			set.add(element);
		return set;
	}
	
	@DataProvider(name = "createSchemaDataProvider")
	public Object[][] createSchemaDataProvider(Method method){
		if(!"createSchema".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 模式名 standard, 分区数6(节点数2倍), 服务器为全部 */
			new Object[] {1, "standard_"+System.currentTimeMillis(), 6, SERVERS},
			/* 模式名 second, 分区数12(节点数4倍), 服务器只有两台 */
			new Object[] {2, "second_"+System.currentTimeMillis(), 12, new String[] {SERVERS[0], SERVERS[1]}},
			/* 模式名 min, 分区数5(最小分区数), 服务器只有一台 */
			new Object[] {3, "min_"+System.currentTimeMillis(), 5, new String[] {SERVERS[2]}},
			/* 模式名 max, 分区数1000(最大分区数), 服务器为全部 */
			new Object[] {4, "max_"+System.currentTimeMillis(), 1000, SERVERS},
			/* 分区名为数字 */
			new Object[] {5, "number_"+System.currentTimeMillis(), 12, SERVERS},
			/* 分区名为带下划线 */
			new Object[] {6, "_314159_"+System.currentTimeMillis(), 5, SERVERS},
			/* 分区名长度31 */
			new Object[] {7, "abcdefghijklmnopqrstuvwxyzabcde", 5, SERVERS}
		};
	}
	/**
	 * 创建模式测试, 正常情况<br/>
	 * @param caseId
	 * @param schemaName
	 * @param partNumber
	 * @param nodes
	 */
	@Test(dataProvider = "createSchemaDataProvider")
	public void createSchema(int caseId, String schemaName, int partNumber, String[] nodes) {
		LOGGER.debug(String.format("createSchema, caseId=%d", caseId));
		/* 创建模式后, 新建的表名 */
		String globalDatabaseName = schemaName + ".demo";
		List<String> nodesList = arrayToList(nodes);
		String nodesListStr = arrayToString(nodes);
		try {
			/* 创建模式 */
			LOGGER.debug(String.format("createSchema(%s, %d, %s)", schemaName, partNumber, nodesList.toString()));
			/* API提供了两种创建模式的方法, 一种是提交字符串, 一种是提交List, 为了都照顾到, 测试时随机选一种 */
			double rate = Math.random();
			if(rate < 0.5)
				trspermission.createSchema(schemaName, partNumber, nodesList);
			else
				trspermission.createSchema(schemaName, partNumber, nodesListStr);
			sleep();
			/* 获取模式 */
			TRSSchema schema = trspermission.getSchema(schemaName);
			Set<String> nodeSet = new HashSet<String>();
			/* 返回了hashSet, 和自行构造的HashSet做差, 得到空集合 */
			nodeSet.removeAll(schema.getNodeList());
			assertEquals(nodeSet.size(), 0);
			/* 断言模式名称和分区数 */
			LOGGER.debug(String.format("sechma.getName=%s, expected=%s", schema.getName(), schemaName));
			assertEquals(schema.getName(), schemaName);
			LOGGER.debug(String.format("sechma.getPartnum=%d, expected=%d", schema.getPartnum(), partNumber));
			assertEquals(schema.getPartnum(), partNumber);
			/* 新建的模式一定是可读可写 */
			LOGGER.debug("schema.isReadwirte="+schema.isReadwrite());
			assertTrue(schema.isReadwrite());
			/* 建表 */
			LOGGER.debug(String.format("createDemoView(conn, %s, 1, DBPOLICY.FAST);", globalDatabaseName));
			boolean createDatabaseSucceed = createDemoDatabase(conn, globalDatabaseName, 1, DBPOLICY.FASTEST);
			assertTrue(createDatabaseSucceed);
			/* 记录入库 */
			LOGGER.debug(String.format("loadRecords(%s, %s, 0);", globalDatabaseName, "." + "/TRSSchema/demo.trs"));
			long loadNumber = conn.loadRecords(globalDatabaseName, "." + "/TRSSchema/demo.trs", 0);
			LOGGER.debug("loadNumber="+loadNumber);
			assertEquals(loadNumber, 5079);
			sleep();
			/* 检索, 只断言结果集大小即可, 检索不是这里的重点 */
			LOGGER.debug(String.format("executeSelect(%s, rowid:*, 0, 10, new SearchParams());", globalDatabaseName));
			TRSResultSet resultSet = conn.executeSelect(globalDatabaseName, "rowid:*", 0, 10, new SearchParams());
			LOGGER.debug("resultSet.size="+resultSet.size());
			assertEquals(resultSet.size(), 10);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			/* 注意先删表, 再删模式, 一定要两个try-catch, 删表如果异常不影响删除模式*/
			try {
				conn.deleteDatabase(globalDatabaseName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			sleep();
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "createSchemaBoundaryDataProvider")
	public Object[][] createSchemaBoundaryDataProvider(Method method){
		if(!"createSchemaBoundary".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 节点列表提供一张空表, 管理台视为选中全部节点, 接口抛出异常 */
			new Object[] {1, "emptyNode", 6, new String[0], -1, "", -1, new String[0]},
			/* 分数区设置比5小, 自动修正为5, 预期的ErrorCode写成了 Integer.MIN_VALUE, 是想表明这个预期不生效 */
			new Object[] {2, "low", 4, SERVERS, Integer.MIN_VALUE, "low", 5, SERVERS},
			/* 分区数不大于0, 接口会抛出异常, 注意管理台会自动修正为5  */
			new Object[] {3, "zero", 0, SERVERS, -1, "zero", 5, SERVERS},
			new Object[] {4, "minus", -1, SERVERS, -1, "minus", 5, SERVERS},
			/* 分区数设置为1000大 */
			new Object[] {5, "over", 1001, SERVERS, Integer.MIN_VALUE, "over", 1000, SERVERS},
			/* 分区名为空*/
			new Object[] {6, "", 6, SERVERS, -1, "", -1, null},
			/* 分区名汉字 */
			new Object[] {7, "汉字", 5, SERVERS, 9920034, "", -1, null},
			/* 分区名为符号字符 */
			new Object[] {9, "!@#$%^&*()", 5, SERVERS, 9920034, "", -1, null},
			/* 节点列表包含了集群中不存在的节点 */
			new Object[] {10, "outer", 5, new String[] {SERVERS[0], SERVERS[1], SERVERS[2], "192.168.192.168:5555"},
					9920500, "", -1, null},
			/* 模式名仅含冒号 */
			new Object[] {12, "my:schema", 6, SERVERS, 9920034, "", -1, null},
			/* 模式名中含有英文句号 */
			/* 2021-1-7: 模式带英文句号仍然允许创建, 但是不能创建任何表, 没有任何存在意义, 要求开发组统一 */
			new Object[] {11, "my.schema", 6, SERVERS, 9920034, "", -1, null},
			/* 创建system模式 */
			new Object[] {12, "system", 6, SERVERS, 9920034, "", -1, null},
			/* 模式名超过31位  */
			new Object[] {13, "abcdefghijkl,mopqrstuvwxyzabcdef", 5, SERVERS, 9920034, "", -1, null}
		};
	}
	/**
	 * 创建模式, 边界情况验证
	 * @param caseId
	 * @param schemaName
	 * @param partNumber
	 * @param nodes
	 * @param errorCode
	 * @param expectedName
	 * @param expectedNumber
	 * @param expectedNodes
	 */
	@Test(dataProvider = "createSchemaBoundaryDataProvider")
	public void createSchemaBoundary(int caseId, String schemaName, int partNumber, String[] nodes, 
			int errorCode, String expectedName, int expectedNumber, String[] expectedNodes) {
		LOGGER.debug("createSchemaBoundary, caseId="+caseId);
		List<String> nodesList = arrayToList(nodes);
		try {
			try {
				trspermission.createSchema(schemaName, partNumber, nodesList);
				if(schemaName.contains("."))
					fail(String.format("模式名:%s, 含有英文句号, 不应当被创建, 但未能抛出异常", schemaName));
			}catch(TRSException e) {
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=%d, e.getErrorString=%s", 
						e.getErrorCode(), errorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), errorCode);
				return;
			}
			sleep();
			TRSSchema schema = trspermission.getSchema(schemaName);
			assertEquals(schema.getName(), expectedName);
			assertEquals(schema.getPartnum(), expectedNumber);
			Set<String> expectedNodeSet = new HashSet<String>();
			expectedNodeSet.addAll(arrayToList(expectedNodes));
			expectedNodeSet.removeAll(schema.getNodeList());
			assertTrue(expectedNodeSet.isEmpty());
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {}
		}
	}
	/**
	 * 创建同名模式抛出异常
	 */
	@Test
	public void schemaDuplicate() {
		String schemaName = "duplicate";
		try {
			/* 第一次创建预期成功 */
			trspermission.createSchema(schemaName, 5, arrayToList(SERVERS));
			/* 第二次创建预期失败, try块最后一行必须是fail */
			try {
				trspermission.createSchema(schemaName, 5, arrayToList(SERVERS));
				fail(String.format("模式[%s]被创建了2次, 未能抛出异常", schemaName));
			}catch(TRSException e) {
				/* 只要能抛出异常就可以了 */
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=9917202, e.getErrorString=%s", 
						e.getErrorCode(), e.getErrorString()));
				assertEquals(e.getErrorCode(), 9917202);
			}
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider(name = "addNodeDataProvider")
	public Object[][] addNodeDataProvider(Method method){
		if(!"addNode".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 正常情况 */
			new Object[] {1, new String[] {SERVERS[0]}, SERVERS[1], true, Integer.MIN_VALUE, new String[] {SERVERS[0], SERVERS[1]}},
			/* 异常情况, 添加一个错误的主机 */
			new Object[] {2, SERVERS, "192.168.0.1:5555", false, 9999999, null},
		};
	}
	
	/**
	 * 添加节点测试
	 * @param caseId
	 * @param initNodes
	 * @param addedNode
	 * @param expectedSucceed
	 * @param errorCode
	 * @param expectedNodes
	 */
	@Test(dataProvider = "addNodeDataProvider")
	public void addNode(int caseId, String[] initNodes, String addedNode, 
			boolean expectedSucceed, int errorCode, String[] expectedNodes) {
		LOGGER.debug(String.format("addNode, caseId=%d", caseId));
		String schemaName = "addnode_"+System.currentTimeMillis();
		try {
			trspermission.createSchema(schemaName, 6, Arrays.asList(initNodes));
			sleep();
			TRSSchema schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("[%s].addNode(%s)", schema.getName(), addedNode));
			schema.addNode(addedNode);
			schema.clearPartition();
			try {
				trspermission.updateSchema(schema, true);
				if(!expectedSucceed) {
					String failureMessage = String.format("为模式[%s]添加节点[%s]预期抛出异常, 实际未抛出", 
							schema.getName(), addedNode);
					LOGGER.error(failureMessage);
					fail(failureMessage);
				}
			} catch(TRSException e) {
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=%d, e.getErrorString=%s", 
						e.getErrorCode(), errorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), errorCode);
				return;
			}
			sleep();
			schema = trspermission.getSchema(schemaName);
			Set<String> nodeSet = schema.getNodeList();
			for(String expectedNode : expectedNodes)
				nodeSet.remove(expectedNode);
			assertEquals(nodeSet.size(), 0);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteSchema(schemaName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider (name = "removeNodeDataProvider")
	public Object[][] removeNodeDataProvider(Method method){
		if(!"removeNode".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 正常情况 */
			new Object[] {1, SERVERS, SERVERS[0], true, true, Integer.MIN_VALUE, new String[] {SERVERS[1], SERVERS[2]}},
			/* 异常, 移除一个不存在的节点 */
			/* 2021.4.14, 现在没有抛出异常, 还是提交到禅道上吧 */
			/* 2021.5.8 研发组表示抛出异常不太合理, 修改方法返回值为boolean, 成功返回true, 失败为false; 
			 * 移除失败(相当于未移除), 再提交, 和提交前没有变化 */
			new Object[] {2, SERVERS, "192.168.0.102:5555", false, true, Integer.MIN_VALUE, SERVERS},
			/* 移除了所有节点, 抛出异常 */
			new Object[] {3, new String[] {SERVERS[0]}, SERVERS[0], true, false, 9999999, null}
		};
	}
	
	/**
	 * 移除节点验证
	 * @param caseId
	 * @param initNodes
	 * @param removedNode
	 * @param expectedRemoveSucceed
	 * @param expectedUpdateSchemaSucceed
	 * @param errorCode
	 * @param expectedNodes
	 */
	@Test(dataProvider = "removeNodeDataProvider")
	public void removeNode(int caseId, String[] initNodes, String removedNode,
			boolean expectedRemoveSucceed, boolean expectedUpdateSchemaSucceed,int errorCode, String[] expectedNodes) {
		LOGGER.debug(String.format("removeNode, caseId=%d", caseId));
		String schemaName =  "removeschema_"+System.currentTimeMillis();
		try {
			trspermission.createSchema(schemaName, 6, Arrays.asList(initNodes));
			sleep();
			TRSSchema schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("[%s].removeNode(%s)", schema.getName(), removedNode));
			schema.removeNode(removedNode);
			LOGGER.debug(String.format("[%s].clearPartition()", schema.getName()));
			schema.clearPartition();
			try {
				trspermission.updateSchema(schema, true);
				if(! expectedUpdateSchemaSucceed) {
					String failureMessage = String.format("模式[%s]移除节点[%s]预期抛出异常, 实际未抛出", 
							schema.getName(), removedNode);
					LOGGER.error(failureMessage);
					fail(failureMessage);
				}
			} catch(TRSException e) {
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=%d, e.getErrorString=%s", 
						e.getErrorCode(), errorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), errorCode);
				return;
			}
			sleep();
			schema = trspermission.getSchema(schemaName);
			Set<String> nodeSet = schema.getNodeList();
			for(String expectedNode : expectedNodes)
				nodeSet.remove(expectedNode);
			assertEquals(nodeSet.size(), 0);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteSchema(schemaName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "deleteSchemaDataProvider")
	public Object[][] deleteSchemaDataProvider(Method method){
		if(!"deleteSchema".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 创建一个模式, 再删除这个模式 */
			new Object[] {1, new String[] {"first"}, new String[] {"first"}},
			/* 创建一批模式, 再删除这批模式 */
			new Object[] {2, new String[] {"second", "third"}, new String[] {"second", "third"}},
			/* 创建的模式和删除的模式不同(也就是删除了一个不存在的模式), 只要要求删除的模式都不存在就行 */
			new Object[] {3, new String[] {"ichi"}, new String[] {"ni"}},
			/* 删除的模式中, 同时包含现有的模式和不存在的模式 */
			new Object[] {4, new String[] {"san", "yonn"}, new String[] {"san", "ni"}},
			/* 重复删除, 等同于删除一个不存在的模式  */
			new Object[] {5, new String[] {"roku"}, new String[] {"roko", "roko"}},
		};
	}
	
	/**
	 * 模式删除正常情况验证<br>
	 * 这个方法只考虑最理想的情况: 模式下既没有数据库, 也没有用户, 一定可以删除<br>
	 * @param caseId
	 * @param createName 创建模式时给定的名称集, 给多少个名字创建多少个模式, 注意不要有重复
	 * @param deleteName 删除模式时给定的名称集, 会将指定的模式都删除, 注意重复删除会抛出异常(当然这也是其中一个测试用例)
	 */
	@Test(dataProvider = "deleteSchemaDataProvider")
	public void deleteSchema(int caseId, String[] createNames, String[] deleteNames) {
		LOGGER.debug("deleteSchema, caseId="+caseId);
		try {
			/* 先创建一(多)个模式 */
			for(int i=0; i<createNames.length; i++)
				trspermission.createSchema(createNames[i], 5, arrayToList(SERVERS));
			sleep();
			/* 然后再指定一(多)个模式, 删除 */
			trspermission.deleteSchema(deleteNames);
			sleep();
			/* 删除后应当没有这些模式, 通过名称检查 */
			List<TRSSchema> schema = trspermission.getSchemaList();
			List<String> schemaNames = new ArrayList<String>(schema.size());
			for(int i=0; i<schema.size(); i++)
				schemaNames.add(schema.get(i).getName());
			for(int i=0; i<deleteNames.length; i++)
				assertFalse(schemaNames.contains(deleteNames[i]));
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			/* 清理环境一定要用for循环, 这样有删除失败的行为(try块里已经按照预期删完模式)不会影响后边的删除作业 */
			for(int i=0; i<createNames.length; i++)
				try {
					trspermission.deleteSchema(createNames[i]);
				}catch(TRSException e){}
		}
	}
	
	@DataProvider(name = "deleteSchemaBoundaryDataProvider")
	public Object[][] deleteSchemaBoundaryDataProvider(Method method){
		if(!"deleteSchemaBoundary".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 删除system模式, 前面的创建模式只是为了能执行下去而已, 没有实际意义 */
			new Object[] {1, new String[] {"gou"}, new String[] {"system"}, 9999999},
		};
	}
	/**
	 * 删除模式边界值测试
	 * @param caseId
	 * @param createNames
	 * @param deleteNames
	 * @param errorCode
	 */
	@Test(dataProvider = "deleteSchemaBoundaryDataProvider")
	public void deleteSchemaBoundary(int caseId, String[] createNames, String[] deleteNames, int errorCode) {
		try {
			for(int i=0; i<createNames.length; i++)
				trspermission.createSchema(createNames[i], 5, arrayToList(SERVERS));
			sleep();
			try {
				trspermission.deleteSchema(deleteNames);
				fail("预期删除模式抛出异常, 但并没有");
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, errorMsg=%s", e.getErrorCode(), e.getErrorString()));
				assertEquals(e.getErrorCode(), errorCode);
			}
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			for(int i=0; i<createNames.length; i++)
				try {
					trspermission.deleteSchema(createNames[i]);
				}catch(TRSException e){}
		}
	}
	/**
	 * 删除模式时, 验证该模式下存在数据库抛出异常, 除非删除所有数据库方可删除模式
	 */
	@Test
	public void deleteSchemaWhenDatabaseExist() {
		String schemaName = "exist";
		String databaseName = schemaName + ".delete";
		try {
			/* 创建模式 */
			trspermission.createSchema(schemaName, 6, arrayToList(SERVERS));
			sleep();
			/* 建表 */
			createDemoDatabase(conn, schemaName + ".delete", 1, DBPOLICY.FASTEST);
			sleep();
			/* 直接删除模式, 预期抛出异常 */
			try {
				trspermission.deleteSchema(schemaName);
				/* 预期抛出异常, 要是没抛出异常, 那就是failure */
				fail(String.format("模式[%s]下存在数据库, 删除模式理应抛出异常, 但是现在没有抛出", schemaName));
			}catch(TRSException e) {
				assertEquals(e.getErrorCode(), 9999999);
			}
			/* 现在删表 */
			assertTrue(conn.deleteDatabase(databaseName));
			sleep();
			/* 再删模式, 这回抛异常直接failure */
			trspermission.deleteSchema(schemaName);
			sleep();
			/* 获取模式, 返回空 */
			assertNull(trspermission.getSchema(schemaName));
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			/* 不管运行结果如何, 强制清理环境 */
			try {
				conn.deleteDatabase(databaseName);
			}catch(TRSException e) {}
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {}
		}
	}
	
	/**
	 * 删除一个被用户引用的模式, 验证抛出异常。让所有用户放弃引用就可删除了
	 */
	@Test
	public void deleteSchemaWhenUserReferIt() {
		String userName = "handler";
		/* 新建模式 */
		String schemaName = "refer";
		
		try {
			trspermission.createSchema(schemaName, 6, arrayToList(SERVERS));
			sleep();
			/* 新建用户 */
			Set<String> schemas = new HashSet<String>();
			schemas.add(schemaName);
			trspermission.createUser(
					userName, "1234qwer", schemas, schemaName, "引用模式refer", true);
			sleep();
			/* 删除模式, 抛出异常 */
			try {
				trspermission.deleteSchema(schemaName);
				fail(String.format("模式[%s]被用户[%s]引用, 理应禁止删除, 结果现在允许删除了", schemaName, userName));
			}catch(TRSException e) {
				assertEquals(e.getErrorCode(), 9999999);
			}
			sleep();
			/* 删除用户 */
			trspermission.deleteUser(userName);
			sleep();
			/* 删除模式 */
			trspermission.deleteSchema(schemaName);
			sleep();
			/* 获取模式为空 */
			assertNull(trspermission.getSchema(schemaName));
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			try {
				trspermission.deleteUser(userName);
			}catch(TRSException e) {}
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider(name = "updateSchemaDataProvider")
	public Object[][] updateSchemaDataProvider(Method method){
		if(!"updateSchema".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 参数格式为*/
			/* new Object[] {用例编号, 
			 *               初始分区数, 
			 *               初始节点列表, 
			 *               新的分区数, 
			 *               新节点列表, 
			 *               是否自动清理分区表, 
			 *               是否数据重分布
			 *               预期是否更新成功,
			 *               预期记录入库是否成功, 
			 *               预期错误号} */
			
			/* 情况1: 分区数6改为20, 节点列表不变, 清理分区, 数据重分布, 预期通过 */
			new Object[] {1, 6, SERVERS, 20, SERVERS, true, true, true, true, Integer.MIN_VALUE},
			/* 情况2: 分区数9改成6, 去掉第三台服务器, 数据重分布, 预期通过 */
			new Object[] {2, 9, SERVERS, 6, new String[] {SERVERS[0], SERVERS[1]}, true, true, true, true, Integer.MIN_VALUE},
			/* 情况3: 分区数由6改成9, 添加第二台服务器, 数据重分布, 预期通过 */
			new Object[] {3, 6, new String[] {SERVERS[0], SERVERS[2]}, 9, SERVERS, true, true, true, true, Integer.MIN_VALUE},
			
			/* 情况4: 分区数6改为20, 节点列表不变, 清理分区, 不做数据重分布, 插入记录失败 */
			new Object[] {4, 6, SERVERS, 20, SERVERS, true, false, true, false, 9920034},
			/* 情况5: 分区数不变, 添加第一个节点, 清理分区, 不作数据重分布, 插入记录失败 */
			/* 2020-1-8: 分区数不变, 本可以不用显式设置, 但测试代码仍然强制指定, 结果节点列表更新失败了, 所以就提了bug */
			new Object[] {5, 9, new String[] {SERVERS[1], SERVERS[2]}, 9, SERVERS, true, false, true, false, 9920034},
			/* 情况6: 分区数和节点列表同时改变, 清理分区, 不做数据重分布, 插入记录失败 */
			/* 2020-1-8: 怎么回事, 你也failure...看来和case5的问题一样, 先等开发组修复再说 */
			new Object[] {6, 6, new String[] {SERVERS[1], SERVERS[2]}, 9, SERVERS, true, false, true, false, 9920034},
			
			/* 情况7: 和情况1一致, 但不执行clearPartition(), 主节点映射表中分区号不全, 预期抛异常; 数据重分布是否设置无所谓, 执行不到那儿 */
			new Object[] {7, 6, SERVERS, 20, SERVERS, false, true, false, true, 9999999},
			/* 情况8: 和情况2一致, 但不执行clearPartition(), 有服务器不在主节点映射表中, 预期抛出异常 */
			new Object[] {8, 9, SERVERS, 6, new String[] {SERVERS[0], SERVERS[1]}, false, true, false, true, 9999999}
		};
	}
	/**
	 * 验证模式更新成功简单验证<br>
	 * 最简单的情况验证, 即刚创建完模式便要修改模式设置
	 * @param caseId
	 * @param schemaName
	 */
	@Test(dataProvider = "updateSchemaDataProvider")
	public void updateSchema(int caseId, 
							 int initPartNum, 
							 String[] initNodes,
							 /* 要保证 partnum 是合法值 */
			                 int partnum, 
			                 String[] nodes, 
			                 boolean clearPartition,
			                 boolean redistributed,
			                 boolean expectedUpdateSuccess,
			                 boolean expectedLoadRecordsSuccess,
			                 int expectedErrorCode) {
		LOGGER.debug("updateSchema, caseId="+caseId);
		String schemaName = "modify"+caseId;
		String databaseName = schemaName + ".demo_" + System.currentTimeMillis();
		try {
			/* 初始创建模式 */
			LOGGER.debug(String.format("createSchema(%s, %d, %s)", schemaName, initPartNum, arrayToList(initNodes)));
			trspermission.createSchema(schemaName, initPartNum, arrayToList(initNodes));
			LOGGER.debug("createSchema succeed");
			sleep();
			TRSSchema schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("schema = getSchema(%s)", schemaName));
			/* 在没有创建表的情况下, 更新模式 */
			/* 不提供修改名称的入口, 这个测试方法也就不加这个参数了 */
			/* 重设分区数  */
			LOGGER.debug("setPartnum("+partnum+")");
			schema.setPartnum(partnum);
			/* 重新设定节点列表, */
			Set<String> nodeList = arrayToSet(nodes);
			LOGGER.debug("setNodeList("+nodeList+");");
			schema.setNodeList(nodeList);
			/* 开发组的单元测试里, 此处有注释: 重新设置了分区数或节点列表，而未执行清理，可能会抛出异常*/
			if(clearPartition) {
				LOGGER.debug("clearPartition()");;
				schema.clearPartition();
			}
			try {
				LOGGER.debug("updateSchema(schema, "+redistributed+");");
				trspermission.updateSchema(schema, redistributed);
				if(!expectedUpdateSuccess)
					fail("预期模式更新失败(expectedUpdateSuccess=false), 但是更新成功了");
			}catch(TRSException e) {
				assertEquals(e.getErrorCode(), expectedErrorCode);
				/* 预期抛出异常, 后续可不再执行 */
				return;
			}
			sleep();
			/* 走到这一步说明你预期更新模式成功, 并且实际上也确实按照你的要求更新了 */
			schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("schema.getName=%s, getPartnum=%d, getNodeList=%s", schema.getName(), schema.getPartnum(), schema.getNodeList()));
			assertEquals(schema.getName(), schemaName);
			assertEquals(schema.getPartnum(), partnum);
			assertEquals(schema.getNodeList(), nodeList);
			/* 建表*/
			LOGGER.debug("createDemoView(conn, "+databaseName+", TRSDatabase.TYPE_VIEW, DBPOLICY.FASTEST)");
			createDemoDatabase(conn, databaseName, TRSDatabase.TYPE_VIEW, DBPOLICY.FASTEST);
			LOGGER.debug("CreateDemoView succeed");
			int loop = 120;
			TRSDatabase[] dbs = conn.getDatabases(databaseName);
			while((dbs == null || dbs.length == 0) && loop-- > 0) {
				sleep(1000L);
				LOGGER.debug("loop="+loop);
				dbs = conn.getDatabases(databaseName);
			}			
			try {
				/* 记录入库, 此处要加 try-catch, 部分用例故意设置更新模式但不重分布, 不做重分布, 读写模式改不过来, 插入记录会报错 */
				String loadFilePath = "." + "/TRSSchema/demo.trs";
				LOGGER.debug(String.format("loadRecords(%s, %s, 0)", databaseName, loadFilePath));
				long loadNumber = conn.loadRecords(databaseName, loadFilePath, 0);
				LOGGER.debug("loadNumber="+loadNumber);
				/* 未做重分布的情况, 预期执行入库会抛出异常进入catch块, 如果没进入catch块, 就是failure */
				if(!expectedLoadRecordsSuccess) {
					fail(String.format("模式[%s](partnum=%d, isReadWrite=%s) 预期入库抛出异常, 但是现在成功了", 
							schema.getName(), schema.getPartnum(), schema.isReadwrite()));
				}
				assertEquals(loadNumber, 5079);
			}catch(TRSException e) {
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=%d, errorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			sleep();
			/* 获取数据库所有子库列表, 内部调用了 exuecteCommand("list_subdb", params);  */
			List<Subdb> subdbs = getSubdbsFromDatabase(conn, databaseName);
			LOGGER.debug("getSubdbFromDatabase(conn, "+databaseName+").size()="+subdbs.size());
			/* 因为这是有系统自动调整的分区映射表, 所以检查分区号的个数和分区数相同 */
			Set<String> partsIdSet = new HashSet<String>();
			Set<String> ipSet = new HashSet<String>();
			for(int i=0, size=subdbs.size(); i<size; i++) {
				partsIdSet.add(subdbs.get(i).partid);
				ipSet.add(subdbs.get(i).ip + ":"+subdbs.get(i).port);
			}
			assertEquals(partsIdSet.size(), schema.getPartnum());
			/* 再检查分布的节点和预期一致 */
			/* 方法是获取实际分布的节点ip:host, 和预期(变更表结构时给定的ip)作差, 得到空集 */
			Set<String> expectedIpSet = new HashSet<String>();
			for(int i=0; i<nodes.length; i++)
				expectedIpSet.add(nodes[i]);
			ipSet.removeAll(expectedIpSet);
			/* 验证空集 */
			assertTrue(ipSet.isEmpty());
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			try {
				conn.deleteDatabase(databaseName);
			}catch(TRSException e) {}
			sleep();
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {}
			sleep();
		}
	}
	
	@DataProvider(name = "updateSchemaComplexDataProvider")
	public Object[][] updateSchemaComplexDataProvider(Method method){
		if(!"updateSchemaComplex".equals(method.getName()))
			return null;
		
		/* 用例4需要用到的主分区表 */
		Map<String, List<Integer>> mainCopyCase4 = new HashMap<String, List<Integer>>();
		mainCopyCase4.put(SERVERS[0], Arrays.asList(0, 1, 2));
		mainCopyCase4.put(SERVERS[1], Arrays.asList(3, 4, 5));
		/* 用例4需要用到的副分区表 */
		Map<String, List<Integer>> partCopyCase4 = new HashMap<String, List<Integer>>();
		partCopyCase4.put(SERVERS[0], Arrays.asList(3, 4, 5));
		partCopyCase4.put(SERVERS[1], Arrays.asList(0, 1, 2));
		
		/* 用例5需要用到的主分区映射表 */
		Map<String, List<Integer>> mainCopyCase8 = new HashMap<String, List<Integer>>();
		mainCopyCase8.put(SERVERS[0], Arrays.asList(0, 3));
		mainCopyCase8.put(SERVERS[1], Arrays.asList(1, 4));
		mainCopyCase8.put(SERVERS[2], Arrays.asList(2, 5));
		/* 用例8需要用到的副分区映射表 */
		Map<String, List<Integer>> partCopyCase8 = new HashMap<String, List<Integer>>();
		partCopyCase8.put(SERVERS[0], Arrays.asList(1, 4));
		partCopyCase8.put(SERVERS[1], Arrays.asList(2, 5));
		partCopyCase8.put(SERVERS[2], Arrays.asList(0, 3));
		
		/* 用例5需要用到的主分区映射表 */
		Map<String, List<Integer>> mainCopyCase9 = new HashMap<String, List<Integer>>();
		mainCopyCase9.put(SERVERS[0], Arrays.asList(0, 3));
		mainCopyCase9.put(SERVERS[1], Arrays.asList(1, 4));
		mainCopyCase9.put(SERVERS[2], Arrays.asList(2, 5));
		/* 用例8需要用到的副分区映射表, 注意0号分区也位于主分区映射表的1号节点中 */
		Map<String, List<Integer>> partCopyCase9 = new HashMap<String, List<Integer>>();
		partCopyCase9.put(SERVERS[0], Arrays.asList(1, 0));
		partCopyCase9.put(SERVERS[1], Arrays.asList(2, 5));
		partCopyCase9.put(SERVERS[2], Arrays.asList(4, 3));
		
		/* 主分区映射表的分区号重复 */
		Map<String, List<Integer>> mainCopyCase10 = new HashMap<String, List<Integer>>();
		mainCopyCase10.put(SERVERS[0], Arrays.asList(0, 0));
		mainCopyCase10.put(SERVERS[1], Arrays.asList(1, 4));
		mainCopyCase10.put(SERVERS[2], Arrays.asList(2, 5, 3));
		/* 主分区映射表虽然错了, 但是仍然要一并提交副分区映射, 否则会因为判定副分区映射表为空而抛出异常 */
		Map<String, List<Integer>> partCopyCase10 = new HashMap<String, List<Integer>>();
		partCopyCase10.put(SERVERS[0], Arrays.asList(1, 4));
		partCopyCase10.put(SERVERS[1], Arrays.asList(2, 5));
		partCopyCase10.put(SERVERS[2], Arrays.asList(3, 6));
		
		/* 主分区映射表的分区号丢失 */
		Map<String, List<Integer>> mainCopyCase11 = new HashMap<String, List<Integer>>();
		mainCopyCase11.put(SERVERS[0], Arrays.asList(0));
		mainCopyCase11.put(SERVERS[1], Arrays.asList(1, 4));
		mainCopyCase11.put(SERVERS[2], Arrays.asList(2, 5));
		
		return new Object[][] {
			/* new Object[]{用例编号, 初始分区数, 初始节点列表, 数据库类型, 
			 * 				是否手工提供主副分区表, 主分区表, 副分区表,
			 * 				预修改的分区数, 预修改节点列表, 是否重分布, 预期更新模式是否成功, 预期更新模式错误号,
			 * 				预期更新后的读写模式, 预期更新后的节点列表, 预期更新后的分区数, 预期更新记录是否成功, 预期更新记录的错误号, 
			 * 				预期的主分区映射表} */	
			
			/* 分区数不变, 去掉最后一个节点, 自动重分布, 可以对模式下的表读写 */
			new Object[] {1, 6, SERVERS, TRSDatabase.TYPE_VIEW, 
						  /* 让系统自动调整主副分区表, 主副分区表这俩参数给null就行 */
						  false, null, null,
						  6, new String[] {SERVERS[0], SERVERS[1]}, true, true, Integer.MIN_VALUE,
						  true, new String[] {SERVERS[0], SERVERS[1]}, 6, true, Integer.MIN_VALUE,
						  null},
			
			/* 分区数不变, 由两个节点增加到三个, 自动重分布, 可以对模式下的表读写 */
			new Object[] {2, 6, new String[] {SERVERS[0], SERVERS[1]}, TRSDatabase.TYPE_VIEW, 
						false, null, null,
						6, SERVERS, true, true, Integer.MIN_VALUE,
						true, SERVERS, 6, true, Integer.MIN_VALUE,
						null},
			
			/* 分区数和节点同时变化, 但表类型为单节点数据库, 无影响 */
			new Object[] {3, 6, SERVERS, TRSDatabase.TYPE_DATABASE,
						  false, null, null,
						  12, new String[] {SERVERS[1]}, true, true, Integer.MIN_VALUE,
						  true, new String[] {SERVERS[1]}, 12, true, Integer.MIN_VALUE,
						  null},
			
			/* 手工提供合法主副分区表, 去掉最后一个节点, 注意主副分区表也要同步调整 */
			new Object[] {4, 6, SERVERS, TRSDatabase.TYPE_VIEW,
						true, mainCopyCase4, partCopyCase4,
						6, new String[] {SERVERS[0], SERVERS[1]}, true, true, Integer.MIN_VALUE,
						true, new String[] {SERVERS[0], SERVERS[1]}, 6, true, Integer.MIN_VALUE,
						mainCopyCase4},
			
			/* 异常情况 */
			
			/* 分区数不变, 去掉一个节点, 不做数据重分布, 读写模式一直是false, 无法写操作, 更新记录时抛出异常 */
			new Object[] {5, 12, SERVERS, TRSDatabase.TYPE_VIEW,
						false, null, null,
						12, new String[] {SERVERS[0], SERVERS[1]}, false, true, Integer.MIN_VALUE,
						false, SERVERS, 12, false, 9920034,
						null},
			
			/* 视图类型, 变更了分区数, 抛出异常 */
			new Object[] {6, 6, SERVERS, TRSDatabase.TYPE_VIEW,
						false, null, null,
						12, SERVERS, true, false, 9999999,
						/* 下边几个参数对于异常的用例而言无意义 */
						false, null, -1, false, Integer.MIN_VALUE,
						null},
			
			/* 添加了一个不存在的节点, 抛出异常 */
			new Object[] {7, 6, SERVERS, TRSDatabase.TYPE_VIEW,
						false, null, null,
					    6, new String[] {SERVERS[0], SERVERS[1], "192.168.0.102:5555"}, true, false, 9999999,
					    false, null, -1, false, Integer.MIN_VALUE,
					    null},
			
			/* 去掉一个节点, 但主分区映射表没做调整,仍然保持3个节点, 提交重分布任务抛出异常*/
			new Object[] {8, 6, SERVERS, TRSDatabase.TYPE_VIEW,
						 true, mainCopyCase8, partCopyCase8,
						 6, new String[] {SERVERS[0]}, true, false, 9999999,
						 false, null, -1, false, Integer.MIN_VALUE,
						 null},
			
			/* 副节点映射表中，分区号0的主节点位于同一个节点上, 提交任务时抛出异常 */
			new Object[] {9, 6, SERVERS, TRSDatabase.TYPE_VIEW,
						 true, mainCopyCase9, partCopyCase9,
						 6, SERVERS, true, false, 9999999,
						 false, null, -1, false, Integer.MIN_VALUE,
						 null},
			
			/* 主分区映射表的分区号重复 */
			/* 2021.4.8 没抛异常, 提交到禅道 */
			/* 2021.5.10 已改, 但要注意一定要提供副分区映射表, 这样一来不会因没提交副分区映射表抛出异常 */
			new Object[] {10, 6, SERVERS, TRSDatabase.TYPE_VIEW,
						true, mainCopyCase10, partCopyCase10,
						6, SERVERS, true, false, 9999999,
						false, null, -1, false, Integer.MIN_VALUE,
						null},
			
			/* 主分区映射表的分区号缺失 */
			/* 2021.4.8 也没抛出异常,  */
			/* 2021.5.10 已改 */
			new Object[] {11, 6, SERVERS, TRSDatabase.TYPE_VIEW,
					true, mainCopyCase11, null,
					6, SERVERS, true, false, 9920034,
					false, null, -1, false, Integer.MIN_VALUE,
					null},
		};
	}
	/**
	 * 模式更新, 复杂情况验证<br>
	 * 新建模式后, 新建一张表并装入记录(简单情况没有这步), 然后修改模式, 考虑是否重分布的差异, 然后修改原来的表的一条记录<br>
	 * 新建的表要考虑到数据库和视图的差别<br><br>
	 * 
	 * P.S. 测试方法有坑, 没填完;  目前会有这些“可能”的情况
	 * 1.TRSSchema.getReadwrite()==true以后, 尝试executeUpdate()结果抛出子库009#005 is read only异常导致后续操作都没法做<br>
	 * 2.120秒后 TRSSchema.getReadwrite() 仍然为false<br>
	 * 3.TRSSchema.getReadwrite()==true以后, 尝试executeUpdate()结果抛出subdatabase:009#005 is not main copy<br><br>
	 * 
	 * 这样一来无法验证模式更新后写操作<br>
	 * 2021.3.29 回归以后不再出现上述三个现象, 可以继续编写测试用例了<br><br>
	 * 
	 * 分时归档视图见方法updateSchemaInfluenceFifo()<br>
	 * 虚拟数据库, 镜像表还有负载均衡视图好像还不能用这个方法检查……<br>
	 * 
	 * @param caseId 用例编号
	 * @param partNum 初始分区数
	 * @param nodes 初始节点列表
	 * @param dbType 数据库类型
	 * @param isManualSubmitCopyTable 是否在测试用例中手工给出 主副分区表
	 * @param mainCopys 主分区映射表, 仅当 isManualSubmitCopyTable=true 时有效
	 * @param partCopys 副分区映射表, 仅当 isManualSubmitCopyTable=true 时有效
	 * @param newPartNum 新的分区数
	 * @param newNodes 新的节点列表
	 * @param redistribute 是否数据重分布
	 * @param expectedUpdateSchemaSucceed 预期更新模式是否成功
	 * @param expectedErrorCode 预期更新模式的错误号, 如果没错就设置为 Integer.MIN_VALUE
	 * @param expectedSchemaReadwrite 预期模式更新后的读写模式
	 * @param expectedSchemaNodes 预期模式更新后的节点列表
	 * @param expectedPartNum 预期更新后的分区数
	 * @param expectedUpdatedRecordsSucceed 预期更新记录是否成功
	 * @param expectedUpdateRecordsErrorCode 预期更新记录的错误号
	 * @param expectedMainCopys 预期主分区映射表, 仅当 isManualSubmitCopyTable=true 时才参与检查
	 */
	@Test(dataProvider = "updateSchemaComplexDataProvider")
	public void updateSchemaComplex(int caseId, int partNum, String[] nodes, int dbType, 
			boolean isManualSubmitCopyTable, Map<String, List<Integer>> mainCopys, Map<String, List<Integer>> partCopys,
			int newPartNum, String[] newNodes,boolean redistribute,boolean expectedUpdateSchemaSucceed, int expectedErrorCode,
			boolean expectedSchemaReadwrite, String[] expectedSchemaNodes, int expectedPartNum, boolean expectedUpdatedRecordsSucceed,
			int expectedUpdateRecordsErrorCode, Map<String, List<Integer>> expectedMainCopys) {
		LOGGER.debug("updateSchemaComplex, caseId=="+caseId);
		String schemaName = "modify_" + caseId + "_" + System.currentTimeMillis();
		String databaseName = schemaName + ".update_" + System.currentTimeMillis();
		try {
			List<String> nodeList = arrayToList(nodes);
			/* 创建模式 */
			LOGGER.debug(String.format("createSchema(%s, %d, %s)", schemaName, partNum, nodeList));
			trspermission.createSchema(schemaName, partNum, nodeList);
			LOGGER.debug("Creating schema succeed.");
			sleep();
			/* 创建数据库 */
			LOGGER.debug(String.format("createDemoDatabase(%s, %d, DBPOLICY.NORMAL)", databaseName, dbType));
			createDemoDatabase(conn, databaseName, dbType, DBPOLICY.NORMAL);
			LOGGER.debug("Creating database succeed");
			sleep();
			/* 插入记录 */
			String loadFilePath = "." + "/TRSSchema/demo.trs";
			LOGGER.debug(String.format("loadRecords(%s, %s, 0)", databaseName, loadFilePath));
			long loadNumber = conn.loadRecords(databaseName, loadFilePath, 0);
			LOGGER.debug("loadNumber="+loadNumber);
			assertEquals(loadNumber, 5079);
			sleep();
			/* 修改模式 */
			LOGGER.debug(String.format("schema = trspermission.getSchema(%s)", schemaName));
			TRSSchema schema = trspermission.getSchema(schemaName);
			LOGGER.debug(schema.getName()+".print()="+schema.print());
			LOGGER.debug(schema.getName()+".setPartnum("+newPartNum+")");
			schema.setPartnum(newPartNum);
			Set<String> nodeSet = arrayToSet(newNodes);
			LOGGER.debug(schema.getName()+".setNodeList("+nodeSet+")");
			schema.setNodeList(nodeSet);
			/* 通过参数给出是否交给系统自动整理主副分区表*/
			if(isManualSubmitCopyTable) {
				/* 是, 使用参数提供的主副分区表 */
				LOGGER.debug(String.format("%s.setPartmain(%s)", schema.getName(), mainCopys));
				schema.setPartmain(mainCopys);
				LOGGER.debug(String.format("%s.setPartCopy(%s)", schema.getName(), partCopys));
				schema.setPartcopy(partCopys);
			}else {
				/* 否, 让系统自动清理分区表*/
				LOGGER.debug(String.format("%s.clearPartition()", schema.getName()));
				schema.clearPartition();
			}
			LOGGER.debug("updateSchema("+schema.getName()+")");
			try {
				/* 注意1：如果已有的表是视图, 然后变更了分区数, 更新模式会抛出异常 */
				/* 注意2: 主副分区表的值如果非法, 更新模式会抛出异常 */
				trspermission.updateSchema(schema, redistribute);
				if(!expectedUpdateSchemaSucceed)
					fail(String.format("更新模式[%s]预期抛出异常, 实际未抛出; 模式信息为%s", schemaName, schema.print()));
				sleep();
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode()=%d, expected=%d, e.getErrorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			/* 模式更新后的检查 */
			LOGGER.debug(String.format("updated = trspermission.getSchema(%s)", schemaName));
			/* 检查读写模式, 这个结果和是否重分布有关,
			 * 因为数据重分布需要等待时间, 这里人工设定超过XX秒直接失败 */
			waitingUntilRedistributeCompleteOrTimeout(schemaName, 60);
			TRSSchema updated = trspermission.getSchema(schemaName);
			LOGGER.debug(updated.getName()+".isReadwrite()="+updated.isReadwrite()+", expected=="+expectedSchemaReadwrite);
			assertEquals(updated.isReadwrite(), expectedSchemaReadwrite);
			/* 只有手工给出主副分区映射表的情况下, 才去检查主副分区表是否和设置的一致 */
			if(isManualSubmitCopyTable) {
				assertEquals(updated.getPartmain(), mainCopys);
				assertEquals(updated.getPartcopy(), partCopys);
			}
			/* 更新记录前先打一遍日志 */
			LOGGER.debug(String.format("%s.print()==%s", updated.getName(), updated.print()));
			/* 检查节点 */
			Set<String> expectedNodeSet = arrayToSet(expectedSchemaNodes);
			Set<String> actualNodeSet = updated.getNodeList();
			LOGGER.debug(String.format("%s.getNodeList()=%s, expected=%s", updated.getName(), actualNodeSet, expectedNodeSet));
			actualNodeSet.removeAll(expectedNodeSet);
			assertTrue(actualNodeSet.isEmpty());
			/* 检查分区数 */
			LOGGER.debug(String.format("%s.getPartnum()=%d, expected=%d", updated.getName(), updated.getPartnum(), expectedPartNum));
			assertEquals(updated.getPartnum(), expectedPartNum);		
			/* 验证表的基本操作 */
			/* 先验证检索, 这个无论是否重分布和读写模式一定都能完成 */
			LOGGER.debug(String.format("resultSet = executeSelect(%s, rowid:*, 0, 10, new SearchParams)", databaseName));
			TRSResultSet resultSet = conn.executeSelect(databaseName, "rowid:*", 0, 10, new SearchParams());
			assertEquals(resultSet.size(), 10);
			/* 拿到第一条记录的UUID, 准备修改这条记录, 验证是否更新成功(同时受读写模式的影响, 可能会失败, 需要加try-catch) */
			LOGGER.debug("resultSet.moveFirst()");
			resultSet.moveFirst();
			LOGGER.debug("firstRecord = resultSet.get()");
			TRSRecord firstRecord = resultSet.get();
			String firstUid = firstRecord.getUid();
			LOGGER.debug("firstUid="+firstRecord.getUid());
			List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>(1);
			TRSInputRecord inputRecord = new TRSInputRecord();
			inputRecord.setUid(firstUid);
			inputRecord.addColumn("正文", "更新记录");
			LOGGER.debug("inputRecords.add(inputRecord)");
			inputRecords.add(inputRecord);
			try {
				/* 如果数据库的读写模式为 Only-Read, 这里应当抛出异常 */
				LOGGER.debug(String.format("executeUpdate(%s, inputRecords)", databaseName));
				conn.executeUpdate(databaseName, inputRecords);
				if(!expectedUpdatedRecordsSucceed) {
					fail(String.format("预期更新数据库[%s]记录抛出异常, 但实际并未抛出异常", databaseName));
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode()=%d, errorString=%s, expected=%d", 
						e.getErrorCode(), e.getErrorString(), expectedUpdateRecordsErrorCode));
				assertEquals(e.getErrorCode(), expectedUpdateRecordsErrorCode);
				/* 预期更新失败了, 也就不需要再执行了, 返回 */
				return;
			}
			sleep();
			/* 到这里, 说明更新记录已经成功, 再检索一次 */
			resultSet = conn.executeSelect(databaseName, "正文:更新记录", 0, 1, new SearchParams());
			assertEquals(resultSet.size(), 1);
			/* 获取数据库所有子库列表, 内部调用了 exuecteCommand("list_subdb", params);  */
			List<Subdb> subdbs = getSubdbsFromDatabase(conn, databaseName);
			LOGGER.debug("getSubdbFromDatabase(conn, "+databaseName+").size()="+subdbs.size());
			Set<String> partsIdSet = new HashSet<String>();
			Set<String> ipSet = new HashSet<String>();
			for(int i=0, size=subdbs.size(); i<size; i++) {
				partsIdSet.add(subdbs.get(i).partid);
				ipSet.add(subdbs.get(i).ip + ":"+subdbs.get(i).port);
			}
			/* 单节点数据库分区名集合固定为000, 000是由getSubdbsFromDatabase()给的, 长度肯定只有1 */
			if(dbType == TRSDatabase.TYPE_DATABASE) {
				LOGGER.debug(String.format("partsIdSet.size=%d, expected=1", partsIdSet.size()));
				assertEquals(partsIdSet.size(), 1);
			}else {
				if(isManualSubmitCopyTable) {
					/* 手工提交主分区映射表的情况, 需要和预期比较 */
					Map<String, List<Integer>> actualMainCopy = getActualMainCopy(subdbs);
					LOGGER.debug(String.format("actualMainCopy=%s, expectedMainCopy=%s", actualMainCopy, expectedMainCopys));
					/* _(:з」∠)_我的天哪, 还得排序…… */
					assertEquals(sort(actualMainCopy), sort(expectedMainCopys));
				}else {
					/* 由系统自动调整主副分区表时, 只要比较分区名集合的大小和分区数一致即可 */
					LOGGER.debug(String.format("partsIdSet.size=%d, schema.getPartnum=%d", partsIdSet.size(), schema.getPartnum()));
					assertEquals(partsIdSet.size(), schema.getPartnum());
				}
			}
			/* 再检查分布的节点和预期一致 */
			/* 方法是获取实际分布的节点ip:host, 和预期(变更表结构时给定的ip)作差, 得到空集 */
			Set<String> expectedIpSet = new HashSet<String>();
			for(int i=0; i<expectedSchemaNodes.length; i++)
				expectedIpSet.add(expectedSchemaNodes[i]);
			LOGGER.debug(String.format("ipSet=%s, expectedIpSet=%s", ipSet, expectedIpSet));
			ipSet.removeAll(expectedIpSet);
			/* 验证空集 */
			assertTrue(ipSet.isEmpty());
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			try {
				conn.deleteDatabase(databaseName);
			}catch(TRSException e) {}
			sleep();
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {}
		}
	}
	
	/**
	 * 手工整理子库列表并获得主分区表
	 * @param subdbs
	 * @return
	 */
	private static Map<String, List<Integer>> getActualMainCopy(List<Subdb> subdbs){
		Map<String, List<Integer>> actualMainCopy = new HashMap<String, List<Integer>>();
		/* 下边这段代码手工整理子库列表为主分区映射表, 举例:
		 * 
		 * subdb.get(0)=[ip=192.168.105.190, port=5555, partsid=0, subdbid=000, dbname=system.demo]
		 * subdb.get(1)=[ip=192.168.105.191, port=5555, partsid=1, subdbid=000, dbname=system.demo]
		 * subdb.get(2)=[ip=192.168.105.190, port=5555, partsid=0, subdbid=001, dbname=system.demo] -> 这是同一个分裂库的不同子库, 对于主分区映射表而言, 它是重复项, 需要排重
		 * subdb.get(3)=[ip=192.168.105.190, port=5555, partsid=2, subdbid=000, dbname=system.demo]  
		 * ...
		 * 
		 * 整理为
		 * [
		 *  key = 192.168.105.190:5555, value = [0, 2]
		 *  key = 192.168.105.191:5555, value = [1]
		 * ]
		 * */
		List<Integer> partsPerHost;
		for(int i=0, size=subdbs.size(); i<size; i++) {
			String key = subdbs.get(i).ip + ":" + subdbs.get(i).port;
			if(!actualMainCopy.containsKey(key)){
				actualMainCopy.put(key, new ArrayList<Integer>());
				continue;
			}
			partsPerHost = actualMainCopy.get(key);
			if(partsPerHost == null || partsPerHost.isEmpty()) {
				partsPerHost = new ArrayList<Integer>();
			}
			if(!partsPerHost.contains(Integer.valueOf(subdbs.get(i).partid)))
				partsPerHost.add(Integer.valueOf(subdbs.get(i).partid));
			actualMainCopy.put(key, partsPerHost);
		}
		return actualMainCopy;
	}
	
	/**
	 * 给主分区映射表做排序<br>
	 * 根据key排序, 字段顺序; value自身做排序
	 * @param map
	 * @return
	 */
	private static Map<String, List<Integer>> sort(Map<String, List<Integer>> map){
		List<Entry<String, List<Integer>>> mapEntrys = new ArrayList<Entry<String, List<Integer>>>();
		for(Entry<String, List<Integer>> entry : map.entrySet())
			mapEntrys.add(entry);
		Collections.sort(mapEntrys, new Comparator<Entry<String, List<Integer>>>(){
			@Override
			public int compare(Entry<String, List<Integer>> e1, Entry<String, List<Integer>> e2) {
				return e1.getKey().compareTo(e2.getKey());
			}
		});
		LinkedHashMap<String,List<Integer>> result = new LinkedHashMap<String,List<Integer>>();
		String key = null;
		List<Integer> value = null;
		for(int i=0, size=mapEntrys.size(); i<size; i++) {
			key = mapEntrys.get(i).getKey();
			value = mapEntrys.get(i).getValue();
			Collections.sort(value);
			result.put(key, value);
		}
		return result;
	}
	
	/**
	 * 获取一个数据库的所有子库的列表
	 * @param conn
	 * @param databaseName
	 * @return
	 * @throws TRSException
	 */
	private static List<Subdb> getSubdbsFromDatabase(TRSConnection conn, String databaseName) throws TRSException{
		List<Subdb> subdbs = new ArrayList<Subdb>();
		/* 举个例子, commandResult=000@modify.demo#007,192.168.101.243,5555;001@modify.demo#007,192.168.101.243,5555;...*/
		String subdbsStr = getSubdbs(conn, databaseName);
		int loop = 120;
		while((subdbsStr == null || subdbsStr.isEmpty()) && loop-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			subdbsStr = getSubdbs(conn, databaseName);
		}
		if((subdbsStr == null || subdbsStr.isEmpty()))
			return subdbs;
		String[] subdbArray = subdbsStr.split(";");
		String subdbInstanceStr = null;
		String[] subdbInstanceInfos = null;
		Subdb subdb = null;
		/* 用英文分号分隔之后, 数组的每个元素形式为: 000@modify.demo#007,192.168.101.243,5555 */
		for(int i=0; i<subdbArray.length; i++) {
			subdbInstanceStr = subdbArray[i];
			/* 把 @ 和 # 替换成半角英文逗号, 这样单个串变为 000,modify.demo,007,192.168.101.243,5555 */
//			LOGGER.debug("subdbInstanceStr="+subdbInstanceStr);
			subdbInstanceStr = subdbInstanceStr.replaceAll("[@#]", ",");
			/* 再把这个串按照逗号分隔, 能分成4~5个元素
			 * 
			 * 视图类型数据库会分隔成
			 * 0:   000
			 * 1:   modify.demo
			 * 2:   007
			 * 3:   192.168.101.243
			 * 4:   5555  
			 * 
			 * 单节点数据库会分割成
			 * 0:   000
			 * 1:   modify.demo
			 * 2:   192.168.105.191
			 * 3:   5555 */
			subdbInstanceInfos = subdbInstanceStr.split(",");
			subdb = new Subdb();
			if(subdbInstanceInfos.length == 4) {
				subdb.subdbid = subdbInstanceInfos[0];
				subdb.dbname = subdbInstanceInfos[1];
				/* 单节点数据库, 分区号锁定给000 */
				subdb.partid = "000";
				subdb.ip = subdbInstanceInfos[2];
				subdb.port = subdbInstanceInfos[3];
			}else {
				subdb.subdbid = subdbInstanceInfos[0];
				subdb.dbname = subdbInstanceInfos[1];
				subdb.partid = subdbInstanceInfos[2];
				subdb.ip = subdbInstanceInfos[3];
				subdb.port = subdbInstanceInfos[4];
			}
			LOGGER.debug(subdb);
			subdbs.add(subdb);
		}
		return subdbs;
	}
	
	/**
	 * 等待重分布完成或超时
	 * @param schemaName
	 * @throws TRSException
	 */
	private void waitingUntilRedistributeCompleteOrTimeout(String schemaName, int loop) throws TRSException {
		TRSSchema schema = trspermission.getSchema(schemaName);
		while(loop -- > 0) {
			sleep(1000L);
			LOGGER.debug(String.format("loop=%d, schema = trspermission.getSchema(%s)", loop, schemaName));
			schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("[%s].isReadwrite=%b", schema.getName(), schema.isReadwrite()));
		}
	}
	
	@DataProvider (name = "updateSchemaInfluenceFifoDataProvider")
	public Object[][] updateSchemaInfluenceFifoDataProvider(Method method){
		if(!"updateSchemaInfluenceFifo".equals(method.getName()))
			return null;
		
		/* 标准主分区映射 */
		Map<String, List<Integer>> mainCopyStandard = new HashMap<String, List<Integer>>();
		mainCopyStandard.put(SERVERS[0], Arrays.asList(0, 3));
		mainCopyStandard.put(SERVERS[1], Arrays.asList(1, 4));
		mainCopyStandard.put(SERVERS[2], Arrays.asList(2, 5));
		/* 标准副分区映射 */
		Map<String, List<Integer>> partCopyStandard = new HashMap<String, List<Integer>>();
		partCopyStandard.put(SERVERS[0], Arrays.asList(1, 4));
		partCopyStandard.put(SERVERS[1], Arrays.asList(2, 5));
		partCopyStandard.put(SERVERS[2], Arrays.asList(0, 3));
		/* 用例1的预期子库列表 */
		String ip = SERVERS[0].substring(0, SERVERS[0].lastIndexOf(":"));
		List<Subdb> expectedSubdbsCase1 = buildExpectedSubdbs(ip);
		
		/* 用例2 主分区映射表 */
		Map<String, List<Integer>> mainCopyCase2 = new HashMap<String, List<Integer>>();
		mainCopyCase2.put(SERVERS[1], Arrays.asList(0, 2, 4, 6, 8, 10));
		mainCopyCase2.put(SERVERS[2], Arrays.asList(1, 3, 5, 7, 9, 11));
		/* 用例2 副分区映射表 */
		Map<String, List<Integer>> partCopyCase2 = new HashMap<String, List<Integer>>();
		partCopyCase2.put(SERVERS[1], Arrays.asList(1, 3, 5, 7, 9, 11));
		partCopyCase2.put(SERVERS[2], Arrays.asList(0, 2, 4, 6, 8, 10));
		/* 用例2 预期子库列表 */
		ip = SERVERS[1].substring(0, SERVERS[1].lastIndexOf(":"));
		List<Subdb> expectedSubdbsCase2 = buildExpectedSubdbs(ip);
		
		/* 用例3的主副分区映射表和用例1一致 */
		/* 用例3预期子库列表, 和用例1一致 */
		List<Subdb> expectedSubdbsCase3 = expectedSubdbsCase1;
		
		return new Object[][] {
			/* 高性能分时归档视图, 根据主分区映射表, 000区的子库应该位于 SERVERS[0] 这个节点中 */
			new Object[] {1, 6, SERVERS, DBPOLICY.FASTEST, 
					SERVERS, mainCopyStandard, partCopyStandard, 
					expectedSubdbsCase1},
			/* 均衡分时归档视图, 去掉了第一个节点, 然后重分布, 根据主分区映射表, 000区的子库应该均位于 SERVERS[2] 这个节点中 */
			new Object[] {2, 12, SERVERS, DBPOLICY.NORMAL, 
					new String[] {SERVERS[1], SERVERS[2]}, mainCopyCase2, partCopyCase2, 
					expectedSubdbsCase2},
			/* 高可靠分时归档视图, 由一个节点(无副本)扩容到三个节点, 根据主分区映射表, 000区的子库应该位于 SERVERS[0] 这个节点中 */
			new Object[] {3, 6, new String[] {SERVERS[0]}, DBPOLICY.SAFEST,
					SERVERS, mainCopyStandard, partCopyStandard,
					expectedSubdbsCase3},
		};
	}
	
	/**
	 * 给更新模式后的分时归档视图构造预期子库列表<br>
	 * 因为测试数据是固定的, 这样一来分区号和子库号都是确定的了, 每条用例都一样,能根据分区表变化的只有所在节点ip 
	 * @param expectedIp
	 * @return
	 */
	private static List<Subdb> buildExpectedSubdbs(String expectedIp){
		List<Subdb> expected = new ArrayList<Subdb>();
		expected.add(Subdb.build().setSubdbid("00000000").setPartid("000").setIp(expectedIp));
		expected.add(Subdb.build().setSubdbid("99999999").setPartid("000").setIp(expectedIp));
		String subdbid = null;
		for(int i=-7; i<=7; i++) {
			subdbid = LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")).substring(0, 8);
			expected.add(Subdb.build().setSubdbid(subdbid).setPartid("000").setIp(expectedIp));
		}
		return expected;
	}
	
	/**
	 * 分时归档视图, 模式更新后的检查.<br>
	 * 顺便也检查不同策略下的数据库的子库列表(实际上没有差异,即使子库不在本地,那也是存在的)<br><br>
	 * 
	 * 测试数据固定提供17条记录<br>
	 * 分时归档视图的子库打开数固定为3<br>
	 * 分区字段固定为空串, 一定会进入000区<br>
	 * 每条记录的分库字段值都是不同的日期(因此一定有17个子库, 和这些子库在HDFS还是本地无关), 将17条记录的分库字段提取出来, 罗列如下<br>
	 * <ul>
	 * 	<li>前第8天 -> 子库一定是00000000, 因此是热数据</li>
	 * 	<li>前第7天 -> 冷数据 -> 同时它也是表属性 split.start.date </li>
	 *  <li>前第6天 -> 冷数据</li>
	 *  <li>前第5天 -> 冷数据</li>
	 *  <li>前第4天 -> 冷数据 -> 测试方法在最后一步提交更新的那条记录</li>
	 *  <li>前第3天 -> 冷数据</li>
	 *  <li>前天</li>
	 *  <li>昨天</li>
	 *  <li>今天</li>
	 *  <li>明天</li>
	 *  <li>后天</li>
	 *  <li>后第3天</li>
	 *  <li>后第4天</li>
	 *  <li>后第5天</li>
	 *  <li>后第6天</li>
	 *  <li>后第7天 -> 它是表属性 split.date.end</li>
	 *  <li>后第8天 -> 子库一定是99999999</li>
	 * </ul>
	 * @param caseId
	 * @param partNum 分区数
	 * @param nodes 初始节点
	 * @param policy 数据库策略
	 * @param newNodes 新节点
	 * @param mainCopys 手工提供的主分区映射表
	 * @param partCopys 手工提供的副分区映射表
	 * @param expectedSubdbs 预期的子库列表, 只检查subdbid, partid, ip 这三个属性
	 */
	@Test(dataProvider = "updateSchemaInfluenceFifoDataProvider")
	public void updateSchemaInfluenceFifo(
			int caseId, int partNum, String[] nodes, DBPOLICY policy, 
			String[] newNodes, Map<String, List<Integer>> mainCopys, Map<String, List<Integer>> partCopys, 
			List<Subdb> expectedSubdbs) {
		LOGGER.debug("updateSchemaInfluenceFifo, caseId="+caseId);
		String schemaName = "updateschema_"+System.currentTimeMillis();
		String dbName = schemaName + ".fifo_" + System.currentTimeMillis();
		List<String> nodeList = Arrays.asList(nodes);
		try {
			/* 创建模式 */
			LOGGER.debug(String.format("trspermission.createSchema(%s, %d, %s)", 
					schemaName, partNum, nodeList));
			trspermission.createSchema(schemaName, partNum, nodeList);
			sleep();
			/* 创建分时归档视图*/
			createASimpleFifoView(conn, dbName, policy);
			sleep();
			/* 插入记录 */
			List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>();
			TRSInputRecord inputRecord = null;
			for(int i=-8; i<=8; i++) {
				inputRecord = new TRSInputRecord();
				/* 固定给空串, 直接进入000分区 */
				inputRecord.addColumn("rowid", "");
				inputRecord.addColumn("number", String.valueOf(i));
				inputRecord.addColumn("date", LocalDateTime.now().plusDays(i).format(DATE_TIME_FORMATTER).substring(0, 10));
				inputRecord.addColumn("phrase", StringOperator.getAName());
				inputRecords.add(inputRecord);
			}
			LOGGER.debug(String.format("conn.executeInsert(%s, inputRecords(size=%d))", dbName, inputRecords.size()));
			conn.executeInsert(dbName, inputRecords);
			sleep();
			/* 修改模式并提交请求 */
			LOGGER.debug(String.format("schema = trspermission.getSchema(%s)", schemaName));
			TRSSchema schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("[%s].setNodeList(%s)", schema.getName(), arrayToSet(newNodes)));
			schema.setNodeList(arrayToSet(newNodes));
			LOGGER.debug(String.format("[%s].setPartmain(%s)", schema.getName(), mainCopys));
			schema.setPartmain(mainCopys);
			LOGGER.debug(String.format("[%s].setPartcopy(%s)", schema.getName(), partCopys));
			schema.setPartcopy(partCopys);
			LOGGER.debug(String.format("trspermission.updateSchema(%s, true)", schema.getName()));
			/* 提交请求, 并重分布 */
			trspermission.updateSchema(schema, true);
			sleep();
			/* 等待重分布完成, 时间改成60秒, 保证冷子库存入HDFS */
			waitingUntilRedistributeCompleteOrTimeout(schemaName, 60);
			LOGGER.debug(String.format("schema = trspermission.getSchema(%s)", schemaName));
			schema = trspermission.getSchema(schemaName);
			LOGGER.debug(String.format("[%s].isReadwrite=%b", schema.getName(), schema.isReadwrite()));
			assertEquals(schema.isReadwrite(), true);
			assertEquals(schema.getPartmain(), mainCopys);
			assertEquals(schema.getPartcopy(), partCopys);
			/* 检查重分布完成后的模式信息和子库列表*/
			LOGGER.debug(String.format("actualSubdbs = getSubdbsFromDatabase(conn, %s)", dbName));
			List<Subdb> actualSubdbs = getSubdbsFromDatabase(conn, dbName);
			checkingActualSubdbsAndExpectedSubdbs(actualSubdbs, expectedSubdbs);
			/* 检索一条冷记录, 并获取uuid */
			/* 建表的时候固定子库打开数为3, 因此往前数4天一定是冷数据 */
			String coldDate = LocalDateTime.now().plusDays(-4).format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")).substring(0, 8);
			String query = "date:"+coldDate;
			SearchParams sp = new SearchParams();
			sp.setProperty("search.include.uncache", "true");
			LOGGER.debug(String.format("resultSet = conn.executeSelect(%s, %s, 0, 1, %s)", dbName, query, sp));
			TRSResultSet resultSet = conn.executeSelect(dbName, query, 0, 1, sp);
			resultSet.moveFirst();
			TRSRecord record = resultSet.get();
			String firstUid = record.getUid();
			/* 根据uuid修改记录 */
			inputRecords.clear();
			inputRecord = new TRSInputRecord();
			inputRecord.setUid(firstUid);
			inputRecord.addColumn("phrase", "修改记录");
			inputRecords.add(inputRecord);
			LOGGER.debug(String.format("conn.executeUpdate(%s, inputRecord(%d))", dbName, inputRecords.size()));
			/* 只要更新不抛异常即可 */
			conn.executeUpdate(dbName, inputRecords);
		} catch (TRSException e) {
			LOGGER.error(String.format("Case failure, code=%d, string=%s%s%s", 
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), Other.stackTraceToString(e)));
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				conn.deleteDatabase(dbName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
			sleep();
			try {
				trspermission.deleteSchema(schemaName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 检查实际的子库列表和预期的子库列表, 只检查subdbid, partid, ip这三个成员变量
	 * @param actual
	 * @param expected
	 */
	private static void checkingActualSubdbsAndExpectedSubdbs(List<Subdb> actual, List<Subdb> expected) {
		if(actual.size() != expected.size()) {
			LOGGER.error(String.format("实际的子库列表长度%d和预期子库列表长度%d不等", actual.size(), expected.size()));
			fail(String.format("实际的子库列表长度%d和预期子库列表长度%d不等", actual.size(), expected.size()));
		}
		Collections.sort(actual, new Comparator<Subdb>() {
			@Override
			public int compare(Subdb s1, Subdb s2) {
				return s1.subdbid.compareTo(s2.subdbid);
			}
		});
		Collections.sort(expected, new Comparator<Subdb>() {
			@Override
			public int compare(Subdb s1, Subdb s2) {
				return s1.subdbid.compareTo(s2.subdbid);
			}
		});
		for(int i=0, size=actual.size(); i<size; i++) {
			LOGGER.debug(String.format("i=%d, actualSubdb=%s, expectedSubdb=%s", i, actual.get(i), expected.get(i)));
			assertEquals(actual.get(i).subdbid, expected.get(i).subdbid);
			assertEquals(actual.get(i).partid, expected.get(i).partid);
			assertEquals(actual.get(i).ip, expected.get(i).ip);
		}
	}
	
	@DataProvider(name = "updateSchemaInfluenceMirrorDataProvider")
	public Object[][] updateSchemaInfluenceMirrorDataProvider(Method method){
		if(!"updateSchemaInfluenceMirror".equals(method.getName()))
			return null;
		
		/* 用例1 主分区映射表 */
		Map<String, List<Integer>> mainCopysCase1 = new HashMap<String, List<Integer>>();
		mainCopysCase1.put(SERVERS[0], Arrays.asList(1, 2));
		mainCopysCase1.put(SERVERS[1], Arrays.asList(3, 4));
		mainCopysCase1.put(SERVERS[2], Arrays.asList(5, 0));
		/* 用例1 副分区映射表 */
		Map<String, List<Integer>> partCopysCase1 = new HashMap<String, List<Integer>>();
		partCopysCase1.put(SERVERS[0], Arrays.asList(5, 0));
		partCopysCase1.put(SERVERS[1], Arrays.asList(1, 2));
		partCopysCase1.put(SERVERS[2], Arrays.asList(3, 4));
		String subdbid = null;
		String ip = SERVERS[2].substring(0, SERVERS[2].lastIndexOf(":"));
		/* 用例1 预期子库列表 */
		List<Subdb> expectedSubdbsCase1 = new ArrayList<Subdb>();
		expectedSubdbsCase1.add(Subdb.build().setPartid("000").setSubdbid("00000000").setIp(ip));
		expectedSubdbsCase1.add(Subdb.build().setPartid("000").setSubdbid("99999999").setIp(ip));
		for(int i=-2; i<=7; i++) {
			subdbid = LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")).substring(0, 8);
			expectedSubdbsCase1.add(Subdb.build().setPartid("000").setSubdbid(subdbid).setIp(ip));
		}
		
		/* 用例2 主分区映射表 */
		Map<String, List<Integer>> mainCopysCase2 = new HashMap<String, List<Integer>>();
		mainCopysCase2.put(SERVERS[1], Arrays.asList(1, 3, 5));
		mainCopysCase2.put(SERVERS[2], Arrays.asList(0, 2, 4));
		/* 用例2 副分区映射表 */
		Map<String, List<Integer>> partCopysCase2 = new HashMap<String, List<Integer>>();
		partCopysCase2.put(SERVERS[1], Arrays.asList(0, 2, 4));
		partCopysCase2.put(SERVERS[2], Arrays.asList(1, 3, 5));
		/* 用例2 预期子库列表 */
		ip = SERVERS[2].substring(0, SERVERS[2].lastIndexOf(":"));
		List<Subdb> expectedSubdbsCase2 = new ArrayList<Subdb>();
		expectedSubdbsCase2.add(Subdb.build().setSubdbid("00000000").setPartid("000").setIp(ip));
		expectedSubdbsCase2.add(Subdb.build().setSubdbid("99999999").setPartid("000").setIp(ip));
		for(int i=-3; i<=7; i++) {
			subdbid = LocalDateTime.now().plusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")).substring(0, 8);
			expectedSubdbsCase2.add(Subdb.build().setSubdbid(subdbid).setPartid("000").setIp(ip));
		}
		
		return new Object[][] {
			/* 镜像表的子库打开数和分时归档视图设置一致, 对于镜像表而言, 子库数量要受到子库打开数的影响 */
			new Object[] {1, SERVERS, 3, 12, SERVERS, mainCopysCase1, partCopysCase1, expectedSubdbsCase1},
			/* 镜像表的子库打开数比分时归档视图大1, 节点由3个缩减为2个, 改变000分区所在的节点 */
			new Object[] {2, SERVERS, 4, 13, new String[] {SERVERS[1], SERVERS[2]}, mainCopysCase2, partCopysCase2, expectedSubdbsCase2}
		};
	}
	
	/**
	 * 镜像表更新模式候的检查<br>
	 * 镜像表和数据重分布无关, 在数据重分布后根据主分区映射表后台完成同步<br>
	 * 为了方便检查, 测试方法固定让映射方式改为精确, 这样可以像检查自动分裂视图一样检查镜像表的子库分布<br>
	 * 源为均衡分时归档视图, 提供了17条记录, 每条记录都位于000区, 17条记录的分库字段各不相同, 因此会创建17个子库,
	 * 镜像表按照子库打开数的规定下载子库, 预期的子库列表要随子库打开数的设置变化<br>
	 * 比如令子库打开数为3, 则会下载到本地12个子库, 因此子库的预期列表要给出这12个子库<br>
	 * <ul>
	 * 	<li>前第8天 -> 子库一定是00000000, 因此是热数据</li>
	 * 	<li>前第7天 -> 冷数据 -> 同时它也是源分时归档视图属性 split.start.date </li>
	 *  <li>前第6天 -> 冷数据</li>
	 *  <li>前第5天 -> 冷数据</li>
	 *  <li>前第4天 -> 冷数据</li>
	 *  <li>前第3天 -> 冷数据</li>
	 *  <li>前天</li>
	 *  <li>昨天</li>
	 *  <li>今天</li>
	 *  <li>明天</li>
	 *  <li>后天</li>
	 *  <li>后第3天</li>
	 *  <li>后第4天</li>
	 *  <li>后第5天</li>
	 *  <li>后第6天</li>
	 *  <li>后第7天 -> 它是是源分时归档视图属性 split.date.end</li>
	 *  <li>后第8天 -> 子库一定是99999999</li>
	 * </ul>
	 * @param caseId
	 * @param mirrorSchemaNodes 镜像表所在模式的节点列表
	 * @param openSubdbNum 镜像表子库打开数
	 * @param expectedResultSetSize 预期镜像表的记录数
	 * @param newNodes 新节点列表
	 * @param mainCopys 主分区映射表
	 * @param partCopys 副分区映射表
	 * @param expectedSubdbs 预期的子库列表, 只检查partid, subdbid, ip三个成员变量
	 */
	@SuppressWarnings("resource")
	@Test(dataProvider = "updateSchemaInfluenceMirrorDataProvider")
	public void updateSchemaInfluenceMirror(
			int caseId, String[] mirrorSchemaNodes, int openSubdbNum, int expectedResultSetSize,
			String[] newNodes, Map<String, List<Integer>> mainCopys, Map<String, List<Integer>> partCopys,
			List<Subdb> expectedSubdbs) {
		LOGGER.debug("updateSchemaInfluenceMirror, caseId="+caseId);
		String originSchemaName = "origin_"+System.currentTimeMillis();
		String mirrorSchemaName = "mirror_"+System.currentTimeMillis();
		int partNum = 6;
		String originDbName = originSchemaName + "." + "fifo_" + System.currentTimeMillis();
		String mirrorDbName = mirrorSchemaName + "." + "mirror_" + System.currentTimeMillis();
		try {
			/*新建模式*/
			LOGGER.debug(String.format("trspermission.createSchema(%s, %d, %s)", originSchemaName, partNum, Arrays.asList(SERVERS)));
			trspermission.createSchema(originSchemaName, partNum, Arrays.asList(SERVERS));
			LOGGER.debug(String.format("trspermission.createSchema(%s, %d, %s)", mirrorSchemaName, partNum, Arrays.asList(mirrorSchemaNodes)));
			trspermission.createSchema(mirrorSchemaName, partNum, Arrays.asList(mirrorSchemaNodes));
			sleep();
			/*建源表*/
			createASimpleFifoView(conn, originDbName, DBPOLICY.NORMAL);
			sleep();
			/*向源插入记录*/
			List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>();
			TRSInputRecord inputRecord = null;
			String dateTimeStr = null;
			for(int i=-8; i<=8; i++) {
				inputRecord = new TRSInputRecord();
				inputRecord.addColumn("rowid", "");
				inputRecord.addColumn("number", String.valueOf(i));
				dateTimeStr = LocalDateTime.now().plusDays(i).format(DATE_TIME_FORMATTER).substring(0, 10);
				inputRecord.addColumn("date", dateTimeStr);
				inputRecord.addColumn("phrase", StringOperator.getAName());
				inputRecords.add(inputRecord);
			}
			LOGGER.debug(String.format("conn.executeInsert(%s, inputRecords(size=%d))", originDbName, inputRecords.size()));
			conn.executeInsert(originDbName, inputRecords);
			sleep();
			/*创建镜像表*/
			TRSDatabase mirror = new TRSDatabase(mirrorDbName, TRSDatabase.TYPE_MIRROR, TRSDatabase.DBPOLICY.FASTEST);
			/*镜像表映射方式锁定为严格映射, 这样方便检查子库列表*/
			mirror.setProperty("mirrordb", originDbName);
			mirror.setProperty("open.subdb.num", String.valueOf(openSubdbNum));
			mirror.setProperty("mirror.copy", "exact");
			conn.createDatabase(mirror);
			sleep();
			/*等待镜像表同步, */
			SearchParams sp = new SearchParams();
			sp.setSortMethod("+date");
			TRSResultSet resultSet = conn.executeSelect(mirrorDbName, "rowid:*", 0, 10000, sp);
			int loop = 120;
			while(resultSet.size() < expectedResultSetSize && loop -- > 0) {
				sleep(1000L);
				LOGGER.debug(String.format("loop=%d, resultSet = conn.executeSelect(%s, \"rowid:*\", 0, 10000, %s)", loop, mirrorDbName, sp));
				resultSet = conn.executeSelect(mirrorDbName, "rowid:*", 0, 10000, sp);
				LOGGER.debug("resultSet.size="+resultSet.size());
			}
			LOGGER.debug(String.format("resultSet.size=%d, expectedResultSetSize=%d", resultSet.size(), expectedResultSetSize));
			assertEquals(resultSet.size(), expectedResultSetSize);
			/*更改镜像表的模式主副分区映射并提交重分布请求*/
			LOGGER.debug(String.format("TRSSchema mirrorSchema = trspermission.getSchema(%s)", mirrorSchemaName));
			TRSSchema mirrorSchema = trspermission.getSchema(mirrorSchemaName);
			LOGGER.debug(String.format("mirrorSchema.setNodeList(%s)", arrayToSet(newNodes)));
			mirrorSchema.setNodeList(arrayToSet(newNodes));
			LOGGER.debug(String.format("mirrorSchema.setPartmain(%s)", mainCopys));
			mirrorSchema.setPartmain(mainCopys);
			LOGGER.debug(String.format("mirrorSchema.setPartcopy(%s)", partCopys));
			mirrorSchema.setPartcopy(partCopys);
			LOGGER.debug(String.format("trspermission.updateSchema(%s)", mirrorSchema));
			trspermission.updateSchema(mirrorSchema);
			/*数据重分布完成后镜像表才开始同步, 所以要等待一段时间, 等待120秒*/
			loop = 120;
			while(loop -- > 0) {
				sleep(1000L);
				LOGGER.debug(String.format("loop=%d", loop));
			}
			/*检查子库列表*/
			List<Subdb> actualSubdbs = getSubdbsFromDatabase(conn, mirrorDbName);
			checkingActualSubdbsAndExpectedSubdbs(actualSubdbs, expectedSubdbs);
		} catch (TRSException e) {
			LOGGER.error(String.format("errorCode=%d, errorString=%s%s%s", 
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), Other.stackTraceToString(e)));
			fail(Other.stackTraceToString(e));
		} finally{
			try {
				conn.deleteDatabase(mirrorDbName);
			} catch (TRSException e2) {
				e2.printStackTrace();
			}
			try {
				conn.deleteDatabase(originDbName);
			} catch (TRSException e2) {
				e2.printStackTrace();
			}
			sleep();
			try {
				trspermission.deleteSchema(mirrorSchemaName);
			} catch (TRSException e1) {
				e1.printStackTrace();
			}
			try {
				trspermission.deleteSchema(originSchemaName);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 发送获取数据库子库的命令
	 * @param conn
	 * @param databaseName
	 * @return
	 * @throws TRSException
	 */
	private static String getSubdbs(TRSConnection conn, String databaseName) throws TRSException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("dbname", databaseName);
		params.put("subdb.location.output", "true");
		return conn.executeCommand("list_subdb", params);
	}
	
	/**
	 * 静态内部类, 负责记录 executeCommand() 返回的单个子库信息
	 * @author trs
	 *
	 */
	private static class Subdb{
		private String subdbid;
		private String partid;
		private String dbname;
		private String ip;
		private String port;
		@Override
		public String toString() {
			return "Subdb [subdbid=" + subdbid + ", partid=" + partid + ", dbname=" + dbname + ", ip=" + ip + ", port="
					+ port + "]";
		}
		public static Subdb build() {
			return new Subdb();
		}
		public Subdb setSubdbid(String subdbid) {
			this.subdbid = subdbid;
			return this;
		}
		public Subdb setPartid(String partid) {
			this.partid = partid;
			return this;
		}
		public Subdb setIp(String ip) {
			this.ip = ip;
			return this;
		}
	}
	
	/**
	 * 用于验证模式基本操作的建表操作
	 * @param conn
	 * @param name
	 * @param type
	 * @param policy
	 * @return
	 * @throws TRSException
	 */
	private static boolean createDemoDatabase(TRSConnection conn, String name, int type, DBPOLICY policy) throws TRSException {
		TRSDatabase db = new TRSDatabase(name, type, policy);
		db.addColumn(new TRSDatabaseColumn("rowid", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("日期", TRSDatabaseColumn.TYPE_DATE));
		db.addColumn(new TRSDatabaseColumn("版次", TRSDatabaseColumn.TYPE_NUMBER));
		db.addColumn(new TRSDatabaseColumn("版名", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("标题", TRSDatabaseColumn.TYPE_PHRASE));
		db.addColumn(new TRSDatabaseColumn("作者", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("正文", TRSDatabaseColumn.TYPE_DOCUMENT));
		db.setParter("rowid");
		Map<String,String> splitMap = new HashMap<String,String>();
		splitMap.put("max.split.num", "10");
		db.setSplitter("number", "版次", splitMap);
		db.setDefSearchColumn("正文");
		return conn.createDatabase(db);
	}
	
	/**
	 * 创建一个简单分时归档视图<br>
	 * start = 今天(不含)前第7天<br>
	 * end = 今天(不含)后第7天<br>
	 * 子库打开数 = 3<br>
	 * @param conn
	 * @param name
	 * @param policy
	 * @return
	 * @throws TRSException
	 */
	private static boolean createASimpleFifoView(TRSConnection conn, String name, DBPOLICY policy) throws TRSException{
		TRSDatabase db = new TRSDatabase(name, TRSDatabase.TYPE_FIFO, policy);
		db.addColumn(new TRSDatabaseColumn("rowid", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("date", TRSDatabaseColumn.TYPE_DATE));
		db.addColumn(new TRSDatabaseColumn("number", TRSDatabaseColumn.TYPE_NUMBER));
		db.addColumn(new TRSDatabaseColumn("phrase", TRSDatabaseColumn.TYPE_PHRASE));
		db.addColumn(new TRSDatabaseColumn("document", TRSDatabaseColumn.TYPE_DOCUMENT));
		db.setParter("rowid");
		Map<String, String> splitMap = new HashMap<String, String>();
		String[] dateRange = decideDateRange();
		splitMap.put("split.date.start", dateRange[0]);
		splitMap.put("split.date.end", dateRange[1]);
		splitMap.put("split.date.level", "day");
		db.setSplitter("date", "date", splitMap);
		db.setProperty("open.subdb.num", "3");
		LOGGER.debug(String.format("conn.createDatabase(%s)", name));
		return conn.createDatabase(db);
	}
	
	private static String[] decideDateRange() {
		LocalDateTime now = LocalDateTime.now();
		String start = now.plusDays(-7).format(DATE_TIME_FORMATTER).substring(0, 10);
		String end = now.plusDays(7).format(DATE_TIME_FORMATTER).substring(0, 10);
		return new String[] {start, end};
	}
}
