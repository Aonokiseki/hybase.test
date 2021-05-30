package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

public class ForceCommonUserToRemoveAddApiPermissionWithoutException {
	
	public static void main(String[] args) {
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String pwd = "1234qwer";
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			/* 新建普通用户 */
			trspermission.createUser(commonUser, pwd, "system", "system", "", true);
			sleep();
			/* 赋予API所有权限 */
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			user = null;
			sleep();
			/* 普通用户登录, 删除自己API所有权限 */
			TRSConnection _conn = new TRSConnection(host, commonUser, pwd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			user = _trspermission.getUserInfo(commonUser);
			user.removeApiPermission(APIPermission.all);
			/* 提交应当抛出异常 */
			_trspermission.updateUser(commonUser, user);
			System.out.println("不应该看到这句话");
		} catch (TRSException e) {
			e.printStackTrace();
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			} catch (TRSException e) {
				e.printStackTrace();
			}
		}
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
