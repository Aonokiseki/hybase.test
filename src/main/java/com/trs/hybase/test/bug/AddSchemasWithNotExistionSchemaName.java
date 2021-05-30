package com.trs.hybase.test.bug;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

public class AddSchemasWithNotExistionSchemaName {
	
	/* 问题: 追加用户模式,添加一个不存在的模式，然后更新用户, 本应抛出异常，实际没抛出*/
	
	public static void main(String[] args) {
		
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String commonPasswd = "1234qwer";
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		
		/* 没有新建模式这一步 */
		
		try {
			/* 新建用户 */
			trspermission.createUser(commonUser, commonPasswd, "system",  "system", "", true);
			sleep();
			
			/* 服务器端获取用户 */
			TRSUser user = trspermission.getUserInfo(commonUser);
			
			/* 追加模式, 模式名是时间戳, 保证不存在 */
			user.addApiPermission(APIPermission.all);
			user.addSchema(String.valueOf(System.currentTimeMillis()));
			
			/* 更新用户, 应当抛出异常 */
			trspermission.updateUser(commonUser, user);
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
