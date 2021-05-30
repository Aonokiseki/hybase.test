package com.trs.hybase.test.old;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.annotations.AfterClass;
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
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSSchema;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.TRSUser.CONSOLEPermission;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
import com.trs.hybase.test.util.Other;

public class TRSUserTest {
	private final static Logger LOGGER = Logger.getLogger(TRSUserTest.class);
	private TRSConnection conn;
	private TRSPermissionClient trspermission;
	
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
	
	private final static String[] SERVERS = new String[] {
			"192.168.105.190:5555",
			"192.168.105.191:5555",
			"192.168.105.192:5555"
	};
	
	private final static String[] USED_SCHEMAS = new String[] {
		"first", "second", "third"
	};
	/**
	 * String 类型参数, array 转 string(间隔符为英文半角逗号)
	 * @param array
	 * @return
	 */
	private static String arrayToString(String[] array) {
		if(array == null || array.length == 0)
			return "";
		if(array.length == 1)
			return array[0];
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<array.length; i++) {
			sb.append(array[i]);
			if(i < array.length - 1)
				sb.append(",");
		}
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
	
	@BeforeClass
	public void beforeClass() {
		PropertyConfigurator.configure("./log4j.properties");
		LOGGER.debug("TRSUserTest, APIVersion="+APIVersion.getVersion());
	}
	
	@BeforeMethod
	public void beforeMethod() {
		conn = new TRSConnection("http://"+SERVERS[0], "admin", "trsadmin", new ConnectParams());
		LOGGER.debug(String.format("beforeMethod: trspermission = new TRSPermissionClient(%s)", conn.getURL()));
		trspermission = new TRSPermissionClient(conn);
		for(int i=0; i<USED_SCHEMAS.length; i++) {
			try {
				if(trspermission.getSchema(USED_SCHEMAS[i]) == null) {
					LOGGER.debug(String.format("beforeMethod: trspermission.createSchema(%s, 6, %s)", 
							USED_SCHEMAS[i], Arrays.asList(SERVERS)));
					trspermission.createSchema(USED_SCHEMAS[i], 6, Arrays.asList(SERVERS));
				}
			} catch (TRSException e) {}
		}
		sleep();
	}
	
	@AfterMethod
	public void afterMethod() {
		if(conn != null)
			conn.close();
		sleep();
	}
	
	@AfterClass
	public void afterClass() {
		for(int i=0; i<USED_SCHEMAS.length; i++)
			try {
				LOGGER.debug(String.format("afterClass: trspermission.deleteSchema(%s)", USED_SCHEMAS[i]));
				trspermission.deleteSchema(USED_SCHEMAS[i]);
			} catch (TRSException e) {}
		if(trspermission != null)
			trspermission.close();
	}
	
