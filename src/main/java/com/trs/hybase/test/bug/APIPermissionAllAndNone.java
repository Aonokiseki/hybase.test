package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

public class APIPermissionAllAndNone {
	
	public static void main(String[] args) {
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String pwd = "1234qwer";
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			trspermission.createUser(commonUser, pwd, "system", "system", "", false);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			/* 同时设置all和none权限, 预期抛出异常 */
			user.addApiPermission(APIPermission.all);
			user.addApiPermission(APIPermission.none);
			trspermission.updateUser(commonUser, user);
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
