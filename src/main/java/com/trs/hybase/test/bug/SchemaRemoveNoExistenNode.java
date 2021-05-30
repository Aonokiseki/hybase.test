package com.trs.hybase.test.bug;

import java.util.Arrays;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSSchema;
import com.trs.hybase.client.params.ConnectParams;

/* 模式移除一个不存在的节点, 预期抛出异常, 实际未抛出 */
public class SchemaRemoveNoExistenNode {
	
	public static void main(String[] args) {
		
		String[] servers = new String[] {
			"192.168.105.190:5555",
			"192.168.105.191:5555",
			"192.168.105.192:5555"
		};
		
		String schemaName = "removeschema_"+System.currentTimeMillis();
		
		TRSConnection conn = new TRSConnection("http://192.168.105.190:5555", "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		try {
			trspermission.createSchema(schemaName, 6, Arrays.asList(servers));
			sleep();
			TRSSchema schema = trspermission.getSchema(schemaName);
			schema.removeNode("192.168.0.102:5555");
			schema.clearPartition();
			trspermission.updateSchema(schema);
			System.out.println("不希望看到这句话");
		} catch (TRSException e) {
			e.printStackTrace();
		} finally {
			try {
				trspermission.deleteSchema(schemaName);
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
