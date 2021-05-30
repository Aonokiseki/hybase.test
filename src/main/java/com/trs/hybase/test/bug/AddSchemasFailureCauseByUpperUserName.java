package com.trs.hybase.test.bug;

import java.util.Arrays;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.TRSUser.APIPermission;
import com.trs.hybase.client.params.ConnectParams;

public class AddSchemasFailureCauseByUpperUserName {
	
	/*
	 * 问题: 用户名大写, 给普通用户添加模式并更新到服务器后, 再从服务器端获取此用户的模式, 则新追加的模式获取不到
	 */
	
	public static void main(String[] args) {
		String host = "http://192.168.105.190:5555";
		/* 换成大写, 则 TRSUser.getSchemas() 返回值不符合预期 */
		String commonUser = "ZHAOYANG";
		String commonPasswd = "1234qwer";
		String customSchema = "custom";
		String[] nodes = new String[] {"192.168.105.190:5555", "192.168.105.191:5555", "192.168.105.192:5555"};
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		
		try {
			/* 创建模式"custom" */
			trspermission.createSchema(customSchema, 6,  Arrays.asList(nodes));
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			/* 创建用户"zhaoyang" */
			trspermission.createUser(commonUser, commonPasswd, "system", "system", "新建一个普通用户", true);
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			/* 服务器端获取用户"zhaoyang" */
			TRSUser user = trspermission.getUserInfo(commonUser);
			
			/* 给"zhaoyang"增加模式"custom"和权限 */
			user.addApiPermission(APIPermission.all);
			user.addSchemas(new String[] {customSchema});
			
			/* 更新用户 */
			trspermission.updateUser(commonUser, user);
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			/* 再次获取用户"zhaoyang", 打印模式集, 现在应该有 system 和 custom 两个 */
			user = trspermission.getUserInfo(commonUser);
			System.out.println(String.format("[%s].getSchemas()=%s", user.getName(), user.getSchemas()));
			
			System.out.println(String.format("TRSConnection _conn = new TRSConnection(%s, %s, %s, new ConnectParams())", 
					host, commonUser, commonPasswd));
			TRSConnection _conn = new TRSConnection(host, commonUser, commonPasswd, new ConnectParams());
			TRSDatabase db = new TRSDatabase(customSchema+".demo", 1, TRSDatabase.DBPOLICY.FASTEST);
			db.addColumn(new TRSDatabaseColumn("id", TRSDatabaseColumn.TYPE_CHAR));
			db.addColumn(new TRSDatabaseColumn("doc", TRSDatabaseColumn.TYPE_DOCUMENT));
			System.out.println("_conn.createDatabase("+db.getName()+")");
			_conn.createDatabase(db);
			System.out.println("End");
		}catch(TRSException e) {
			e.printStackTrace();
		}finally {
			try {
				conn.deleteDatabase(customSchema+".demo");
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				trspermission.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				trspermission.deleteSchema(customSchema);
			}catch(TRSException e) {
				e.printStackTrace();
			}
			trspermission.close();
		}
		conn.close();
	}
}
