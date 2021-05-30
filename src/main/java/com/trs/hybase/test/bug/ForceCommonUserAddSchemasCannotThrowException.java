package com.trs.hybase.test.bug;

import java.util.Arrays;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

public class ForceCommonUserAddSchemasCannotThrowException {
	
	/* 问题: 普通用户为自己追加模式, 更新未能抛出异常 */
	
	public static void main(String[] args) {
		
		String host = "http://192.168.105.190:5555";
		
		String commonUser = "zhaoyang";
		String password = "1234qwer";
		String schemaName = "custom";
		String[] nodes = new String[] {
				"192.168.105.190:5555", 
				"192.168.105.191:5555", 
				"192.168.105.192:5555"
		};
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		
		TRSConnection _conn = null;
		TRSPermissionClient _trspermission = null;
		try {
			/* admin 新建模式 custom */
			trspermission.createSchema(schemaName, 6, Arrays.asList(nodes));
			sleep();
			/* admin 新建普通用户 commonUser */
			trspermission.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			/* admin 获得普通用户 commonUser, 改权限然后更新 */
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			/* commonUser 连接集群 */
			_conn = new TRSConnection(host, commonUser, password, new ConnectParams());
			_trspermission = new TRSPermissionClient(conn);
			/* commonUser 从服务器端获取自己 */
			TRSUser _user = _trspermission.getUserInfo(commonUser);
			/* commonUser 为自己增加一个模式 custom */
			_user.addSchema(schemaName);
			/* commonUser 更新自己 */
			_trspermission.updateUser(commonUser, _user);
			System.out.println("不应该看到这句话");
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
