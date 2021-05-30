package com.trs.hybase.test.bug;

import java.util.Arrays;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.params.ConnectParams;

/* 问题: 和添加模式一样, 移除模式时遇到大写用户名也有移除无效的现象 */
public class RemoveSchemaFailureCauseByUpperUserName {
	
	public static void main(String[] args) {
		String host = "http://192.168.105.190:5555";
		String commonUser = "ZHAOYANG";
		String password = "1234qwer";
		String schemaName = "custom";
		String[] nodes = new String[] {
			"192.168.105.190:5555",
			"192.168.105.191:5555",
			"192.168.105.192:5555"
		};
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			trspermission.createSchema(schemaName, 6, Arrays.asList(nodes));
			sleep();
			trspermission.createUser(commonUser, password, "system,custom", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			/* 更新前打印模式列表 */
			System.out.println(user.getName()+".getSchemas="+user.getSchemas());
			user.removeSchema("system");
			/* 更新用户 */
			trspermission.updateUser(commonUser, user);
			sleep();
			user = trspermission.getUserInfo(commonUser);
			/* 更新后打印模式列表 */
			System.out.println(user.getName()+".getSchemas="+user.getSchemas());
		} catch (TRSException e) {
			e.printStackTrace();
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
