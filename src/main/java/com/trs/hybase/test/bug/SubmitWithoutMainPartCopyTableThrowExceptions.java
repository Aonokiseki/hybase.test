package com.trs.hybase.test.bug;

import java.util.Arrays;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSSchema;
import com.trs.hybase.client.params.ConnectParams;

/*
 * 之前未提交主副分区表(或调用 clearPartition() 清理主副分区映射) 是由系统自动生成的
 * 
 * 意味着可以利用clearPartition()这个技巧让系统自动配置主副分区映射表
 * 
 * 后来在bug1278中变更了策略: 已修改主副分区有一个为null则抛出异常
 * 
 * http://192.168.105.35:81/zentao/bug-view-1278.html
 * 
 * 这又带来了一个新的问题: 调用clearPartition()会直接清理掉主副分区映射表, 若再不手工提供, 则会抛出异常
 * 
 * 这样一来无法令系统替用户自动配置主副分区映射表
 * 
 * 现在需要明确的是, 今后就要求用户手工提交主副分区映射, 还是可以选择令系统自动配置?
 */
public class SubmitWithoutMainPartCopyTableThrowExceptions {
	
	public static void main(String[] args) {
		TRSConnection conn = new TRSConnection("http://192.168.105.190:5555", "admin", "trsadmin", new ConnectParams());
		
		String[] nodes = new String[] {"192.168.105.190:5555", "192.168.105.191:5555", "192.168.105.192:5555"};
		
		TRSPermissionClient trspermission = new TRSPermissionClient(conn);
		
		String schemaName = "custom_"+System.currentTimeMillis();
		try {
			trspermission.createSchema(schemaName, 6, Arrays.asList(nodes));
			sleep();
			TRSSchema schema = trspermission.getSchema(schemaName);
			/* 移除一个节点 */
			schema.removeNode(nodes[0]);
			/* 之前利用这个方法清理掉当前的主副分区映射, 然后由系统自动配置 */
			schema.clearPartition();
			/* 这里抛出异常, 未设置主分区映射 */
			trspermission.updateSchema(schema);
		} catch (TRSException e) {
			e.printStackTrace();
		} finally {
			try {
				trspermission.deleteSchema(schemaName);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void sleep() {
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
