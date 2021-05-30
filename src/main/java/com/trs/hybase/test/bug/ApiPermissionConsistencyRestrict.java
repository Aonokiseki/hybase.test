package com.trs.hybase.test.bug;

import java.util.Arrays;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

/*
 * 讨论一下API权限中的一致问题:
 * 
 * 现在的权限里包含  all, createdb, insert, search, hadoop, deletedb, none
 * all 等同于 createdb, insert, search, hadoop, deletedb 的总和，none 等同于 没有
 * 
 * 先不讨论all和none放入枚举中是否合适, 然后get方法返回值的问题见ApiPermissionConsistency.java, 这里也不提了
 * 
 * 这里讨论的是一致性在使用中的问题, 举个例子:
 * 
 * 1. 普通用户 zhaoyang 获得了 all 权限
 * 2. 普通用户 zhaoyang 又被剥夺了 deletedb,insert,search,hadoop,none 权限 (只剩下建表权限)
 * 3. 普通用户 zhaoyang 建了个表, 抛出了异常, 没有权限
 */

public class ApiPermissionConsistencyRestrict {
	public static void main(String[] args) {
		
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String password = "1234qwer";
		String[] nodes = new String[] {
			"192.168.105.190:5555",
			"192.168.105.191:5555",
			"192.168.105.192:5555"
		};
		String schemaName = "custom";
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			/* 新建模式 */
			trspermission.createSchema(schemaName, 6, Arrays.asList(nodes));
			sleep();
			/* 新建用户 */
			trspermission.createUser(commonUser, password, schemaName, schemaName, "", true);
			sleep();
			/* admin获得此用户, 添加所有权限, 然后提交到服务器 */
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			/* 再获取一次, 去掉一部分权限(只剩下建表权限了), 然后提交到服务器 */
			user = trspermission.getUserInfo(commonUser);
			user.removeApiPermission(APIPermission.none);
			user.removeApiPermission(APIPermission.deletedb);
			user.removeApiPermission(APIPermission.hadoop);
			user.removeApiPermission(APIPermission.insert);
			user.removeApiPermission(APIPermission.search);
			trspermission.updateUser(commonUser, user);
			sleep();
			/* 普通用户建立连接, 建表 */
			TRSConnection _conn = new TRSConnection(host, commonUser, password, new ConnectParams());
			TRSDatabase db = new TRSDatabase(schemaName + ".zhaoyangcreate", 1, TRSDatabase.DBPOLICY.FASTEST);
			db.addColumn(new TRSDatabaseColumn("id", TRSDatabaseColumn.TYPE_CHAR));
			_conn.createDatabase(db);
			System.out.println("完成");
		} catch (TRSException e) {
			e.printStackTrace();
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				conn.deleteDatabase(schemaName + ".zhaoyangcreate");
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				trspermission.deleteUser(schemaName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
		trspermission.close();
		conn.close();
	}
	
	private static void sleep() {
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
