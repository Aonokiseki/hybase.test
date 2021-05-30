package com.trs.hybase.test.bug;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSUser;
import com.trs.hybase.client.params.ConnectParams;

/*
 * 问题: removeIpBlackList 参数和目的不明确, 导致移除没有生效 
 */
public class RemoveIpsFailure {
	
	public static void main(String[] args) {
		
		String host = "http://192.168.105.190:5555";
		String commonUser = "zhaoyang";
		String password = "1234qwer";
		String localHost = getFirstLocalIp();
		String[] blackIps = new String[] {localHost};
		String[] whiteIps = new String[] {localHost};
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		TRSPermissionClient client = new TRSPermissionClient(conn);
		try {
			client.createUser(commonUser, password, "system", "system", "", true);
			sleep();
			TRSUser user = client.getUserInfo(commonUser);
			/* 初始黑白ip均为本机 */
			user.setIpBlackList(Arrays.asList(blackIps));
			user.setIpWhiteList(Arrays.asList(whiteIps));
			client.updateUser(commonUser, user);
			sleep();
			user = client.getUserInfo(commonUser);
			/*
			 * API的方法如下:
			 * public void removeIpBlackList(String ipBlackList) {
			 * 		if (this.ipBlackList == null) {
			 *			this.ipBlackList = new ArrayList();
    		 *		}
    		 *		this.ipBlackList.remove(ipBlackList);
    		 *	}
    		 *
    		 * 看参数名, 似乎是希望传递一个 String 类型的 ip 列表
    		 * 但是实现的时候并没有解析这个串, 假如有两个元素(或者只有一个元素,但是在拼装时带上了分号或逗号), 就没有效果了
    		 * 换言之, 仅支持1个ip的移除。
    		 * 
    		 * 注意最后追加了一个分号
			 */
			user.removeIpBlackList(String.format("%s;", localHost));
			client.updateUser(commonUser, user);
			sleep();
			user = client.getUserInfo(commonUser);
			System.out.println("user.getIpBlackList()="+user.getIpBlackList());
		} catch (TRSException e1) {
			e1.printStackTrace();
		} finally {
			try {
				client.deleteUser(commonUser);
			}catch(TRSException e) {
				e.printStackTrace();
			}
		}
		conn.close();
	}
	
	private static void sleep(){
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static String getFirstLocalIp() {
		List<String> ips = new ArrayList<String>();
		try {
			ips = getLocalIps();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		String regex = "^\\d{1,3}.\\d{1,3}.[5][1|6].\\d+$";
		for(int i=0, size=ips.size(); i<size; i++) {
			if(ips.get(i).matches(regex)) {
				return ips.get(i);
			}
		}
		return "127.0.0.1";
	}
	
	/**
     * 获取本机ip地址
     * @return
     * @throws SocketException
     */
    public static List<String> getLocalIps() throws SocketException{
    	List<String> ips = new ArrayList<String>();
    	Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
    	NetworkInterface networkInterface = null;
    	Enumeration<InetAddress> inetAddressEnum = null;
    	InetAddress inetAddress = null;
    	while(enumeration.hasMoreElements()) {
    		networkInterface = enumeration.nextElement();
    		if(networkInterface.isLoopback() || networkInterface.isVirtual())
    			continue;
    		inetAddressEnum = networkInterface.getInetAddresses();
    		while(inetAddressEnum.hasMoreElements()) {
    			inetAddress = inetAddressEnum.nextElement();
    			if(inetAddress.isLoopbackAddress() || !inetAddress.isSiteLocalAddress() || inetAddress.isAnyLocalAddress())
    				continue;
    			ips.add(inetAddress.getHostAddress());
    		}
    	}
    	return ips;
    }
}
