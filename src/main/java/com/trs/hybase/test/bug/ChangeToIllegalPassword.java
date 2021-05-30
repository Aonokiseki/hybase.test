package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

public class ChangeToIllegalPassword {
	
	public static void main(String[] args) {
		TRSConnection conn = new TRSConnection("http://192.168.105.190:5555", "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		
		String commonUser = "zhaoyang_"+System.currentTimeMillis();
		String pwd = "1234qwer";
		
		try {
			trspermission.createUser(commonUser, pwd, "system", "system", "", true);
			sleep();
			TRSUser user = trspermission.getUserInfo(commonUser);
			user.addApiPermission(APIPermission.all);
			trspermission.updateUser(commonUser, user);
			sleep();
			TRSConnection _conn = new TRSConnection("http://192.168.105.190:5555", commonUser, pwd, new ConnectParams());
			TRSPermissionClient _trspermission = new TRSPermissionClient(_conn);
			_trspermission.changePwd(commonUser, pwd, "");
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
