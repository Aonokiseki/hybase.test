package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

/* 问题: API赋予一个普通用户 all 权限后, 则 TRSUser.*Able() 方法均应该返回true, 实际没有
 * 破坏了一致性 */

public class ApiPermissionConsistency {
	public static void main(String[] args) {
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String password = "1234qwer";
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			trspermission.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			/* 添加所有权限 */
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			user = trspermission.getUserInfo(commonUser);
			System.out.println(String.format("apiUnlimitAble=%b, createdbAble=%b, deleteAble=%b, hadoopAble=%b, insertAble=%b, searchAble=%b",
					user.apiUnlimitAble(), user.createdbAble(), user.deletedbAble(), user.hadoopAble(), user.insertAble(), user.searchAble()));
		} catch (TRSException e) {
			e.printStackTrace();
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