	@DataProvider(name = "createUserDataProvider")
	public Object[][] createUserDataProvider(Method method){
		if(!"createUser".equals(method.getName()))
			return null;
		return new Object[][] {
			/*
			 * new Object[]{用例编号, 用户名, 密码, 赋予(多个)模式, 赋予默认模式, 备注, 设置是否可用, 
			 * 			预期创建用户成功, 预期从服务器获取的模式, 预期默认模式, 预期错误号}
			 */
			/* 正常情况 */
			/* 一个普通用户 */
			new Object[] {1, "normal", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "一个普通用户", true, 
					true, USED_SCHEMAS, USED_SCHEMAS[0], Integer.MIN_VALUE},
			/* 禁用普通用户 */
			new Object[] {2, "disable", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "禁用", false, 
					true, USED_SCHEMAS, USED_SCHEMAS[0], Integer.MIN_VALUE},
			/* 用户名64位, 密码64位,备注64位 单个节点 */
			new Object[] {3, "1234567890123456789012345678901234567890123456789012345678901234", 
					/*密码=*/"123456789012345678901234567890123456789012345678901234567890123a", 
					new String[] {USED_SCHEMAS[0]}, USED_SCHEMAS[0],
					"1234567890123456789012345678901234567890123456789012345678901234", false, 
					true, new String[] {USED_SCHEMAS[0]}, USED_SCHEMAS[0], Integer.MIN_VALUE},
			/* 模式只给一个，默认模式不给，自动赋值 */
			new Object[] {4, "oneschema", "1234qwer", new String[] {USED_SCHEMAS[0]}, /*给定默认模式:*/"", "不给默认模式", true, 
					true, new String[] {USED_SCHEMAS[0]}, /*服务器端获取默认模式:*/USED_SCHEMAS[0], Integer.MIN_VALUE},
			/* 模式给两个, 其它和4一样 */
			new Object[] {5, "twoschema", "1234qwer", new String[] {USED_SCHEMAS[1], USED_SCHEMAS[2]}, 
					/*给定默认模式:*/"", "仍然不给默认模式", false, 
					true, new String[] {USED_SCHEMAS[1], USED_SCHEMAS[2]}, 
					/*服务器端获取默认模式:*/USED_SCHEMAS[2], Integer.MIN_VALUE},
			
			/* 异常情况, 预期要抛出异常, 后续预期均可给null*/
			/* 用户名65位*/
			new Object[] {6, "12345678901234567890123456789012345678901234567890123456789012345", 
					"1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "名字很长", true, 
					false, null, null, 9999999},
			/* 用户名为空 */
			new Object[] {7, "", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "无名氏", true, 
					false, null, null, 9999999},
			/* 用户名全是空格 */
			new Object[] {8, "        ", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "无名氏", true, 
					false, null, null, 9999999},
			/* 用户名的画风有些奇怪 */
			new Object[] {9, "_(:з」∠)_", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "混进来一个奇怪的用户", true, 
					false, null, null, 9999999},
			/* 用户名是汉字 */
			new Object[] {10, "皇帝", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "他要当皇帝", true, 
					false, null, null, 9999999},
			/* 过于复杂的密码 */
			new Object[] {11, "long_passwd", "12345678901234567890123456789012345678901234567890123456789012345", 
					USED_SCHEMAS, USED_SCHEMAS[0], "你以为用了这么长的密码就破译不了么", true, 
					false, null, null, 9920108},
			/* 空密码 */
			new Object[] {12, "empty_passwd", "", USED_SCHEMAS, USED_SCHEMAS[0], "空密码", true, 
					false, null, null, 9920108},
			/* 短密码 */
			new Object[] {13, "short_passwd", "trs123", USED_SCHEMAS, USED_SCHEMAS[0], "短密码", true, 
					false, null, null, 9920108},
			/* 纯数字密码 */
			new Object[] {14, "num_passwd", "12345678", USED_SCHEMAS, USED_SCHEMAS[0], "数字密码", true, 
					false, null, null, 9920108},
			/* 纯英文密码 */
			new Object[] {15, "eng_passwd", "qwertyui", USED_SCHEMAS, USED_SCHEMAS[0], "字母密码", true, 
					false, null, null, 9920108},
			/* 汉字密码 */
			new Object[] {16, "chs_passwd", "我是密码", USED_SCHEMAS, USED_SCHEMAS[0], "汉字密码", true, 
					false, null, null, 9920108},
			/* 混进去了一些符号 */
			new Object[] {17, "otherpasswd", "_(:з」∠)_1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], "密码的画风不对", true, 
					false, null, null, 9920108},
			/* 备注太长 */
			new Object[] {18, "common", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], 
					"12345678901234567890123456789012345678901234567890123456789012345", true, 
					false, null, null, 9920106},
			
			/* 默认模式在系统中不存在 */
			new Object[] {19, "noexist", "1234qwer", USED_SCHEMAS, 
					/*默认加上时间戳保证不存在*/"schema_"+System.currentTimeMillis(), 
					"默认模式在系统中不存在", true, 
					false, null, null, 9920034},
			/* 默认模式确实在系统中存在, 但是用户没有这个模式, 相当于给了用户此模式*/
			new Object[] {20, "noexist", "1234qwer", new String[] {USED_SCHEMAS[0], USED_SCHEMAS[1]}, USED_SCHEMAS[2],
					"默认模式不在该用户的模式列表中", true, 
					true, USED_SCHEMAS, USED_SCHEMAS[2], 9920034},
			/* 其中一个模式不存在 */
			new Object[] {21, "noexist", "1234qwer", new String[] {USED_SCHEMAS[0], "unknown"+System.currentTimeMillis()}, USED_SCHEMAS[0],
					"模式列表中有不存在的模式", true, 
					false, null, null, 9920034}
		};
	}
	/**
	 * 简单验证创建用户, 仅检查信息, 无后续操作<br>
	 */
	@Test(dataProvider = "createUserDataProvider")
	public void createUser(int caseId, 
			String userName, String password, String[] schemas, String defschema, String comment, boolean enable,
			boolean expectedCreateUserSuccess, String[] expectedSchemas, String expectedDefaultSchema, int expectedErrorCode) {
		LOGGER.debug("createUser, caseId="+caseId);
		try {
			try {
				TRSSchema trsSchema = null;
				for(int i=0; i<schemas.length; i++) {
					trsSchema = trspermission.getSchema(schemas[i]);
					if(trsSchema == null)
						LOGGER.warn(String.format("%s not exist, be aware of your case!", schemas[i]));
					else
						LOGGER.debug(String.format("trspermission.getSchema -> schemaName=%s, nodes=%s", trsSchema.getName(), trsSchema.getNodeList()));
				}
				LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, %s, %s, %b)", 
						userName, password, arrayToString(schemas), defschema, comment, enable));
				trspermission.createUser(userName, password, arrayToString(schemas), defschema, comment, enable);
				if(!expectedCreateUserSuccess)
					fail(String.format("预期创建用户[%s]失败, 但是却成功了", userName));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode==%d, expected==%d, errorString=%s", 
						e.getErrorCode(),  expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			sleep();
			LOGGER.debug(String.format("trspermission.getUserInfo(%s)", userName));
			TRSUser user = trspermission.getUserInfo(userName);
			assertEquals(user.getName(), userName);
			Set<String> userSchemas = user.getSchemas();
			LOGGER.debug("user.getSchemas="+userSchemas);
			for(int i=0; i<expectedSchemas.length; i++)
				userSchemas.remove(expectedSchemas[i]);
			assertTrue(userSchemas.isEmpty());
			assertEquals(user.getDefSchema(), expectedDefaultSchema);
			assertEquals(user.getComment(), comment);
			assertEquals(user.isEnable(), enable);
			/* 这里不真的验证用户可以登录和操作, 因为还得更新API权限 */
		} catch (TRSException e) {
			LOGGER.error(String.format("Case failure, code=%d, message=%s%sstack=", 
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), Other.stackTraceToString(e)));
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(userName);
			}catch(TRSException e) {}
		}
	}
	
	/**
	 * 创建用户同名抛出异常
	 */
	@Test
	public void createUserDuplication() {
		LOGGER.debug("createUserDuplication");
		String name = "common_"+System.currentTimeMillis();
		String password = "1234qwer";
		try {
			trspermission.createUser(name, password, "system", "system", "", true);
			sleep();
			try {
				trspermission.createUser(name, password, "system", "system", "", true);
			}catch(TRSException e) {
				assertEquals(e.getErrorCode(), 9920034);
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(name);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "createUserComplexDataProvider")
	public Object[][] createUserComplexDataProvider(Method method){
		if(!"createUserComplex".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 新建一个普通用户, 能完成表和记录的基本操作 */
			new Object[] {
				/* 新建用户 */
				1, "standard", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], true,
				/* 新建用户登录 */
				"standard", "1234qwer",
				/* 建表预期 */
				"standardFirst", true, Integer.MIN_VALUE,
				/* 入库预期 */
				"standardFirst", "." + "/TRSUser/demo.trs", true, Integer.MIN_VALUE,
				/* 检索记录预期 */
				"standardFirst", true, Integer.MIN_VALUE
			},
			
			/* 直接禁用用户 */
			new Object[] {
				2, "init", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], false,
				/* 新建用户登录(其实此时还没发送请求,要等建表才行) */
				"init", "1234qwer",
				/* 建表预期异常 */
				"first.initFirst", false, 403,
				/* 剩下的参数用不上 */
				"", "", false, Integer.MIN_VALUE,
				"", false, Integer.MIN_VALUE
			},
			
			/* 使用错误的用户名 */
			new Object[] {
				3, "init", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], false,
				/* 新建用户登录(其实此时还没发送请求,要等建表才行) */
				"UserNameNotExist", "1234qwer",
				/* 建表预期异常 */
				"first.initFirst", false, 401,
				/* 剩下的参数用不上 */
				"", "", false, Integer.MIN_VALUE,
				"", false, Integer.MIN_VALUE
			},
			
			/* 使用错误的密码 */
			new Object[] {
				4, "init", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], false,
				/* 新建用户登录(其实此时还没发送请求,要等建表才行) */
				"init", "qwer1234",
				/* 建表预期异常 */
				"first.initFirst", false, 401,
				/* 剩下的参数用不上 */
				"", "", false, Integer.MIN_VALUE,
				"", false, Integer.MIN_VALUE
			},
			
			/* 使用没有给的模式强行建表 */
			new Object[] {
				/* 新建用户 */
				5, "common", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], true,
				/* 新建用户登录 */
				"common", "1234qwer",
				/* 建表预期(模式名为时间戳, 保证不曾存在) */
				System.currentTimeMillis() + ".standardFirst", false, 9920500,
				/* 后续无需验证 */
				"", "", false, Integer.MIN_VALUE,
				"", false, Integer.MIN_VALUE
			},
			
			/* 使用没有给的模式表强行入库, 被当成表不存在了 */
			new Object[] {
				/* 新建用户 */
				6, "loadrecords", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], true,
				/* 新建用户登录 */
				"loadrecords", "1234qwer",
				/* 建表预期(强行指定第二个模式) */
				"second.searchFirst", true, Integer.MIN_VALUE,
				/* 入库, 换成其它模式被当成了表不存在异常 */
				/* 等一下, 是不是还有一种情况是表存在但就是没给用户的异常 */
				"third.searchFirst", "." + "/TRSUser/demo.trs", false, 9918025,
				/* 后边参数用不上 */
				"", false, Integer.MIN_VALUE
			},
			
			/* 使用没有给的模式强行入库, 这次保证表存在, 但就是没赋予用户 */
			new Object[] {
				/* 新建用户 */
				7, "loadrecords", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], true,
				/* 新建用户登录 */
				"loadrecords", "1234qwer",
				/* 建表预期(强行指定第3个模式) */
				"third.searchFirst", true, Integer.MIN_VALUE,
				/* 入库, 直接向system.demo表入库*/
				"system.demo", "." + "/TRSUser/demo.trs", false, 9920500,
				/* 后边参数用不上 */
				"", false, Integer.MIN_VALUE
			},
			
			/* 使用没有给的模式强行检索 */
			new Object[] {
				/* 新建用户 */
				8, "search", "1234qwer", USED_SCHEMAS, USED_SCHEMAS[0], true,
				/* 新建用户登录 */
				"search", "1234qwer",
				/* 建表预期(强行指定第3个模式) */
				"third.searchFirst", true, Integer.MIN_VALUE,
				/* 入库, 直接向system.demo表入库*/
				"third.searchFirst", "." + "/TRSUser/demo.trs", true, Integer.MIN_VALUE,
				/* 检索没有被给予的表 */
				"system.entity", false, 9920500
			}
		};
	}
	/**
	 * 添加用户, 并使用新建用户对表和记录做操作, 就为了验证赋予用户的模式可用, enable参数有效
	 * @param caseId 
	 * @param userName 新建用户名
	 * @param password 用户密码
	 * @param schemas 赋予用户的模式
	 * @param defschema 赋予用户的默认模式
	 * @param enable 是否使用
	 * @param loginUserName 登录时填写的用户名
	 * @param loginUserPasswd 登录时填写的用户密码
	 * @param createDatabaseName 创建数据库时用的名字
	 * @param expectedCreateDbSucceed 预期创建数据库是否成功
	 * @param expectedCreateErrorCode 创建数据库失败时预期的异常号
	 * @param loadRecordsDatabaseName 记录入库填写的数据库名
	 * @param loadFilePath 入库文件路径
	 * @param expectedLoadRecordsSucceed 预期入库是否成功
	 * @param expectedLoadRecordsErrorCode 入库异常时，预期的异常号
	 * @param searchDatabase 检索的数据库名
	 * @param searchSucceed 预期检索是否成功
	 * @param expectedSearchErrorCode 检索异常时，预期的异常号
	 */
	@Test(dataProvider = "createUserComplexDataProvider")
	public void createUserComplex(int caseId, 
							   String userName, 
							   String password, 
							   String[] schemas, 
							   String defschema, 
							   boolean enable,
							   String loginUserName,
							   String loginUserPasswd,
							   String createDatabaseName,
							   boolean expectedCreateDbSucceed,
							   int expectedCreateErrorCode,
							   String loadRecordsDatabaseName,
							   String loadFilePath,
							   boolean expectedLoadRecordsSucceed,
							   int expectedLoadRecordsErrorCode,
							   String searchDatabase,
							   boolean searchSucceed,
							   int expectedSearchErrorCode) {
		LOGGER.info("createUserComplex, caseId="+caseId);
		try {
			/* 新建用户 */
			trspermission.createUser(userName, password, arrayToSet(schemas), defschema, String.format("caseId=%d", caseId), enable);
			sleep();
			/* 赋予API所有权限 */
			TRSUser user = trspermission.getUserInfo(userName);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(userName, user);
			sleep();
			/* 使用新建的用户登录 */
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], loginUserName, loginUserPasswd, new ConnectParams());
			/* 因为权限设置是全部, 所以不会因为权限问题抛出异常
			 * 会抛出异常会有几个主要原因:
			 * 1. 用户被禁用
			 * 2. 建表的模式不对  */
			try {
				createDemoDatabase(_conn, createDatabaseName);
				if(!expectedCreateDbSucceed)
					fail(String.format("用户[%s]创建表[%s]预期抛出异常,实际创建成功", loginUserName, createDatabaseName));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedCreateErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedCreateErrorCode);
				_conn.close();
				return;
			}
			sleep();
			/* 尝试记录入库 */
			try {
				_conn.loadRecords(loadRecordsDatabaseName, loadFilePath, 0);
				if(!expectedLoadRecordsSucceed)
					fail(String.format("用户[%s]向表[%s]入库预期抛出异常, 实际未抛出", loginUserName, loadRecordsDatabaseName));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedLoadRecordsErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedLoadRecordsErrorCode);
				_conn.close();
				return;
			}
			sleep();
			/* 检索记录 */
			try {
				_conn.executeSelect(searchDatabase, "rowid:*", 0, 10, new SearchParams());
				if(!searchSucceed)
					fail(String.format("用户[%s]检索[%s]预期抛出异常,但成功了", loginUserName, searchDatabase));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expectedSearchErrorCode=%d, getErrorString=%s", 
						e.getErrorCode(), expectedSearchErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedSearchErrorCode);
				_conn.close();
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				conn.deleteDatabase(createDatabaseName);
			}catch(TRSException e) {}
			sleep();
			try {
				trspermission.deleteUser(userName);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider(name = "deleteUserDataProvider")
	public Object[][] deleteUserDataProvider(Method method){
		if(!"deleteUser".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 正常情况, 删除一个存在的用户 */
			new Object[] {1, "createfirst", "createfirst", true, Integer.MIN_VALUE},
			
			/* 删除一个不存在的用户, 不会抛出异常, 只要用户被删除即可 */
			new Object[] {2, "createsecond", "deletefirst", true, Integer.MIN_VALUE},
			/* 删除admin用户, 抛出异常 */
			/* 2021-1-13: 但并没有抛出异常, admin用户也没有删除, 这样也可以接受 */
			/* 2021.4.9: 决定提交到禅道上 */
			new Object[] {3, "normal", "admin", false, 9900034}
		};
	}
	/**
	 * 删除用户
	 * @param caseId
	 * @param createUserName
	 * @param deleteUserName
	 * @param expectedSucceed
	 * @param expectedErrorCode
	 */
	@Test(dataProvider = "deleteUserDataProvider")
	public void deleteUser(int caseId, String createUserName, String deleteUserName, boolean expectedSucceed, int expectedErrorCode) {
		LOGGER.debug("deleteUser, caseId="+caseId);
		try {
			trspermission.createUser(createUserName, "1234qwer", arrayToString(USED_SCHEMAS), USED_SCHEMAS[0], "", true);
			sleep();
			try {
				trspermission.deleteUser(deleteUserName);
				if(!expectedSucceed)
					fail(String.format("预期删除用户[%s]抛出异常, 实际未抛出", deleteUserName));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			sleep();
			/* 既然已删除了用户, 此处再检索时预期抛出异常, 没抛出反倒是错误的现象 */
			try {
				trspermission.getUserInfo(deleteUserName);
				fail(String.format("预期获取用户[%s]抛出异常, 实际未抛出", deleteUserName));
			}catch(TRSException e) {
				return;
			}
		} catch (TRSException e) {
			LOGGER.error(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(createUserName);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider (name = "adminChangePwdDataProvider")
	public Object[][] adminChangePwdDataProvider(Method method){
		if(!"adminChangePwd".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 新建普通用户, 管理员修改密码后普通用户登陆获取数据库 */
			new Object[] {1, "zehir", "1234qwer", "zehir", "qwer1234", true, Integer.MIN_VALUE, "zehir", "qwer1234", true, Integer.MIN_VALUE},
			/* 使用修改前的密码登录 */
			new Object[] {2, "godric", "1234qwer", "godric", "qwer1234", true, Integer.MIN_VALUE, "godric", "1234qwer", false, 401},
			/* 修改的用户不存在, 在adminChangePwd()方法抛出异常 */
			new Object[] {3, "freyda", "1234qwer", "klaus", "qwer1234", false, 9999999, "", "", false, Integer.MIN_VALUE},
		};
	}
	/**
	 * 管理员修改其它用户密码
	 * @param caseId
	 * @param userName
	 * @param createPasswd
	 * @param modifyName 欲修改的用户的名字
	 * @param newPasswd
	 * @param expectedChangeSucceed 预期修改密码是否成功
	 * @param expectedChangeErrorCode 修改密码异常时, 预期的错误号
	 * @param loginName 普通用户的登录名
	 * @param loginPasswd 普通用户的登录密码
	 * @param expectedLoginSucceed
	 * @param expectedErrorCode
	 */
	@Test(dataProvider = "adminChangePwdDataProvider")
	public void adminChangePwd(
			int caseId, String userName, String createPasswd, 
			String modifyName, String newPasswd, 
			boolean expectedChangeSucceed, int expectedChangeErrorCode,
			String loginName, String loginPasswd, 
			boolean expectedLoginSucceed, int expectedErrorCode) {
		LOGGER.debug("adminChangePwd, caseId="+caseId);
		try {
			/* 管理员新建用户 */
			LOGGER.debug(String.format("createUser(%s, %s, system, system, comment, true)", userName, createPasswd));
			trspermission.createUser(userName, createPasswd, "system", "system", "passwd="+createPasswd, true);
			sleep();
			/* 管理更新用户权限 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", userName));
			TRSUser user = trspermission.getUserInfo(userName);
			LOGGER.debug("user.addApiPermission(APIPermission.all)");
			user.addApiPermission(APIPermission.all);
			LOGGER.debug(String.format("trspermission.updateUser(%s, user)", userName));
			trspermission.updateUser(userName, user);
			sleep();
			/* 管理员修改用户密码 */
			/* 2020-01-19: 这个方法不会自动转换用户名为小写, 已提交至禅道, 待修复后去掉 .toLowerCase() */
			/* 2020-03-29: 已经修复 */
			LOGGER.debug(String.format("adminChangePwd(%s, %s)", modifyName.toLowerCase(), newPasswd));
			try {
				trspermission.adminChangePwd(modifyName/*.toLowerCase()*/, newPasswd);
				if(!expectedChangeSucceed)
					fail(String.format("admin修改了用户[%s]的密码为[%s], 预期修改失败, 但是成功了", modifyName, newPasswd));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedChangeErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedChangeErrorCode);
				return;
			}
			sleep();
			/* 普通用户(这个普通用户的名字由参数给出, loginName和userName一致就是刚被创建的用户登录, 否则就是一个其他用户登录)
			 * 登录, 然后尝试获取数据库列表 */
			/* 2020-1-19: 构造器也有没转换到小写这个问题 */
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], loginName/*.toLowerCase()*/, loginPasswd, new ConnectParams());
			try {
				_conn.getDatabases();
				if(!expectedLoginSucceed)
					fail(String.format("用户[%s]使用密码[%s]登录却成功了",  loginName, loginPasswd));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
			}finally {
				_conn.close();
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(userName);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider(name = "changePwdDataProvider")
	public Object[][] changePwdDataProvider(Method method){
		if(!"changePwd".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 普通用户修改密码登录系统获取数据库列表 */
			new Object[] {1, "godric", "1234qwer", "1234qwer", "qwer1234", "qwer1234", true, Integer.MIN_VALUE},
			/* 改成和原来一样的密码 */
			new Object[] {2, "zehir", "1234qwer", "1234qwer", "1234qwer", "1234qwer", true, Integer.MIN_VALUE},
			/* 普通用户修改密码时填写了错误的旧密码 */
			new Object[] {3, "findan", "1234qwer", "1q2w3e4r", "1234qwer", "1234qwer", false, 9999999},
			/* 普通用户修改密码时填写了不合法的密码 */
			/* 2021.5.10 错误号怎么是-1??? 需要问开发组 */
			/* 2021.5.12 就是-1*/
			new Object[] {4, "agreal", "1234qwer", "1234qwer", "", "", false, 9999999},
			/* 不存在的用户(相当于修改其它用户密码, 不放到这里检查) */
		};
	}
	/**
	 * 普通用户修改自己的密码<br/>
	 * (修改其它人的密码见 forceCommonUserChangeOthersPassword() 方法)
	 * @param caseId
	 * @param userName
	 * @param realOldPasswd 管理员设定好的旧密码
	 * @param inputOldPasswd 普通用户记忆中的旧密码
	 * @param newPasswd
	 * @param loginPasswd
	 * @param expectedChangePwdSucceed
	 * @param expectedErrorCode
	 */
	@Test(dataProvider = "changePwdDataProvider")
	public void changePwd(int caseId, String userName, String realOldPasswd, String inputOldPasswd, String newPasswd, 
			String loginPasswd, boolean expectedChangePwdSucceed, int expectedErrorCode) {
		LOGGER.debug(String.format("changePwd, caseid=%s", caseId));
		try {
			/* 管理员创建用户  */
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, system, system, comment, true)", userName, realOldPasswd));
			trspermission.createUser(userName, realOldPasswd, "system", "system", "自行修改密码", true);
			sleep();
			/* 管理员修改普通用户权限 */
			TRSUser user = trspermission.getUserInfo(userName);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(userName, user);
			sleep();
			/* 普通用户登录 */
			LOGGER.debug(String.format("_conn = new TRSConnection(host, %s, %s, new ConnectParams())", userName, realOldPasswd));
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], userName, realOldPasswd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			try {
				/* 普通用户修改密码 */
				LOGGER.debug(String.format("_trspermission.changePwd(%s, %s, %s)", userName, inputOldPasswd, newPasswd));
				_trspermission.changePwd(userName, inputOldPasswd, newPasswd);
				if(!expectedChangePwdSucceed)
					fail(String.format("用户[%s]的老密码是[%s], 他希望将密码[%s]变更为[%s], 预期变更失败,但是却成功了", 
							userName, realOldPasswd, inputOldPasswd, newPasswd));
				sleep();
				_trspermission.close();
				_conn.close();
				/* 普通用户登录并获取数据库列表 */
				_conn = new TRSConnection("http://"+SERVERS[0], userName, loginPasswd, new ConnectParams());
				_conn.getDatabases();
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
			}finally {
				_trspermission.close();
				_conn.close();
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(userName);
			}catch(TRSException e) {}
		}
	}
	/**
	 * 强制一个(有API所有权限)普通用户修改其它普通用户的密码, 预期抛出异常
	 */
	@Test
	public void forceCommonUserChangeOthersPassword() {
		String firstUser = "female";
		String firstUserPasswd = "1234qwer";
		String secondUser = "male";
		String secondUserPasswd = "rewq4321";
		try {
			trspermission.createUser(firstUser, firstUserPasswd, "system", "system", "", true);
			trspermission.createUser(secondUser, secondUserPasswd, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(firstUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(firstUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], firstUser, firstUserPasswd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			try {
				_trspermission.changePwd(secondUser, secondUserPasswd, firstUserPasswd);
				fail(String.format("用户[%s]修改用户[%s]的密码, 由[%s]变成[%s], 预期抛出异常, 实际成功", 
						firstUser, secondUser, secondUserPasswd, firstUserPasswd));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), -1, e.getErrorString()));
				assertEquals(e.getErrorCode(), -1);
			}finally {
				_trspermission.close();
				_conn.close();
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(firstUser);
			} catch (TRSException e) {}
			try {
				trspermission.deleteUser(secondUser);
			} catch (TRSException e) {}
		}
	}
	/**
	 * 强制一个(拥有API所有权限)普通用户调用adminChangePwd()
	 */
	@Test
	public void forceCommonUserCallAdminChangePwd() {
		String commonUser = "common";
		String commonPasswd = "1234qwer";
		try {
			trspermission.createUser(commonUser, commonPasswd, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			/* 不仅调用adminChangePwd, 而且还要修改admin用户的密码 */
			try {
				_trspermission.adminChangePwd("admin", "1234qwer");
				fail(String.format("普通用户[%s]调用[%s]方法,修改[%s]的密码, 预期失败, 但是成功了", 
						commonUser, "adminChangePwd()", "admin"));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), -1, e.getErrorString()));
				assertEquals(e.getErrorCode(), -1);
			} finally {
				_trspermission.close();
				_conn.close();
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
		}
	}
	/**
	 * 强制一个普通用户创建用户
	 */
	@Test
	public void forceCommonUserToCreateUser() {
		String commonUser = "common";
		String commonPasswd = "1234qwer";
		try {
			trspermission.createUser(commonUser, commonPasswd, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			try {
				_trspermission.createUser("child", "1234qwer", "system", "system", "", true);
				fail(String.format("用户[%s]视图创建用户[%s], 预期抛出异常, 实际未能抛出", "common", "child"));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), 9920034, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9920034);
			}
			_trspermission.close();
			_conn.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
		}
	}
	/**
	 * 强制一个普通用户删除用户
	 */
	@Test
	public void forceCommonUserToDeleteUser() {
		String commonUser = "common";
		String commonPasswd = "1234qwer";
		try {
			trspermission.createUser(commonUser, commonPasswd, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			try {
				_trspermission.deleteUser(commonUser);
				fail(String.format("用户[%s]视图删除用户[%s], 预期抛出异常, 实际未能抛出", commonUser, commonUser));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), 9917037, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9920034);
			}
			_trspermission.close();
			_conn.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
		}
	}
	/**
	 * 强制一个普通用户修改他人权限
	 */
	@Test
	public void forceCommonUserModifyOthersPermission() {
		String commonUser = "common";
		String commonPasswd = "1234qwer";
		String otherUser = "other";
		String otherPasswd = "1234qwer";
		try {
			trspermission.createUser(commonUser, commonPasswd, "system", "system", "", true);
			trspermission.createUser(otherUser, otherPasswd, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			try {
				_trspermission.modifyUserConsolePermission(commonUser, CONSOLEPermission.all);
				fail(String.format("用户[%s]修改用户[%s]的API权限, 预期抛出异常, 实际成功", commonUser, otherUser));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), 9999999, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9999999);
			}
			_trspermission.close();
			_conn.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
			try {
				trspermission.deleteUser(otherUser);
			}catch(TRSException e) {}
		}
	}
	
	/**
	 * 强制一个普通用户移除模式
	 */
	@Test
	public void forceCommonUserToRemoveSchema() {
		String commonUser = "common";
		String commonPassword = "1234qwer";
		TRSPermissionClient _trspermission = null;
		try {
			trspermission.createUser(commonUser, commonPassword, arrayToString(USED_SCHEMAS), "", "forceCommonUserToRemoveSchema", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPassword, new ConnectParams());
			_trspermission = new TRSPermissionClient(_conn);
			try {
				_trspermission.deleteSchema(USED_SCHEMAS[0]);
				_trspermission.updateUser(commonUser, user);
				fail(String.format("用户[%s]移除了用户[%s]的模式[%s]， 预期抛出异常, 实际成功", commonUser, commonUser, USED_SCHEMAS[0]));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), 9999999, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9999999);
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
		}
	}
	
	/**
	 * 强制一个普通用户修改管理台权限
	 */
	@Test
	public void forceCommonUserToSetConsolePermission() {
		String commonUser = "common";
		String commonPassword = "1234qwer";
		TRSPermissionClient _trspermission = null;
		try {
			trspermission.createUser(commonUser, commonPassword, arrayToString(USED_SCHEMAS), "", "forceCommonUserToSetConsolePermission", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPassword, new ConnectParams());
			try {
				_trspermission = new TRSPermissionClient(_conn);
				_trspermission.modifyUserConsolePermission(commonUser, CONSOLEPermission.all);
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), 9999999, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9999999);
			}
		} catch (TRSException e) {
			e.printStackTrace();
		}
	}
	
	@DataProvider(name = "forceCommonUserToRemoveAddApiPermissionDataProvider")
	public Object[][] forceCommonUserToRemoveAddApiPermissionDataProvider(Method method){
		if(!"forceCommonUserToRemoveAddApiPermission".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 普通用户仅移除权限, 不再添加 */
			new Object[] {1, false},
			/* 普通用户先移除权限, 然后添加 */
			new Object[] {2, true},
			
			/* ↑ 对于上述2种情况没有区别, 都会抛出异常 */
			/* 2021.5.12 未能抛出异常, 提交到禅道 */
		};
	}
	
	/**
	 * 强制一个普通用户增加API权限
	 */
	@Test(dataProvider = "forceCommonUserToRemoveAddApiPermissionDataProvider")
	public void forceCommonUserToRemoveAddApiPermission(int caseId, boolean addAfterRemove) {
		LOGGER.debug("forceCommonUserToAddApiPermission, caseId="+caseId);
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String password = "1234qwer";
		try {
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, system, system, \"\", true)", commonUser, password));
			trspermission.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			TRSUser user = trspermission.getUserInfo(commonUser);
			LOGGER.debug(String.format("[%s].addApiPermission(APIPermission.all)", user.getName()));
			user.addApiPermission(APIPermission.all);
			LOGGER.debug(String.format("trspermission.updateUser(%s, user)", commonUser));
			trspermission.updateUser(commonUser, user);
			sleep();
			LOGGER.debug(String.format("_conn = new TRSConnection(%s, %s, %s, new ConnectParams())", "http://"+SERVERS[0], commonUser, password));
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, password, new ConnectParams());
			LOGGER.debug(String.format("_trspermission = new TRSPermissionClient(%s)", _conn.getURL()));
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			LOGGER.debug(String.format("user = _trspermission.getUserInfo(%s)", commonUser));
			user = _trspermission.getUserInfo(commonUser);
			try {
				LOGGER.debug(String.format("[%s].removeApiPermission(APIPermission.all)", user.getName()));
				user.removeApiPermission(APIPermission.all);
				if(addAfterRemove) {
					LOGGER.debug(String.format("[%s].addApiPermission(APIPermission.createdb)", user.getName()));
					user.addApiPermission(APIPermission.createdb);
				}
				LOGGER.debug(String.format("_trspermission.updateUser(%s, user)", commonUser));
				_trspermission.updateUser(commonUser, user);
				fail(String.format("用户[%s]变更自己的权限, 预期抛出异常, 实际未抛出", commonUser));
			}catch(TRSException e) {
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=%d, e.getErrorString=%s", 
						e.getErrorCode(), 9999999, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9999999);
			}
			_trspermission.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 强制普通用户移除ip黑白名单
	 */
	@Test
	public void forceCommonUserToRemoveIpList() {
		LOGGER.debug("forceCommonUserToRemoveIpList");
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String password = "1234qwer";
		try {
			trspermission.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			user.setIpBlackList(Arrays.asList(getFirstLocalIp()));
			user.setIpWhiteList(Arrays.asList(getFirstLocalIp()));
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, password, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			user = _trspermission.getUserInfo(commonUser);
			user.removeIpBlackList(getFirstLocalIp());
			try {
				_trspermission.updateUser(commonUser, user);
			}catch(TRSException e) {
				assertEquals(e.getErrorCode(), 9999999);
			}
			_trspermission.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "forceCommonUserToAddSchemaDataProvider")
	public Object[][] forceCommonUserToAddSchema(Method method){
		if(!"forceCommonUserToAddSchema".equals(method.getName()))
			return null;
		return new Object[][] {
			new Object[] {1, true, 9999999},
			new Object[] {2, false, 9999999}
		};
	}
	/**
	 * 强迫一个普通用户添加模式列表
	 * 2021.4.1 目前有bug, 普通用户追加模式成功了, 待修复
	 * 2021.5.8 已修复
	 */
	@Test(dataProvider = "forceCommonUserToAddSchemaDataProvider")
	public void forceCommonUserToAddSchema(int caseId, boolean myself, int expected){
		LOGGER.debug(String.format("forceCommonUserToAddSchema, caseId=%d", caseId));
		String self = "self";
		String other = "other";
		String password = "1234qwer";
		TRSPermissionClient _trspermission = null;
		try {
			/* 新建两个普通用户 */
			trspermission.createUser(self, password, "system", "system", "forceCommonUserToAddSchema", true);
			trspermission.createUser(other, password, "system", "system", "forceCommonUserToAddSchema", true);
			sleep();
			/* 给 self 用户添加API所有权限 */
			TRSUser user = trspermission.getUserInfo(self);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(self, user);
			sleep();
			/* self 用户初始化连接 */
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], self, password, new ConnectParams());
			_trspermission = new TRSPermissionClient(_conn);
			/* 决定被修改的用户是自己还是他人 */
			String modifyUserName = myself ? self : other;
			try {
				LOGGER.debug(String.format("user = _trspermission.getUserInfo(%s)", modifyUserName));
				TRSUser _user = _trspermission.getUserInfo(modifyUserName);
				LOGGER.debug(String.format("user.addSchema(%s)", USED_SCHEMAS[0]));
				_user.addSchema(USED_SCHEMAS[0]);
				LOGGER.debug(String.format("_trspermission.updateUser(%s, user)", modifyUserName));
				_trspermission.updateUser(modifyUserName, _user);
				fail(String.format("用户 [%s] 为用户 [%s] 添加模式[%s], 预期失败, 但是成功了", self, modifyUserName, USED_SCHEMAS[0]));
			}catch(TRSException e) {
				LOGGER.debug(String.format("e.getErrorCode=%d, expected=%d, e.getErrorString=%s", 
						e.getErrorCode(), expected, e.getErrorString()));
				assertEquals(e.getErrorCode(), expected);
				return;
			}
		}catch(TRSException e) {
			fail(Other.stackTraceToString(e));
		}finally {
			try {
				trspermission.deleteUser(self);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				trspermission.deleteUser(other);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			if(_trspermission != null)
				_trspermission.close();
		}
	}
	
	@DataProvider(name = "addUserApiPermissionDataProvider")
	public Object[][] addUserApiPermissionDataProvider(Method method){
		if(!"addUserApiPermission".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 所有权限 */
			new Object[] {1, new APIPermission[] {APIPermission.all}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			/* 建表权限 */
			new Object[] {2, new APIPermission[] {APIPermission.createdb}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, false, 500028, false, 403, false, 403},
			/* 删表权限 */
			new Object[] {3, new APIPermission[] {APIPermission.deletedb}, true, Integer.MIN_VALUE,
					false, 403, false, 500028, false, 403, true, Integer.MIN_VALUE},
			/* 入库权限 */
			new Object[] {4, new APIPermission[] {APIPermission.insert}, true, Integer.MIN_VALUE,
					false, 403, true, Integer.MIN_VALUE, false, 403, false, 403},
			/* 检索权限 */
			new Object[] {5, new APIPermission[] {APIPermission.search}, true, Integer.MIN_VALUE,
					false, 403, false, 500028, true, Integer.MIN_VALUE, false, 403},
			/* 无权限 */
			new Object[] {6, new APIPermission[] {APIPermission.none}, true, Integer.MIN_VALUE,
					false, 403, false, 403, false, 403, false, 403},
			/* 组合权限 */
			new Object[] {7, new APIPermission[] {APIPermission.createdb, APIPermission.deletedb, APIPermission.insert, APIPermission.search, APIPermission.all}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			/* 这里边没包含hadoop权限, 放到其它方法中 */
			
			/* 异常情况, all 和 none 一起给, 这俩冲突 */
			/* 2021.5.12 实际没冲突, 提交到禅道 */
			new Object[] {8, new APIPermission[] {APIPermission.all, APIPermission.none}, false, 9999999,
					false, Integer.MIN_VALUE, false, Integer.MIN_VALUE, false, Integer.MIN_VALUE, false, Integer.MIN_VALUE},
			
			/* 其它情况, 如 createdb 和 none 一起给, 会导致用户无任何权限 */
			new Object[] {9, new APIPermission[] {APIPermission.createdb, APIPermission.none}, true, Integer.MIN_VALUE,
					false, 403, false, 403, false, 403, false, 403},
		};
	}
	/**
	 * 用户权限(组合)添加测试
	 * @param caseId
	 * @param apiPermissions 赋予的权限(列表)
	 * @param expectedCreateDatabaseSucceed
	 * @param expectedCreateDatabaseErrorCode
	 * @param expectedLoadRecordsSucceed
	 * @param expectedLoadRecordErrorCode
	 * @param expectedSearchSucceed
	 * @param expectedSearchErrorCode
	 * @param expectedDropSucceed
	 * @param expectedDropErrorCode
	 */
	@Test(dataProvider = "addUserApiPermissionDataProvider")
	public void addUserApiPermission(
			int caseId, 
			APIPermission[] apiPermissions,
			boolean expectedUpdateUserSucceed,
			int expectedUpdatedUserErrorCode,
			boolean expectedCreateDatabaseSucceed,
			int expectedCreateDatabaseErrorCode,
			boolean expectedLoadRecordsSucceed,
			int expectedLoadRecordErrorCode,
			boolean expectedSearchSucceed,
			int expectedSearchErrorCode,
			boolean expectedDropSucceed,
			int expectedDropErrorCode
			) {
		LOGGER.debug("addUserApiPermission, caseId="+caseId);
		String commonUser = "common";
		String commonPasswd = "1234qwer";
		String databaseNameWhichAdminPrepared = String.format("%s.admincreate_%d_%d", USED_SCHEMAS[0], caseId, System.currentTimeMillis());
		String databaseNameWhichCommonPrepared = String.format("%s.commoncreate_%d_%d", USED_SCHEMAS[0], caseId, System.currentTimeMillis());
		try {
			/* 新建用户 */
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, %s, comment, true)", 
					commonUser, commonPasswd, arrayToString(USED_SCHEMAS), USED_SCHEMAS[0]));
			trspermission.createUser(commonUser, commonPasswd, arrayToSet(USED_SCHEMAS), USED_SCHEMAS[0], "", true);
			sleep();
			/* 修改权限 */
			LOGGER.debug(String.format("TRSUser user = trspermission.getUserInfo(%s)", commonUser));
			TRSUser user = trspermission.getUserInfo(commonUser);
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<apiPermissions.length; i++) {
				LOGGER.debug(String.format("user.addApiPermission(%s)", apiPermissions[i].toString()));
				user.addApiPermission(apiPermissions[i]);
				sb.append(apiPermissions[i].toString()).append(";");
			}
			LOGGER.debug(String.format("trspermission.updateUser(%s, user)", commonUser));
			/* all 和 none 一起给的情况, 应当抛出异常*/
			try {
				trspermission.updateUser(commonUser, user);
				if(!expectedUpdateUserSucceed) {
					fail(String.format("给用户[%s]赋予模式[%s]预期抛出异常, 实际没有", commonUser, sb.toString()));
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, expected=%d, errorString=%s", 
						e.getErrorCode(), expectedUpdatedUserErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedUpdatedUserErrorCode);
				return;
			}
			sleep();
			/* 检查普通用户 apiUnlimitAble(), createdbAble(), deletedbAble(), insertAble(), searchAble(), hadoopAble() */
			checkUserPermission(trspermission, commonUser, apiPermissions);
			/* 管理员事先准备好一张表和记录 */
			LOGGER.debug(String.format("create database [%s]", databaseNameWhichAdminPrepared));
			createDemoDatabase(conn, databaseNameWhichAdminPrepared);
			sleep();
			/* 这里先不要装库, 等普通用户的装库权限验证后再入库 */
			/* 新建的普通用户验证权限 */
			LOGGER.debug(String.format("_conn = new TRSConnecton(%s, %s, %s, new ConnectParams())", 
					"http://" + SERVERS[0], commonUser, commonPasswd));
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			try {
				LOGGER.debug(String.format("[%s] creating database [%s]", commonUser, databaseNameWhichCommonPrepared));
				createDemoDatabase(_conn, databaseNameWhichCommonPrepared);
				if(!expectedCreateDatabaseSucceed)
					fail(String.format("用户[%s]创建数据库[%s]预期抛出异常, 但是成功了", commonUser, databaseNameWhichCommonPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedCreateDatabaseErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedCreateDatabaseErrorCode);
			}
			try {
				/* 普通用户的入库权限验证 */
				/* 注意从这里开始, 使用的表都是管理员创建的, 不使用普通用户自己创建的(万一没给建表权限, 建表抛了异常后续全都没法查了) */
				LOGGER.debug(String.format("_conn.loadRecords(%s, %s, 0)", databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs"));
				_conn.loadRecords(databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs", 0);
				if(!expectedLoadRecordsSucceed)
					fail(String.format("用户[%s]尝试向表[%s]装入记录,预期抛出异常,实际成功", 
							commonUser, databaseNameWhichCommonPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedLoadRecordErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedLoadRecordErrorCode);
				/* 入库进入catch块, 说明表里没有记录, 由管理员负责入库 */
				LOGGER.debug(String.format("conn.loadRecords(%s, %s, 0)", databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs"));
				long loadNumber = conn.loadRecords(databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs", 0);
				assertEquals(loadNumber, 5079);
				sleep();
			}
			/* 普通用户检索权限验证 */
			try {
				LOGGER.debug(String.format("_conn.executeSelect(%s, rowid:*, 0, 1, new SearchParams())", databaseNameWhichAdminPrepared));
				_conn.executeSelect(databaseNameWhichAdminPrepared, "rowid:*", 0, 1, new SearchParams());
				if(!expectedSearchSucceed)
					fail(String.format("用户[%s]检索[%s]预期抛出异常, 实际成功", commonUser, databaseNameWhichAdminPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedSearchErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedSearchErrorCode);
			}
			/* 普通用户删表权限验证 */
			try {
				LOGGER.debug(String.format("_conn.deleteDatabase(%s)", databaseNameWhichAdminPrepared));
				_conn.deleteDatabase(databaseNameWhichAdminPrepared);
				if(!expectedDropSucceed)
					fail(String.format("用户[%s]删除表[%s]预期抛出异常, 实际成功", commonUser, databaseNameWhichAdminPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedDropErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedDropErrorCode);
			}
			_conn.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
			try {
				conn.deleteDatabase(databaseNameWhichAdminPrepared);
			}catch(TRSException e) {}
			try {
				conn.deleteDatabase(databaseNameWhichCommonPrepared);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider(name = "removeUserApiPermissionDataProvider")
	public Object[][] removeUserApiPermissionDataProvider(Method method){
		if(!"removeUserApiPermission".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 不剥夺权限:即拥有所有权限 */
			new Object[] {1, 
					new APIPermission[] {APIPermission.none}, 
					true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			/* 保留建表权限, 其它全剥夺 */
			new Object[] {2, 
					new APIPermission[] {
							APIPermission.deletedb, 
							APIPermission.insert, 
							APIPermission.search, 
							APIPermission.hadoop, 
							APIPermission.none}, 
					true, Integer.MIN_VALUE, false, 500028, false, 403, false, 403},
			/* 保留删表权限, 其它全剥夺 */
			new Object[] {3, 
					new APIPermission[] {
							APIPermission.createdb, 
							APIPermission.insert, 
							APIPermission.search, 
							APIPermission.hadoop, 
							APIPermission.none}, 
					false, 403, false, 500028, false, 403, true, Integer.MIN_VALUE},
			/* 保留入库权限, 其它全剥夺 */
			new Object[] {4, 
					new APIPermission[] {
							APIPermission.createdb, 
							APIPermission.deletedb, 
							APIPermission.search, 
							APIPermission.hadoop, 
							APIPermission.none}, 
					false, 403, true, Integer.MIN_VALUE, false, 403, false, 403},
			/* 保留检索权限, 其它全剥夺 */
			new Object[] {5, 
					new APIPermission[] {
							APIPermission.createdb, 
							APIPermission.deletedb, 
							APIPermission.insert, 
							APIPermission.hadoop, 
							APIPermission.none}, 
					false, 403, false, 500028, true, Integer.MIN_VALUE, false, 403},
			/* 剥夺所有权限 */
			new Object[] {6, 
					new APIPermission[] {APIPermission.all}, 
					false, 403, false, 403, false, 403, false, 403},
			/* 剥夺所有权限 */
			new Object[] {7, 
					new APIPermission[] {
							APIPermission.createdb, 
							APIPermission.deletedb, 
							APIPermission.insert, 
							APIPermission.search, 
							APIPermission.all},
					false, 403, false, 403, false, 403, false, 403}
			/* 这里边没包含hadoop权限, 放到其它方法中 */
		};
	}
	
	/**
	 * 用户权限(组合)移除测试<br><br>
	 * 
	 * 思路和增加权限的测试方法完全一致<br>
	 * 例如, 在添加的测试方法addUserApiPermission()中增加建表权限时参数只填写APIPermission.createdb<br>
	 * 则在这里填写要去除的权限为search,deletedb,insert,update,hadoop<br><br>
	 * 
	 * 2021.4.6 因为一致性问题验证不了, 提交到禅道
	 * @param caseId
	 * @param apiPermissionsToBeRemoved 将要被剥夺的权限
	 * @param expectedCreateDatabaseSucceed
	 * @param expectedCreateDatabaseErrorCode
	 * @param expectedLoadRecordsSucceed
	 * @param expectedLoadRecordErrorCode
	 * @param expectedSearchSucceed
	 * @param expectedSearchErrorCode
	 * @param expectedDropSucceed
	 * @param expectedDropErrorCode
	 */
	@Test(dataProvider = "removeUserApiPermissionDataProvider")
	public void removeUserApiPermission(
			int caseId, 
			APIPermission[] apiPermissionsToBeRemoved,
			boolean expectedCreateDatabaseSucceed,
			int expectedCreateDatabaseErrorCode,
			boolean expectedLoadRecordsSucceed,
			int expectedLoadRecordErrorCode,
			boolean expectedSearchSucceed,
			int expectedSearchErrorCode,
			boolean expectedDropSucceed,
			int expectedDropErrorCode){
		LOGGER.debug("removeUserApiPermission, caseId="+caseId);
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String password = "1234qwer";
		String databaseNamePreparedByAdmin = String.format("%s.admincreate_%d_%d", USED_SCHEMAS[0], caseId, System.currentTimeMillis());
		String databaseNamePreparedByCommonUser = String.format("%s.commoncreate_%d_%d", USED_SCHEMAS[0], caseId, System.currentTimeMillis());
		try {
			/* 新建用户 */
			Set<String> initSchemas = arrayToSet(USED_SCHEMAS);
			String comment = String.format("remove user api permission, case=%d", caseId);
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, %s, %s, true)", 
					commonUser, password, initSchemas, USED_SCHEMAS[0], comment, true));
			trspermission.createUser(commonUser, password, initSchemas, USED_SCHEMAS[0], 
					comment, true);
			sleep();
			/* 赋予用户初始权限, 初始一律为所有权限, 添加所有权限是否有效这里不验证, 见增加权限的测试代码 */
			LOGGER.debug(String.format("trspermission.getUserInfo(%s)", commonUser));
			TRSUser user = trspermission.getUserInfo(commonUser);
			LOGGER.debug("user.addApiPermission(APIPermission.all);");
			user.addApiPermission(APIPermission.all);
			LOGGER.debug(String.format("trspermission.updateUser(%s, newUser);", commonUser));
			trspermission.updateUser(commonUser, user);
			sleep();
			/* 管理员准备好表和记录, 验证普通用户的入库权限; 先不要入库, 待验证普通用户的入库权限后再说; */
			LOGGER.debug(String.format("createDemoDatabase(conn, %s)", databaseNamePreparedByAdmin));
			createDemoDatabase(conn, databaseNamePreparedByAdmin);
			sleep();
			/* 再获取普通用户, 这次是去除权限, 去除的权限在 apiPermissionsToBeRemoved 参数中 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			user = trspermission.getUserInfo(commonUser);
			for(APIPermission permissionToBeRemoved : apiPermissionsToBeRemoved) {
				LOGGER.debug(String.format("user.removeApiPermission(%s)", permissionToBeRemoved));
				user.removeApiPermission(permissionToBeRemoved);
			}
			LOGGER.debug("trspermission.updateUser("+commonUser+", user)");
			trspermission.updateUser(commonUser, user);
			sleep(10000L);
			/* apiUnlimitAble(), createdbAble(), deletedbAble(), insertAble(), searchAble(), hadoopAble() 
			 * 这里不做检查, 改为放到增加权限那里检查*/
			/* 普通用户建立连接 */
			String commonHost = "http://" + SERVERS[0];
			LOGGER.debug(String.format("TRSConnection _conn = new TRSConnection(%s, %s, %s, new ConnectParams());", 
					commonHost, commonUser, password));
			TRSConnection _conn = new TRSConnection(commonHost, commonUser, password, new ConnectParams());
			/* 建表权限验证 */
			try {
				LOGGER.debug(String.format("[%s] creating database [%s]", commonUser, databaseNamePreparedByCommonUser));
				createDemoDatabase(_conn, databaseNamePreparedByCommonUser);
				if(!expectedCreateDatabaseSucceed)
					fail(String.format("用户[%s]创建数据库[%s]预期抛出异常, 但是成功了", commonUser, databaseNamePreparedByCommonUser));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedCreateDatabaseErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedCreateDatabaseErrorCode);
			}
			try {
				/* 普通用户的入库权限验证 */
				/* 注意从这里开始, 使用的表都是管理员创建的, 不使用普通用户自己创建的(若剥夺建表权限, 建表抛了异常后续没法检查) */
				LOGGER.debug(String.format("_conn.loadRecords(%s, %s, 0)", databaseNamePreparedByAdmin, "." + "/TRSUser/demo.trs"));
				_conn.loadRecords(databaseNamePreparedByAdmin, "." + "/TRSUser/demo.trs", 0);
				if(!expectedLoadRecordsSucceed)
					fail(String.format("用户[%s]尝试向表[%s]装入记录,预期抛出异常,实际成功", 
							commonUser, databaseNamePreparedByAdmin));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedLoadRecordErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedLoadRecordErrorCode);
				/* 入库进入catch块, 说明表里没有记录, 由管理员负责入库 */
				LOGGER.debug(String.format("conn.loadRecords(%s, %s, 0)", databaseNamePreparedByAdmin, "." + "/TRSUser/demo.trs"));
				long loadNumber = conn.loadRecords(databaseNamePreparedByAdmin, "." + "/TRSUser/demo.trs", 0);
				assertEquals(loadNumber, 5079);
				sleep();
			}
			/* 普通用户检索权限验证 */
			try {
				LOGGER.debug(String.format("_conn.executeSelect(%s, rowid:*, 0, 1, new SearchParams())", databaseNamePreparedByAdmin));
				_conn.executeSelect(databaseNamePreparedByAdmin, "rowid:*", 0, 1, new SearchParams());
				if(!expectedSearchSucceed)
					fail(String.format("用户[%s]检索[%s]预期抛出异常, 实际成功", commonUser, databaseNamePreparedByAdmin));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedSearchErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedSearchErrorCode);
			}
			/* 普通用户删表权限验证 */
			try {
				LOGGER.debug(String.format("_conn.deleteDatabase(%s)", databaseNamePreparedByAdmin));
				_conn.deleteDatabase(databaseNamePreparedByAdmin);
				if(!expectedDropSucceed)
					fail(String.format("用户[%s]删除表[%s]预期抛出异常, 实际成功", commonUser, databaseNamePreparedByAdmin));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedDropErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedDropErrorCode);
			}
			_conn.close();
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				conn.deleteDatabase(databaseNamePreparedByAdmin);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				conn.deleteDatabase(databaseNamePreparedByCommonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 检查 apiUnlimitAble(), createdbAble(), deletedbAble(), insertAble(), searchAble(), hadoopAble()<br><br>
	 * 
	 * 不再为这些方法单独编写测试方法, 而是将它们嵌入addApiPermission() 和 removeApiPermission() 的测试代码中
	 * @param trspermission
	 * @param userName
	 * @param apiPermissions
	 * @throws TRSException
	 */
	private static void checkUserPermission(TRSPermissionClient trspermission, String userName, APIPermission[] apiPermissionsGivenToUser) throws TRSException {
		TRSUser user = trspermission.getUserInfo(userName);
		List<APIPermission> permissionList = Arrays.asList(apiPermissionsGivenToUser);
		/* 检查有all权限, 直接断言每个查看权限的方法返回都是true */
		if(permissionList.contains(APIPermission.all)) {
			LOGGER.debug(String.format("apiPermissionsGivenToUser contains APIPermission.all, user.apiUnlimitAble()=%b, user.createdbAble()=%b, user.deletedbAble()=%b, user.insertAble()=%b, user.searchAble()=%b, user.hadoopAble()=%b", 
					user.apiUnlimitAble(), user.createdbAble(), user.deletedbAble(), user.insertAble(), user.searchAble(), user.hadoopAble()));
			assertTrue(user.apiUnlimitAble() && user.createdbAble() && user.deletedbAble() && 
					user.insertAble() && user.searchAble() && user.hadoopAble());
			return;
		}
		/* 检查有none权限, 直接断言每个查看权限的方法返回都是false */
		if(permissionList.contains(APIPermission.none)) {
			LOGGER.debug(String.format("apiPermissionsGivenToUser contains APIPermission.none, user.apiUnlimitAble()=%b, user.createdbAble()=%b, user.deletedbAble()=%b, user.insertAble()=%b, user.searchAble()=%b, user.hadoopAble()=%b", 
					user.apiUnlimitAble(), user.createdbAble(), user.deletedbAble(), user.insertAble(), user.searchAble(), user.hadoopAble()));
			assertFalse(user.apiUnlimitAble() && user.createdbAble() && user.deletedbAble() && 
					user.insertAble() && user.searchAble() && user.hadoopAble());
			return;
		}
		/* 不可能存在 all 和 none 同时给的情况, 因为这种情况在提交到服务器时就已经抛出异常了 */
		/* 既没给all, 也没给none 时的权限检查 */
		for(APIPermission permission : apiPermissionsGivenToUser) {
			LOGGER.debug(String.format(
				"current permission=%s, user.apiUnlimitAble()=%b, user.createdbAble()=%b, user.deletedbAble()=%b, user.insertAble()=%b, user.searchAble()=%b, user.hadoopAble()=%b", 
				permission.toString(), user.apiUnlimitAble(), user.createdbAble(), user.deletedbAble(), user.insertAble(), user.searchAble(), user.hadoopAble()));
			switch(permission) {
				case createdb : assertTrue(user.createdbAble()); break;
				case deletedb : assertTrue(user.deletedbAble()); break;
				case insert : assertTrue(user.insertAble()); break;
				case search : assertTrue(user.searchAble()); break;
				case hadoop : assertTrue(user.hadoopAble()); break;
				default : fail(String.format("当前权限为[%s], 不属于[createdb|deletedb|insert|search|hadoop|all|none]", permission.toString()));
			}
		}
	}
	
	private static String getFirstLocalIp() {
		List<String> ips = new ArrayList<String>();
		try {
			ips = getLocalIps();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		String regex = "^\\d{1,3}.\\d{1,3}.[5][1|6].\\d+$";
		for(int i=0, size=ips.size(); i<size; i++) {
			if(ips.get(i).matches(regex)) {
				return ips.get(i);
			}
		}
		return "127.0.0.1";
	}
	
    private static List<String> getLocalIps() throws SocketException{
    	List<String> ips = new ArrayList<String>();
    	Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
    	NetworkInterface networkInterface = null;
    	Enumeration<InetAddress> inetAddressEnum = null;
    	InetAddress inetAddress = null;
    	while(enumeration.hasMoreElements()) {
    		networkInterface = enumeration.nextElement();
    		if(networkInterface.isLoopback() || networkInterface.isVirtual())
    			continue;
    		inetAddressEnum = networkInterface.getInetAddresses();
    		while(inetAddressEnum.hasMoreElements()) {
    			inetAddress = inetAddressEnum.nextElement();
    			if(inetAddress.isLoopbackAddress() || !inetAddress.isSiteLocalAddress() || inetAddress.isAnyLocalAddress())
    				continue;
    			ips.add(inetAddress.getHostAddress());
    		}
    	}
    	return ips;
    }
	
	@DataProvider(name = "setIpBlackWhiteListDataProvider")
	public Object[][] setIpBlackWhiteListDataProvider(Method method){
		if(!"setIpBlackWhiteList".equals(method.getName()))
			return null;
		String localHost = getFirstLocalIp();
		return new Object[][] {
			/* 只有白名单, 没有黑名单; 白名单为客户端(执行测试代码的电脑)IP*/
			new Object[] {1, new String[] {}, new String[] {localHost}, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			/* 只有黑名单, 没有白名单, 黑名单为客户端IP, 这样新建用户登录被禁止 */
			new Object[] {2, new String[] {localHost}, new String[] {}, true, Integer.MIN_VALUE, false, 403},
			/* 本机ip同时上黑白名单, 正常访问 */
			new Object[] {3, new String[] {localHost}, new String[] {localHost}, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			/* *.* 设置黑名单 */
			new Object[] {4, new String[] {"*.*"}, new String[] {}, false, 9999999, false, Integer.MIN_VALUE},
			/* *.* 也不能给白名单 */
			new Object[] {5, new String[] {}, new String[] {"*.*"}, false, 9999999, false, Integer.MIN_VALUE},
			/* 范围指定ip白名单 */
			new Object[] {6, new String[] {}, new String[] {"192.168.56.*", "192.168.51.*"}, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			/* 范围指定ip黑名单 */
			new Object[] {7, new String[] {"192.168.56.*", "192.168.51.*"}, new String[] {}, true, Integer.MIN_VALUE, false, 403},
			/* 星号不在最后的位置, 更新用户权限时就抛出异常 */
			new Object[] {8, new String[] {"192.*.56.*"}, new String[] {}, false, 9999999, false, Integer.MIN_VALUE}
		};
	}
	
	/**
	 * 验证设置IP黑白名单
	 * @param caseId
	 * @param blackIps
	 * @param whiteIps
	 * @param expectedUpdateUserSucceed
	 * @param expectedUpdateUserErrorCode
	 * @param expectedLoginSucceed
	 * @param expectedLoginErrorCode
	 */
	@Test(dataProvider = "setIpBlackWhiteListDataProvider")
	public void setIpBlackWhiteList(
			int caseId, String[] blackIps, String[] whiteIps, 
			boolean expectedUpdateUserSucceed, int expectedUpdateUserErrorCode,
			boolean expectedLoginSucceed, int expectedLoginErrorCode) {
		LOGGER.debug("setIpBlackWhiteList, caseId="+caseId);
		String commonUser = "common_"+System.currentTimeMillis();
		String commonPasswd = "1234qwer";
		List<String> ipBlackList = Arrays.asList(blackIps);
		List<String> ipWhiteList = Arrays.asList(whiteIps);
		try {
			trspermission.createUser(commonUser, commonPasswd, "system", "system", "检查ip黑名单", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			/* 设置初始ip黑名单 */
			user.setIpBlackList(ipBlackList);
			/* 设置初始ip白名单 */
			user.setIpWhiteList(ipWhiteList);
			try {
				trspermission.updateUser(commonUser, user);
				if(!expectedUpdateUserSucceed)
					fail(String.format("更新用户[%s]ip黑白名单预期异常，实际成功, ipWhiteList=%s, ipBlackList=%s", 
							commonUser, ipWhiteList, ipBlackList));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorCode=%s", 
						e.getErrorCode(), expectedUpdateUserErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedUpdateUserErrorCode);
				return;
			}
			sleep();
			/* 验证get方法 */
			user = trspermission.getUserInfo(commonUser);
			LOGGER.debug(String.format("user.getIpBlackList=%s", user.getIpBlackList()));
			assertEquals(user.getIpBlackList(), ipBlackList);
			LOGGER.debug(String.format("user.getIpWhiteList=%s", user.getIpWhiteList()));
			assertEquals(user.getIpWhiteList(), ipWhiteList);
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			try {
				_conn.getDatabases();
				if(!expectedLoginSucceed)
				fail(String.format("用户[%s]登录预期抛出异常, 实际成功, ipWhiteList=%s, ipBlackList=%s", 
						commonUser, ipWhiteList, ipBlackList));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorCode=%s", 
						e.getErrorCode(), expectedLoginErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedLoginErrorCode);
			}finally {
				_conn.close();
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider(name = "addIpBlackWhiteListDataProvider")
	public Object[][] addIpBlackWhiteListDataProvider(Method method){
		if(!"addIpBlackWhiteList".equals(method.getName()))
			return null;
		String localHost = getFirstLocalIp();
		String[] empty = new String[0];
		return new Object[][] {
			/* new Object[]{用例编号, 初始黑名单, 初始白名单, 追加黑名单, 追加白名单, 追加是否成功, 追加失败错误号,
			 * 		普通用户登录是否成功, 登录失败错误号, 最终白名单预期, 最终黑名单预期} */
			
			/* 正常情况 */
			
			/* 新建用户, 初始黑白名单为空, 白名单追加本机IP, 登录成功 */
			new Object[] {1, new String[0], new String[0] , new String[0], new String[] {localHost}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, new String[] {localHost}, new String[0]},
			/* 新建用户, 初始黑白名单为空, 黑名单追加本机IP, 登录失败 */
			new Object[] {2, new String[0], new String[0], new String[] {localHost}, new String[0], true, Integer.MIN_VALUE,
					false, 403, new String[0], new String[] {localHost}},
			/* 新建用户, 初始白名单为本机, 初始黑名单为空, 追加本机进黑名单, 登录成功 */
			new Object[] {3, new String[0], new String[] {localHost}, new String[] {localHost}, new String[0], true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, new String[] {localHost}, new String[] {localHost}},
			/* 新建用户, 初始化黑名单为本机, 初始白名单为空, 追加本机进白名单, 登录成功 */
			new Object[] {4, new String[]{localHost}, new String[0], new String[0], new String[]{localHost}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, new String[] {localHost}, new String[] {localHost}},
			/* 新建用户, 初始化黑名单为本机, 初始白名单为空, 使用通配符追加白名单, 登录成功 */
			new Object[] {5, new String[] {localHost}, new String[0], new String[0], new String[] {"192.168.56.*", "192.168.51.*"}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, new String[] {"192.168.56.*", "192.168.51.*"}, new String[] {localHost}},
			/* 新建用户, 黑白名单从来都是空, 登录成功 */
			new Object[] {6, new String[0], new String[0], new String[0], new String[0], true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, new String[0], new String[0]},
			/* 新建用户, 初始化黑白名单均为空, 黑名单使用通配符追加, 登录失败 */
			new Object[] {7, new String[0], new String[0], new String[] {"192.168.56.*","192.168.51.*"}, new String[0], true, Integer.MIN_VALUE,
					false, 403, new String[0], new String[] {"192.168.56.*","192.168.51.*"}},
			
			/*异常情况验证, 对于追加黑白名单抛出异常的情况, 后续无需验证*/
			
			/* *.* 追加仅黑名单 */
			new Object[] {8, new String[0], new String[0], new String[] {"*.*"}, new String[0], false, 9999999,
					false, Integer.MIN_VALUE, empty, empty},
			/* 形如 “192.*.56.*” 追加进白名单, 抛出异常 */
			new Object[] {9, new String[0], new String[0], new String[0], new String[] {"192.*.51.*", "192.*.56.*"}, false, 9999999,
					false, Integer.MIN_VALUE, empty, empty},
			/* 形如 "192.168.*.*" 追加进黑名单, 抛出异常 */
			new Object[] {10, new String[0], new String[0], new String[] {"192.168.*.*"}, new String[0], false, 9999999,
					false, Integer.MIN_VALUE, empty, empty}
			
		};
	}
	/**
	 * IP黑白名单追加验证
	 * @param caseId
	 * @param initBlackIps 初始IP黑名单
	 * @param initWhiteIps 初始IP白名单
	 * @param appendBlackIps 追加IP黑名单
	 * @param appendWhiteIps 追加IP白名单
	 * @param expectedAppendIpSucceed 预期添加IP是否成功
	 * @param expectedAppendIpErrorCode 预期添加IP的错误号
	 * @param expectedLoginSucceed 预期登录是否成功
	 * @param expectedLoginErrorCode 预期登录的错误号
	 * @param expectedAllWhitesIps 预期最后的白名单
	 * @param expectedAllBlackIps 预期最后的黑名单
	 */
	@Test(dataProvider = "addIpBlackWhiteListDataProvider")
	public void addIpBlackWhiteList(int caseId, String[] initBlackIps, String[] initWhiteIps, String[] appendBlackIps, 
			String[] appendWhiteIps, boolean expectedAppendIpSucceed, int expectedAppendIpErrorCode, 
			boolean expectedLoginSucceed, int expectedLoginErrorCode,
			String[] expectedAllWhitesIps, String[] expectedAllBlackIps) {
		LOGGER.debug("addIpBlackWhiteList, caseId="+caseId);
		String userName = "common_"+System.currentTimeMillis();
		String userPasswd = "1234qwer";
		List<String> initIpBlackList = Arrays.asList(initBlackIps);
		List<String> initIpWhiteList = Arrays.asList(initWhiteIps);
		List<String> appendIpBlackList = Arrays.asList(appendBlackIps);
		List<String> appendIpWhiteList = Arrays.asList(appendWhiteIps);
		List<String> expectedAllIpBlackList = Arrays.asList(expectedAllBlackIps);
		List<String> expectedAllIpWhiteList = Arrays.asList(expectedAllWhitesIps);
		try {
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, system, system, comment, true);", 
					userName, userPasswd));
			trspermission.createUser(userName, userPasswd, "system", "system", "", true);
			sleep();
			/* 获取用户, 设置初始ip, 这一步不要有任何异常 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", userName));
			TRSUser user = trspermission.getUserInfo(userName);
			LOGGER.debug(String.format("[%s].setIpBlackList(%s)", user.getName(), initIpBlackList));
			user.setIpBlackList(initIpBlackList);
			LOGGER.debug(String.format("[%s].setIpWhiteList(%s)", user.getName(), initIpWhiteList));
			user.setIpWhiteList(initIpWhiteList);
		    user.addApiPermission(APIPermission.all);
		    LOGGER.debug(String.format("trspermission.updateUser(%s, user)", userName));
		    trspermission.updateUser(userName, user);
		    sleep();
		    /* 获取用户, 追加设置ip, 当设置非法值时, 要检查异常号 */
		    LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", userName));
		    user = trspermission.getUserInfo(userName);
		    LOGGER.debug(String.format("[%s].addIpBlackList(%s)", user.getName(), appendIpBlackList));
		    user.addIpBlackList(appendIpBlackList);
		    LOGGER.debug(String.format("[%s].addIpWhiteList(%s)", user.getName(), appendIpWhiteList));
		    user.addIpWhiteList(appendIpWhiteList);
		    LOGGER.debug(String.format("trspermission.updateUser(%s, user)", userName));
		    try {
		    	 trspermission.updateUser(userName, user);
		    	 if(!expectedAppendIpSucceed)
		    		 fail(String.format("追加了用户[%s]的密码, 预期抛出异常,实际成功, appendIpWhiteList=%s, appendIpBlackList", 
		    				 appendIpWhiteList, appendIpBlackList));
		    }catch(TRSException e) {
		    	LOGGER.debug(String.format("getErrorCode=%s, expected=%d, getErrorString=%s", 
		    			e.getErrorCode(), expectedAppendIpErrorCode, e.getErrorString()));
		    	assertEquals(e.getErrorCode(), expectedAppendIpErrorCode);
		    	return;
		    }
		    sleep();
		    TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], userName, userPasswd, new ConnectParams());
		    TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
		    /* 获取普通用户 */
		    LOGGER.debug("user = trspermission.getUserInfo("+userName+");");
		    user = trspermission.getUserInfo(userName);
		    LOGGER.debug(String.format("assertEquals([%s].getIpBlackList()=%s, expectedAllIpBlackList=%s)", 
		    		user.getName(), user.getIpBlackList(), expectedAllIpBlackList));
		    assertEquals(user.getIpBlackList(), expectedAllIpBlackList);
		    LOGGER.debug(String.format("assertEquals([%s].getIpWhiteList()=%s, expectedAllIpWhiteList()=%s)", 
		    		user.getName(), user.getIpWhiteList(), expectedAllIpWhiteList));
		    assertEquals(user.getIpWhiteList(), expectedAllIpWhiteList);
		    try {
		    	  _conn.getDatabases();
		    	  if(!expectedLoginSucceed)
		    		  fail(String.format("用户[%s]登录系统预期抛出异常, 实际未抛出", userName));
		    }catch(TRSException e) {
		    	LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
		    			e.getErrorCode(), expectedLoginErrorCode, e.getErrorString()));
		    	assertEquals(e.getErrorCode(), expectedLoginErrorCode);
		    	return;
		    }finally {
		    	_trspermission.close();
		    	_conn.close();
		    }
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(userName);
			}catch(TRSException e) {}
		}
	}
	
	@DataProvider (name = "removeIpBlackWhiteListDataProvider")
	public Object[][] removeIpBlackWhiteListDataProvider(Method method){
		if(!"removeIpBlackWhiteList".equals(method.getName()))
			return null;
		String localHost = getFirstLocalIp();
		return new Object[][] {
			/* 1. 初始黑白名单均为本机, 将本机从黑名单移除, 可以登录 */
			new Object[] {1, new String[] {localHost}, new String[] {localHost}, new String[] {localHost}, new String[0], 
					true, Integer.MIN_VALUE, new String[] {localHost}, new String[0]},
			/* 2. 初始黑白名单均为使用通配符表示的ip(包含了本机), 将本机从白名单移除, 禁止登录 */
			new Object[] {2, new String[] {"192.168.51.*", "192.168.56.*"}, new String[] {"192.168.51.*", "192.168.56.*"}, new String[0], new String[] {"192.168.51.*", "192.168.56.*"},
					false, 403, new String[0], new String[] {"192.168.51.*", "192.168.56.*"}},
			
			/* 2012.5.12 研发组回复, 设置/追加不支持正则表达式, 这样一来无所谓移除是否支持了, 就视为不支持, 用例3和4无效 */
			/* 3. 初始黑白名单均为正则表达式代表的ip(包含了本机), 将黑白名单同时移除, 登录成功 */
//			new Object[] {3, new String[] {"192\\.168\\.5[16]\\.\\d{3}"}, new String[] {"192\\.168\\.5[16]\\.\\d{3}"}, new String[] {"192\\.168\\.5[16]\\.\\d{3}"}, new String[] {"192\\.168\\.5[16]\\.\\d{3}"},
//					true, Integer.MIN_VALUE, new String[0], new String[0]},
			
			/* 4. 白名单始终为空, 黑名单为客户端精确ip和ip正则表达式(包含本机)的组合, 只移除了本机ip, 仍然禁止登录*/
//			new Object[] {4, new String[] {localHost, "192\\.168\\.5[16]\\.\\d{3}"}, new String[0], new String[] {localHost}, new String[0],
//					false, 403, new String[0], new String[] {"192\\.168\\.5[16]\\.\\d{3}"}}
		};
	}
	/**
	 * 移除(多个?)IP黑白名单<br>
	 * <div>
	 * 2021.4.1 目前这个测试方法没有写完, 原因是疑似不支持移除多个IP<br>
	 * 2021.5.8 研发组变更了方法名和参数名; 参数为列表或数组, 列表或数组的每个元素表示一条描述ip的规则, 设置/追加/移除均代表规则的设置/追加/移除<br>
	 * </div>
	 * <div>
	 * 一条规则可以是:<br>
	 * <ul>	
	 * 	<li>精确ip</li>
	 * 	<li>带有通配符的ip</li>
	 * 	<li>ip的正则表达式</li>
	 * </ul>
	 * </div>
	 * @param caseId
	 * @param initBlackIps 初始IP黑名单
	 * @param initWhiteIps 初始IP白名单
	 * @param blackIpsToBeRemoved 待移除的IP黑名单
	 * @param whiteIpsToBeRemoved 待移除的IP白名单
	 * @param expectedLoginSucceed 预期是否登录成功
	 * @param errorCode 预期错误号
	 * @param expectedAllWhiteIps 预期最后的白名单
	 * @param expectedAllBlackIps 预期最后的黑名单
	 */
	@Test(dataProvider = "removeIpBlackWhiteListDataProvider")
	public void removeIpBlackWhiteList(int caseId, String[] initBlackIps, String[] initWhiteIps, 
			String[] blackIpsToBeRemoved, String[] whiteIpsToBeRemoved, boolean expectedLoginSucceed, int errorCode,
			String[] expectedAllWhiteIps, String[] expectedAllBlackIps) {
		String caseInfo = "removeIpBlackWhiteList, caseId="+caseId;
		LOGGER.debug(caseInfo);
		/* 用户名要求大小写混用, 任何时候发现是纯小写, 都是因为曾经因为debug忘了调整回来了, 改回来 */
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String password = "1234qwer";
		List<String> initIpBlackList = Arrays.asList(initBlackIps);
		List<String> initIpWhiteList = Arrays.asList(initWhiteIps);
		/*
		 * 2021.4.6 我看了api的源码, 目的不清晰
		 * 
		 * public void removeIpBlackList(String ipBlackList) {
		 * 		if (this.ipBlackList == null) {
      	 *			this.ipBlackList = new ArrayList();
    	 *		}
    	 *		this.ipBlackList.remove(ipBlackList);
  		 * }
  		 * 
  		 * 看参数名, 似乎是希望我们传递一个 String 类型的 ip 列表, 列表, 嗯, 再重复一次, 列表
  		 * 但是实现的时候并没有解析这个串, 假如有两个元素(或者只有一个元素,但是在拼装时带上了分号或逗号), 就没有效果了
  		 * 换言之, 仅支持1个ip的移除。看看开发组要不要改了
  		 * 
  		 * 2021.5.8 研发组更换了方法名和参数名, 所有黑白名单的方法，其参数是列表或数组,其每个元素为一条描述ip的规则, 每条规则可以是:
  		 * 
  		 * 1.精确ip, 如 192.168.0.102
  		 * 2.带有通配符的ip, 如 192.168.0.*
  		 * 3.使用正则表达式表示的ip, 如 192\.168\.5[16]\.\d{3}
  		 * 
  		 * 自然, 移除的也是规则, 而不是特指某个ip
  		 * 
  		 * 假如1,2,3同时组合设置黑名单(白名单为空), 这三条规则都覆盖到了192.168.0.102, 假如只移除了第一条规则,
  		 * 则你使用192.168.0.102登录仍然会被禁止
  		 * 
  		 * 2021.5.12 研发组回复: 设置/追加不支持正则表达式, 所以移除支持不支持也无所谓了, 就视为不支持
		 */
		List<String> expectedAllIpBlackList = Arrays.asList(expectedAllBlackIps);
		List<String> expectedAllIpWhiteList = Arrays.asList(expectedAllWhiteIps);
		try {
			/* 新建普通用户 */
			String userSchemasStr = arrayToString(USED_SCHEMAS);
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, \"\", %s, true)", 
					commonUser, password, userSchemasStr, caseInfo));
			trspermission.createUser(commonUser, password, userSchemasStr, "", caseInfo, true);
			sleep();
			/* 赋予初始ip列表 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			TRSUser user = trspermission.getUserInfo(commonUser);
			LOGGER.debug(String.format("user.setIpBlackList(%s)", initIpBlackList));
			user.setIpBlackList(initIpBlackList);
			LOGGER.debug(String.format("user.setIpWhiteList(%s)", initIpWhiteList));
			user.setIpWhiteList(initIpWhiteList);
			user.addApiPermission(APIPermission.all);
			LOGGER.debug(String.format("trspermission.updateUser(%s, user)", commonUser));
			trspermission.updateUser(commonUser, user);
			sleep(10000L);
			/* 再次从服务器端获取ip列表, 然后移除指定ip */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			user = trspermission.getUserInfo(commonUser);
			/* 移除ip黑白名单规则, 需要循环移除, 每次一条 */
			for(String blackIpToBeRemoved : blackIpsToBeRemoved) {
				LOGGER.debug(String.format("[%s].removeIpBlackRule(%s)", user.getName(), blackIpToBeRemoved));
				user.removeIpBlackList(blackIpToBeRemoved);
			}
			for(String whiteIpToBeRemoved : whiteIpsToBeRemoved) {
				LOGGER.debug(String.format("[%s].removeIpWhiteRule(%s)", user.getName(), whiteIpToBeRemoved));
				user.removeIpWhiteList(whiteIpToBeRemoved);
			}
			LOGGER.debug(String.format("trspermission.updateUser(%s, user)", commonUser));
			trspermission.updateUser(commonUser, user);
			sleep(10000L);
			/* 第三次从服务器端获取ip列表, 和预期比较 */
			user = trspermission.getUserInfo(commonUser);
			List<String> actualBlackList = user.getIpBlackList();
			List<String> actualWhiteList = user.getIpWhiteList();
			LOGGER.debug(String.format("actualBlackList=%s, expected=%s", actualBlackList, expectedAllIpBlackList));
			assertEquals(actualBlackList, expectedAllIpBlackList);
			LOGGER.debug(String.format("actualWhiteList=%s, expected=%s", actualWhiteList, expectedAllIpWhiteList));
			assertEquals(actualWhiteList, expectedAllIpWhiteList);
			/* 普通用户登录 */
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, password, new ConnectParams());
			try {
				_conn.getDatabases();
				if(!expectedLoginSucceed) {
					LOGGER.error(String.format("用户[%s]登录预期抛出异常,但是成功了, ip黑名单是%s, 白名单是%s", 
							commonUser, actualBlackList, actualWhiteList));
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
		    			e.getErrorCode(), errorCode, e.getErrorString()));
		    	assertEquals(e.getErrorCode(), errorCode);
			}
			_conn.close();
		} catch (TRSException e) {
			LOGGER.error(String.format("caseId=%d, failure. caused by: e.getErrorCode=%d, e.getErrorString=%s", 
					caseId, e.getErrorCode(), e.getErrorString()));
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "addSchemasDataProvider")
	public Object[][] addSchemasDataProvider(Method method){
		if(!"addSchemas".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 一般情况, 添加一个已存在的模式 */
			new Object[] {1, new String[] {USED_SCHEMAS[0]}, true, Integer.MIN_VALUE, 
					new String[] {"system", USED_SCHEMAS[0]}, USED_SCHEMAS[0]+".demo"},
			/* 添加多个已存在的模式 */
			new Object[] {2, new String[] {USED_SCHEMAS[0], USED_SCHEMAS[1]}, true, Integer.MIN_VALUE,
					new String[] {"system", USED_SCHEMAS[0], USED_SCHEMAS[1]}, USED_SCHEMAS[1]+".demo"},
			/* 重复模式自动过滤 */
			new Object[] {3, new String[] {USED_SCHEMAS[0], USED_SCHEMAS[0]}, true, Integer.MIN_VALUE,
					new String[] {"system", USED_SCHEMAS[0]}, USED_SCHEMAS[0]+".demo"},
			
			/* 错误情况, 添加一个不存在的模式(本例用的是时间戳当待添加的模式名, 可以保证任何环境都没有), 更新用户时抛出异常 */
			/* 2021.04.01: 它没抛异常……待修复 */
			/* 2021.5.12: 抛异常了 */
			new Object[] {4, new String[] {String.valueOf(System.currentTimeMillis())}, false, 9999999,
					null, null}
		};
	}
	
	/**
	 * 添加模式验证<br>
	 * 通过建表检查添加模式成功
	 * @param caseId
	 * @param schemas 追加模式列表
	 * @param expectedAddSucceed 预期追加是否成功
	 * @param expectedErrorCode 预期追加模式的错误号
	 * @param expectedSchemas 最后对拥有的模式的预期
	 * @param dbName 追加模式验证, 最后一步要建表, 这里提供表名
	 */
	@Test(dataProvider="addSchemasDataProvider")
	public void addSchemas(int caseId, String[] schemas, boolean expectedAddSucceed, int expectedErrorCode,
			String[] expectedSchemas, String dbName) {
		LOGGER.debug("addSchemas, caseId="+caseId);
		/* 2021.04.01: 用户名中出现大写字母, 添加模式更新用户后再获取, 返回的模式不符合预期, 暂时先改为小写,
		 * 待开发组修复, 修复后用户名干脆锁定为大小写混合 */
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String commonPasswd = "1234qwer";
		try {
			/* 新建用户 */
			String comment = "addSchemas, caseId="+caseId;
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, \"system\", \"system\", \"%s\", true)", commonUser, commonPasswd, comment));
			trspermission.createUser(commonUser, commonPasswd, "system", "system", comment, true);
			sleep();
			/* 拿到用户, 添加模式 */
			LOGGER.debug(String.format("TRSUser user = trspermission.getUserInfo(%s)", commonUser));
			TRSUser user = trspermission.getUserInfo(commonUser);
			/* 普通用户添加所有权限 */
			LOGGER.debug(String.format("[%s].addApiPeremission(APIPermission.all)", user.getName()));
			user.addApiPermission(APIPermission.all);
			LOGGER.debug(String.format("[%s].addSchemas(%s)", user.getName(), arrayToString(schemas)));
			user.addSchemas(schemas);
			try {
				/* 添加模式有可能抛出异常, 加try-catch捕捉, 并断言错误号 */
				LOGGER.debug(String.format("trspermission.updateUser(%s, user)", commonUser));
				trspermission.updateUser(commonUser, user);
				if(!expectedAddSucceed) {
					fail(String.format("预期为用户[%s]添加模式(%s)失败, 但是成功了", commonUser, arrayToString(schemas)));
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, expectedErrorCode=%d, errorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			sleep();
			/* 检查普通用户的所有模式 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			user = trspermission.getUserInfo(commonUser);
			Set<String> userSchemas = user.getSchemas();
			LOGGER.debug(String.format("[%s].getSchemas()=%s, expected=%s", user.getName(), userSchemas, arrayToString(expectedSchemas)));
			assertEquals(userSchemas.size(), expectedSchemas.length);
			for(String schema : expectedSchemas)
				userSchemas.remove(schema);
			assertTrue(userSchemas.isEmpty());
			/* 添加模式成功后, 普通用户新建数据库验证添加模式成功 */
			/* 不考虑建表失败的情况 */
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, commonPasswd, new ConnectParams());
			assertTrue(createDemoDatabase(_conn, dbName));
			sleep();
			LOGGER.debug(String.format("[%s].getDatabase(%s)", conn.getURL(), dbName));
			TRSDatabase[] dbs = conn.getDatabases(dbName);
			if(dbs == null)
				dbs = new TRSDatabase[0];
			LOGGER.debug(String.format("dbs.length=%d, expected=1", dbs.length));
			assertEquals(dbs.length, 1);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				conn.deleteDatabase(dbName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			sleep();
			try {
				trspermission.deleteUser(commonUser);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 验证创建和变更时间<br>
	 * 服务器端有延迟, 粗查即可, 然后 update 比 create 时间晚即可<br>
	 * 2021.04.01 用户名出现大写字母就会出现 <b>创建时间</b> 和 <b>更新时间</b> 相等的现象, 待修复<br><br>
	 * 
	 * 注：TRSUser.get*Date() 返回值 <b>不是</b> 时间戳, 它可以描述为<br>
	 * <div>
	 * 	<code>LocalDateTime now = LocalDateTime.now();</code><br>
	 * 	<code>String dateTimeStr = now.parse(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));</code><br>
	 * 	<code>long createDate = Long.valueOf(dateTimeStr);</code><br><br>
	 * 	createDate 就是 TRSUser.getCreateDate() 的返回值
	 * </div>
	 */
	@Test
	public void createDateAndUpdateDate() {
		LOGGER.debug(String.format("createDateAndUpdateDate"));
		String commonUser = "commonUser_"+System.currentTimeMillis();
		String password = "1234qwer";
		String formatter = "yyyyMMddHHmmss";
		try {
			LocalDateTime dateTimeBeforeCreateUser = LocalDateTime.now();
			long beforeDate = Long.valueOf(dateTimeBeforeCreateUser.format(DateTimeFormatter.ofPattern(formatter)));
			/* 创建用户 */
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, system, system, \"createDateAndUpdateDate\", true)", 
					commonUser, password));
			trspermission.createUser(commonUser, password, "system", "system", "createDateAndUpdateDate", true);
			sleep();
			/* 获得用户 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			TRSUser user = trspermission.getUserInfo(commonUser);
			long createDate = user.getCreateDate();
			LOGGER.debug(String.format("beforeDate=%d, createDate=%d", beforeDate, createDate));
			/* 创建时间检查
			 * P.S. 如果服务器时间和客户端时间不一致, 有可能不满足下面的条件, 就暂时去掉 */
//			assertTrue(createDate > beforeDate);
			/* 修改备注并提交 */
			user.setComment("add comment");
			LOGGER.debug(String.format("trspermission.updateUser(%s, user)", commonUser));
			trspermission.updateUser(commonUser, user);
			sleep();
			/* 重新获取 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", commonUser));
			user = trspermission.getUserInfo(commonUser);
			long updateDate = (long)(user.getUpdateDate());
			LOGGER.debug(String.format("createDate=%d, updateDate=%d", 
					createDate, updateDate));
			/* 更新时间晚于创建时间 */
			assertTrue(updateDate > createDate);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "removeSchemaDataProvider")
	public Object[][] removeSchemaDataProvider(Method method){
		if(!"removeSchema".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 一般情况: 新建first.demo后, 再移除普通用户的first模式, 获取表时抛出异常 */
			new Object[] {1, USED_SCHEMAS, "first.demo", "first", true, true, Integer.MIN_VALUE, new String[] {USED_SCHEMAS[1], USED_SCHEMAS[2]}, false},
			/* 接口没提供同时移除多个模式的方法, 略 */
			
			/* 移除了不存在的模式, 没有效果即可 */
			/* 2021.4.14 最终决定提交到禅道上 */
			/* 2021.5.8 研发组表示抛出异常不合理, 调整方法返回值, 移除成功返回true, 否则返回false
			 * 在返回false时相当于执行不生效, 和移除前一致 */
			new Object[] {2, USED_SCHEMAS, "first.demo", String.valueOf(System.currentTimeMillis()), false, true, Integer.MIN_VALUE, USED_SCHEMAS, true},
		};
	}
	
	/**
	 * 移除模式验证
	 * @param caseId
	 * @param initSchemas 初始模式集
	 * @param dbName 建表, 模式名要求在初始模式集中
	 * @param removingSchema 被移除的模式
	 * @param expectedRemoveSuccess 预期是否成功移除(在提交更新前)
	 * @param expectedUpdateUserSucceed 预期是否成功提交用户
	 * @param expectedErrorCode 预期的错误号
	 * @param expectedSchemas 预期移除模式后,普通用户的模式列表
	 * @param expectedGetDatabase 预期能否获得 dbName 表, 如果dbName表的模式从普通用户的模式列表中移除, 应该填写false;反之则为true
	 */
	@Test(dataProvider = "removeSchemaDataProvider")
	public void removeSchema(int caseId, String[] initSchemas, String dbName, String removingSchema, 
			boolean expectedRemoveSuccess, boolean expectedUpdateUserSucceed, 
			int expectedErrorCode, String[] expectedSchemas, boolean expectedGetDatabase) {
		LOGGER.debug(String.format("removeSchema, caseId="+caseId));
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String password = "1234qwer";
		try {
			/* 新建普通用户 */
			String initSchemaStr = arrayToString(initSchemas);
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, \"\", comment, true)", 
					commonUser, password, initSchemaStr));
			trspermission.createUser(commonUser, password, initSchemaStr, "", "removeSchema,caseId="+caseId, true);
			sleep();
			/* 获取普通用户, 给所有API权限 */
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			/* 普通用户新建一张表 */
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, password, new ConnectParams());
			LOGGER.debug(String.format("createDemoDatabase(%s, %s)", _conn.getURL(), dbName));
			createDemoDatabase(_conn, dbName);
			sleep();
			/* 移除普通用户的模式, 模式名和上一步新建表的模式名一致 */
			user = trspermission.getUserInfo(commonUser);
			LOGGER.debug(String.format("user.removeSchema(%s)", removingSchema));
			user.removeSchema(removingSchema);
			try {
				trspermission.updateUser(commonUser, user);
				if(!expectedUpdateUserSucceed) {
					String failureMessage = String.format("更新用户[%s]预期抛出异常, 实际未抛出", commonUser);
					LOGGER.error(failureMessage);
					fail(failureMessage);
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, expectedCode=%d, errorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			sleep();
			/* 检查普通用户拥有的模式 */
			user = trspermission.getUserInfo(commonUser);
			Set<String> userSchemas = user.getSchemas();
			LOGGER.debug(String.format("userSchemas=%s, expected=%s", userSchemas, Arrays.asList(expectedSchemas)));
			for(String expectedSchema : expectedSchemas)
				userSchemas.remove(expectedSchema);
			assertTrue(userSchemas.isEmpty());
			try {
				/* 普通用户获取表, 这张表所属的模式已经不属于此用户 */
				LOGGER.debug(String.format("_conn.getDatabase(%s)", dbName));
				_conn.getDatabases(dbName);
				if(!expectedGetDatabase) {
					fail(String.format("用户[%s]获取表[%s]预期失败, 但是成功了; 这个用户拥有的模式有%s", 
							commonUser, dbName, user.getSchemas()));
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, expectedCode=%d, errorString=%s", 
						e.getErrorCode(), 9920500, e.getErrorString()));
				assertEquals(e.getErrorCode(), 9920500);
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				conn.deleteDatabase(dbName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "modifyUserApiPermissionDataProvider")
	public Object[][] modifyUserApiPermission(Method method){
		if(!"modifyUserApiPermission".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 建表权限 */
			new Object[] {1, new APIPermission[] {APIPermission.createdb}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, false, 500028, false, 403, false, 403},
			/* 删表权限 */
			new Object[] {2, new APIPermission[] {APIPermission.deletedb}, true, Integer.MIN_VALUE,
					false, 403, false, 500028, false, 403, true, Integer.MIN_VALUE},
			/* 入库权限 */
			new Object[] {3, new APIPermission[] {APIPermission.insert}, true, Integer.MIN_VALUE,
					false, 403, true, Integer.MIN_VALUE, false, 403, false, 403},
			/* 检索权限 */
			new Object[] {4, new APIPermission[] {APIPermission.search}, true, Integer.MIN_VALUE,
					false, 403, false, 500028, true, Integer.MIN_VALUE, false, 403},
			/* 组合权限 */
			new Object[] {5, new APIPermission[] {APIPermission.createdb, APIPermission.deletedb, APIPermission.insert, APIPermission.search}, true, Integer.MIN_VALUE,
					true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE, true, Integer.MIN_VALUE},
			
			/* 异常情况 */
			/* all 和 none 的组合, 预期抛出异常, 这俩本身冲突, 而且约定以此法修改权限不能给这俩值 */
			new Object[] {6, new APIPermission[] {APIPermission.all, APIPermission.none}, false, -1,
					false, Integer.MIN_VALUE, false, Integer.MIN_VALUE, false, Integer.MIN_VALUE, false, Integer.MIN_VALUE},
		};
	}
	
	/**
	 * 修改用户的部分权限<br><br>
	 * 
	 * 注意这个方法是调用 <code>TRSPermissionClient.modifyUserApiPermission()</code> , 不是先获得TRSUser对象再去修改的。
	 * 另外在权限设置上也有限制, 只能是search, insert, createdb, deletedb, hadoop的任意组合, 没有all和none
	 * @param caseId
	 * @param permissions 设置的权限列表
	 * @param expectedModifySucceed 预期修改权限是否成功
	 * @param expectedModifyErrorCode 预期修改权限的错误号
	 * @param expectedCreateDatabaseSucceed 预期建表是否成功
	 * @param expectedCreateDatabaseErrorCode 预期建表错误号
	 * @param expectedLoadRecordsSucceed 预期入库是否成功
	 * @param expectedLoadRecordsErrorCode 预期入库错误号
	 * @param expectedSearchSucceed 预期检索是否成功
	 * @param expectedSearchErrorCode 预期检索错误号
	 * @param expectedDropSucceed 预期删表是否成功
	 * @param expectedDropErrorCode 预期删表错误号
	 */
	@Test(dataProvider = "modifyUserApiPermissionDataProvider")
	public void modifyUserApiPermission(int caseId, APIPermission[] permissions, 
			boolean expectedModifySucceed, int expectedModifyErrorCode, 
			boolean expectedCreateDatabaseSucceed, int expectedCreateDatabaseErrorCode,
			boolean expectedLoadRecordsSucceed, int expectedLoadRecordsErrorCode,
			boolean expectedSearchSucceed, int expectedSearchErrorCode,
			boolean expectedDropSucceed, int expectedDropErrorCode) {
		String caseInfo = "modifyUserApiPermission, caseId="+caseId;
		LOGGER.debug(caseInfo);
		/* 又是大小写 */
		String commonUser = "commonuser_"+System.currentTimeMillis();
		String password = "1234qwer";
		String schemaName = "system";
		String databaseNameWhichAdminPrepared = "system.admincreate_"+caseId+"_"+System.currentTimeMillis();
		String databaseNameWhichCommonPrepared = "system.commoncreate_"+caseId+"_"+System.currentTimeMillis();
		try {
			/* 新建用户 */
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, %s, \"%s\", true)", 
					commonUser, password, schemaName, schemaName, caseInfo));
			trspermission.createUser(commonUser, password, schemaName, schemaName, caseInfo, true);
			sleep();
			/* 调用 TRSPermissionClient.modifyUserApiPermission 修改权限, 本质上相当于set */
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<permissions.length; i++)
				sb.append(permissions[i].toString());
			LOGGER.debug(String.format("trspermission.modifyUserApiPermission(%s, %s)",commonUser, sb.toString()));
			try {
				trspermission.modifyUserApiPermission(commonUser, permissions);
				if(!expectedModifySucceed) {
					fail(String.format("修改用户[%s]的权限为[%s], 预期异常, 结果通过了", commonUser, sb.toString()));
				}
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, expected=%d, errorString=%s", e.getErrorCode(), expectedModifyErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedModifyErrorCode);
				return;
			}
			sleep();
			/* 检查普通用户 apiUnlimitAble(), createdbAble(), deletedbAble(), insertAble(), searchAble(), hadoopAble() */
			checkUserPermission(trspermission, commonUser, permissions);
			/* 管理员事先准备好一张表和记录 */
			LOGGER.debug(String.format("create database [%s]", databaseNameWhichAdminPrepared));
			createDemoDatabase(conn, databaseNameWhichAdminPrepared);
			sleep();
			/* 这里先不要装库, 等普通用户的装库权限验证后再入库 */
			/* 新建的普通用户验证权限 */
			LOGGER.debug(String.format("_conn = new TRSConnecton(%s, %s, %s, new ConnectParams())", 
					"http://" + SERVERS[0], commonUser, password));
			TRSConnection _conn = new TRSConnection("http://"+SERVERS[0], commonUser, password, new ConnectParams());
			try {
				LOGGER.debug(String.format("[%s] creating database [%s]", commonUser, databaseNameWhichCommonPrepared));
				createDemoDatabase(_conn, databaseNameWhichCommonPrepared);
				if(!expectedCreateDatabaseSucceed)
					fail(String.format("用户[%s]创建数据库[%s]预期抛出异常, 但是成功了", commonUser, databaseNameWhichCommonPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedCreateDatabaseErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedCreateDatabaseErrorCode);
			}
			try {
				/* 普通用户的入库权限验证 */
				/* 注意从这里开始, 使用的表都是管理员创建的, 不使用普通用户自己创建的(万一没给建表权限, 建表抛了异常后续全都没法查了) */
				LOGGER.debug(String.format("_conn.loadRecords(%s, %s, 0)", databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs"));
				_conn.loadRecords(databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs", 0);
				if(!expectedLoadRecordsSucceed)
					fail(String.format("用户[%s]尝试向表[%s]装入记录,预期抛出异常,实际成功", 
							commonUser, databaseNameWhichCommonPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedLoadRecordsErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedLoadRecordsErrorCode);
				/* 入库进入catch块, 说明表里没有记录, 由管理员负责入库 */
				LOGGER.debug(String.format("conn.loadRecords(%s, %s, 0)", databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs"));
				long loadNumber = conn.loadRecords(databaseNameWhichAdminPrepared, "." + "/TRSUser/demo.trs", 0);
				assertEquals(loadNumber, 5079);
				sleep();
			}
			/* 普通用户检索权限验证 */
			try {
				LOGGER.debug(String.format("_conn.executeSelect(%s, rowid:*, 0, 1, new SearchParams())", databaseNameWhichAdminPrepared));
				_conn.executeSelect(databaseNameWhichAdminPrepared, "rowid:*", 0, 1, new SearchParams());
				if(!expectedSearchSucceed)
					fail(String.format("用户[%s]检索[%s]预期抛出异常, 实际成功", commonUser, databaseNameWhichAdminPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedSearchErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedSearchErrorCode);
			}
			/* 普通用户删表权限验证 */
			try {
				LOGGER.debug(String.format("_conn.deleteDatabase(%s)", databaseNameWhichAdminPrepared));
				_conn.deleteDatabase(databaseNameWhichAdminPrepared);
				if(!expectedDropSucceed)
					fail(String.format("用户[%s]删除表[%s]预期抛出异常, 实际成功", commonUser, databaseNameWhichAdminPrepared));
			}catch(TRSException e) {
				LOGGER.debug(String.format("getErrorCode=%d, expected=%d, getErrorString=%s", 
						e.getErrorCode(), expectedDropErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedDropErrorCode);
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				conn.deleteDatabase(databaseNameWhichCommonPrepared);
			}catch(TRSException e) {
				if(!e.getErrorString().contains("not exist!"))
					e.printStackTrace();
			}
			try {
				conn.deleteDatabase(databaseNameWhichAdminPrepared);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 创建一个demo数据库
	 * @param dbName
	 * @return
	 * @throws TRSException
	 */
	private boolean createDemoDatabase(TRSConnection conn, String dbName) throws TRSException {
		TRSDatabase db = new TRSDatabase(dbName, TRSDatabase.TYPE_VIEW, TRSDatabase.DBPOLICY.FASTEST);
		db.addColumn(new TRSDatabaseColumn("rowid", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("作者", TRSDatabaseColumn.TYPE_CHAR));
		db.addColumn(new TRSDatabaseColumn("版次", TRSDatabaseColumn.TYPE_NUMBER));
		db.addColumn(new TRSDatabaseColumn("日期", TRSDatabaseColumn.TYPE_DATE));
		db.addColumn(new TRSDatabaseColumn("标题", TRSDatabaseColumn.TYPE_PHRASE));
		db.addColumn(new TRSDatabaseColumn("正文", TRSDatabaseColumn.TYPE_DOCUMENT));
		db.setParter("rowid");
		LOGGER.debug(String.format("[%s].createDatabase(%s)", conn.getURL(), dbName));
		return conn.createDatabase(db);
	}
	
	@DataProvider(name = "setConsolePermissionDataProvider")
	public Object[][] setConsolePermissionDataProvider(Method method){
		if(!"setConsolePermission".equals(method.getName()))
			return null;
		return new Object[][] {
			new Object[] {1, CONSOLEPermission.none, CONSOLEPermission.none},
			new Object[] {2, CONSOLEPermission.readonly, CONSOLEPermission.readonly},
			new Object[] {3, CONSOLEPermission.all, CONSOLEPermission.all},
		};
	}
	/**
	 * 管理台权限设置<br>
	 * 这里只验证服务器端获取的权限和设置的权限一致
	 */
	@Test(dataProvider = "setConsolePermissionDataProvider")
	public void setConsolePermission(int caseId, CONSOLEPermission consolePermission, CONSOLEPermission expected) {
		LOGGER.debug("setConsolePermission, caseId="+caseId);
		String commonUser = "commonUser_"+System.currentTimeMillis();
		String password = "1234qwer";
		try {
			trspermission.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.setConsolePermission(consolePermission);
			trspermission.updateUser(commonUser, user);
			sleep();
			user = trspermission.getUserInfo(commonUser);
			assertEquals(user.getConsolePermission(), expected);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@DataProvider(name = "setFunctionsDataProvider")
	public Object[][] setFunctionsDataProvider(Method method){
		if(!"setFunctions".equals(method.getName()))
			return null;
		return new Object[][] {
			/* 正常情况, 名称, 模式, 默认模式改成合法值 */
			/* 2021.5.12 用户名不允许修改 */
			new Object[] {1, "zhaoyang", USED_SCHEMAS, USED_SCHEMAS[0], 
					"zhaoyang", new String[] {USED_SCHEMAS[0], USED_SCHEMAS[1]}, USED_SCHEMAS[1], 
					true, Integer.MIN_VALUE,
					"zhaoyang", new String[] {USED_SCHEMAS[0], USED_SCHEMAS[1]}, USED_SCHEMAS[1]},
			/* 默认模式追加一个系统中存在, 但是没有出现在模式列表的, 被系统强塞进去 */
			new Object[] {2, "zhaoyang", USED_SCHEMAS, USED_SCHEMAS[0],
					"zhaoyang", USED_SCHEMAS, "system",
					true, Integer.MIN_VALUE,
					"zhaoyang", new String[] {USED_SCHEMAS[0],USED_SCHEMAS[1],USED_SCHEMAS[2],"system"}, "system"},
			/* 一个非法姓名 */
			new Object[] {3, "zhaoyang", USED_SCHEMAS, USED_SCHEMAS[0],
					"一个普通用户", USED_SCHEMAS, "system",
					false, 9999999,
					null, null, null},
			/* 不存在的模式 */
			new Object[] {4, "zhaoyang", USED_SCHEMAS, USED_SCHEMAS[0],
					"一个普通用户", new String[] {String.valueOf(System.currentTimeMillis())}, "system",
					false, 9999999,
					null, null, null},
		};
	}
	
	/**
	 * 重新设置用户的基本信息
	 * @param caseId
	 * @param initName 初始名称
	 * @param initSchemas 初始模式
	 * @param initDefSchema 初始默认模式
	 * @param newName 新名字
	 * @param newSchemas 新模式列表
	 * @param newDefSchema 新的默认模式
	 * @param expectedUpdateSucceed 预期提交用户是否成功
	 * @param expectedErrorCode 预期错误号
	 * @param expectedName 最后断言时的名称
	 * @param expectedSchemas 最后断言时的模式列表
	 * @param expectedDefSchema 最后断言时的默认模式
	 */
	@Test(dataProvider = "setFunctionsDataProvider")
	public void setFunctions(
			int caseId, String initName, String[] initSchemas, String initDefSchema,
			String newName, String[] newSchemas, String newDefSchema, 
			boolean expectedUpdateSucceed, int expectedErrorCode,
			String expectedName, String[] expectedSchemas, String expectedDefSchema) {
		String caseInfo = String.format("setFunction, caseId=%d", caseId);
		LOGGER.debug(caseInfo);
		String password = "1234qwer";
		try {
			/* 新建用户 */
			String initSchemasStr = arrayToString(initSchemas);
			LOGGER.debug(String.format("trspermission.createUser(%s, %s, %s, %s, \"%s\", true)", 
					initName, password, initSchemasStr, initDefSchema, caseInfo));
			trspermission.createUser(initName, password, initSchemasStr, initDefSchema, caseInfo, true);
			sleep();
			/* 获得用户, 重新设置基本信息, 提交到服务器 */
			TRSUser user = trspermission.getUserInfo(initName);
			/* 从服务器端获取来的用户, 不支持修改用户名 */
			user.setName(newName);
			user.SetSchemas(arrayToSet(newSchemas));
			user.setDefSchema(newDefSchema);
			LOGGER.debug(String.format("trspermission.getUserInfo(%s).setName(%s).setSchemas(%s).setDefSchema(%s)", 
					initName, newName, arrayToSet(newSchemas), newDefSchema));
			try {
				/* 提交的新信息有可能非法,需要加try-catch */
				LOGGER.debug(String.format("trspermission.updateUser(%s, user)", initName));
				trspermission.updateUser(initName, user);
				if(!expectedUpdateSucceed)
					fail(String.format("将用户[%s]的名称由[%s]变更为[%s], 模式列表由%s变更为%s, 默认模式由%s变更为%s, 预期抛出异常, 实际成功", 
							initName, initName, newName, initSchemasStr, arrayToSet(newSchemas), initDefSchema, newDefSchema));
			}catch(TRSException e) {
				LOGGER.debug(String.format("errorCode=%d, expected=%d, errorString=%s", 
						e.getErrorCode(), expectedErrorCode, e.getErrorString()));
				assertEquals(e.getErrorCode(), expectedErrorCode);
				return;
			}
			sleep();
			/* 重新获得用户, 验证信息 */
			LOGGER.debug(String.format("user = trspermission.getUserInfo(%s)", newName));
			user = trspermission.getUserInfo(newName);
			if(user == null) {
				fail(String.format("获取用户[%s]返回为空, 预期获取成功", newName));
			}
			LOGGER.debug(String.format("user.getName=%s, expected=%s", user.getName(), expectedName));
			assertEquals(user.getName(), expectedName);
			Set<String> actualSchemas = user.getSchemas();
			LOGGER.debug(String.format("actualSchemas=%s, expected=%s", actualSchemas, arrayToString(expectedSchemas)));
			for(String expectedNewSchema : expectedSchemas)
				actualSchemas.remove(expectedNewSchema);
			assertTrue(actualSchemas.isEmpty());
			LOGGER.debug(String.format("user.getDefSchema=%s, expected=%s", user.getDefSchema(), expectedDefSchema));
			assertEquals(user.getDefSchema(), expectedDefSchema);
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(newName);
			} catch (TRSException e) {}
			try {
				trspermission.deleteUser(initName);
			}catch(TRSException e) {}
		}
	}
	
	/**
	 * 用户改名, 名称重复, 抛出异常
	 */
	@Test
	public void setNameDuplication() {
		LOGGER.debug("setNameDuplication");
		String name = "common_"+System.currentTimeMillis();
		String password = "1234qwer";
		String otherName = "other_"+System.currentTimeMillis();
		try {
			trspermission.createUser(name, password, "system", "system",  "",  true);
			sleep();
			trspermission.createUser(otherName, password, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(otherName);
			user.setName(name);
			try {
				trspermission.updateUser(otherName, user);
			} catch(TRSException e) {
				assertEquals(e.getErrorCode(), 9920034);
			}
		} catch (TRSException e) {
			fail(Other.stackTraceToString(e));
		} finally {
			try {
				trspermission.deleteUser(name);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				trspermission.deleteUser(otherName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
}
