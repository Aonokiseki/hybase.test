package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.params.ConnectParams;

/* 移除一个不存在的模式, 预期抛出异常, 实际未抛出 */
public class UserRemoveNoExistenSchema {
	
	public static void main(String[] args) {
		TRSConnection conn = new TRSConnection("http://192.168.105.190:5555", "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		
		String commonUser = "zhaoyang";
		String password = "1234qwer";
		
		try {
			trspermission.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.removeSchema("unknown");
			trspermission.updateUser(commonUser, user);
			System.out.println("不希望看到这句话");
		} catch (TRSException e) {
			e.printStackTrace();
		} finally {
			try {
				trspermission.deleteUser(commonUser);
			} catch (TRSException e) {
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
