package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

/* 问题: 修改用户权限为all或none, 预期抛出异常, 实际没有 */
public class ModifyUserPermissionToAllAndNone {
	
	public static void main(String[] args) {
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String password = "1234qwer";
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			trspermission.createUser(commonUser, password, "system", "system", "comment", true);
			sleep();
			/* 修改部分权限, 是[search,insert,createdb,deletedb,hadoop]的任意组合 */
			trspermission.modifyUserApiPermission(commonUser, APIPermission.all, APIPermission.none);
			System.out.println("不应该看到这句话");
		} catch (TRSException e) {
			e.printStackTrace();
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
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
