package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.params.ConnectParams;

public class AdminDeleteHimself {
	
	public static void main(String[] args) {
		
		TRSConnection conn = new TRSConnection("http://192.168.105.190:5555", "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			trspermission.deleteUser("admin");
			System.out.println("不应该看见这句话");
		} catch (TRSException e) {
			e.printStackTrace();
		}
		conn.close();
	}
}
