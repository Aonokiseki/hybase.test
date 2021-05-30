package com.trs.hybase.test.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.test.bean.Constants;
import com.trs.hybase.test.pojo.SubDatabase;

public class Tools {
	private Tools() {}
	/**
	 * 将TRSRecord对象的每个字段值拼接成一个字符串
	 * @param record
	 * @return
	 * @throws TRSException
	 */
	public static String recordToString(TRSRecord record) throws TRSException {
		StringBuilder sb = new StringBuilder();
		sb.append("Record [");
		String[] columnNames = record.getColumnNames();
		String value = null;
		for(int i=0; i<columnNames.length; i++) {
			value = record.getString(columnNames[i]);
			if(value == null) value = "";
			value = value.substring(0, Math.min(value.length(), 20));
			sb.append(columnNames[i]).append("=").append(value);
			if(i < columnNames.length - 1)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}
	/**
	 * 构造日期分库参数
	 * @param splitDateStart
	 * @param splitDateEnd
	 * @param splitDateLevel
	 * @return
	 */
	public static Map<String,String> createSplitParams(
			LocalDateTime splitDateStart, LocalDateTime splitDateEnd, String splitDateLevel){
		String splitDateStartStr = splitDateStart.format(Constants.DEFAULT_DATE_TIME_FORMATTER).substring(0, 10);
		String splitDateEndStr = splitDateEnd.format(Constants.DEFAULT_DATE_TIME_FORMATTER).substring(0, 10);
		Map<String,String> splitParams = new HashMap<String,String>();
		splitParams.put("split.date.start", splitDateStartStr);
		splitParams.put("split.date.end", splitDateEndStr);
		splitParams.put("split.date.level", splitDateLevel);
		return splitParams;
	}
	/**
	 * 解析主机列表字符串(String), 转换为节点列表(List), 如<br><br>
	 * 参数: 192.168.0.101:5555;192.168.0.102:5555;192.168.0.103:5555<br>
	 * 返回:<br>
	 * <ul>
	 * 	<li>192.168.0.101:5555</li>
	 * 	<li>192.168.0.102:5555</li>
	 * 	<li>192.168.0.103:5555</li>
	 * </ul>
	 * 
	 * @param hostsStr
	 * @return
	 */
	public static List<String> splitHostsToList(String hostsStr) {
		List<String> hosts = Arrays.asList(hostsStr.split("[\\,\\;]"));
		return hosts;
	}
	/**
	 * 解析主机列表字符串(String), 为每个主机追加 http://, 如<br><br>
	 * 参数: 192.168.0.101:5555;192.168.0.102:5555;192.168.0.103:5555<br>
	 * 返回: http://192.168.0.101:5555;http://192.168.0.102:5555;http://192.168.0.103:5555
	 * 
	 * @param hostsStr
	 * @return
	 */
	public static String hostsAppendHttpHeader(String hostsStr) {
		List<String> hosts = Arrays.asList(hostsStr.split("[\\,\\;]"));
		StringBuilder sb = new StringBuilder();
		for(int i=0,size=hosts.size(); i<size; i++) {
			sb.append("http://").append(hosts.get(i));
			if(i < size - 1)
				sb.append(";");
		}
		return sb.toString();
	}
	/**
	 * 获取数据库的所有子库列表
	 * @param conn
	 * @param dbName
	 * @return
	 * @throws TRSException
	 */
	public static List<SubDatabase> getSubdbsFromDatabase(TRSConnection conn, String dbName) throws TRSException{
		List<SubDatabase> subdatabases = new ArrayList<SubDatabase>();
		/* 举个例子, subdatabasesStr=000@modify.demo#007,192.168.101.243,5555;001@modify.demo#007,192.168.101.243,5555;...*/
		String subdatabasesStr = getSubDatabaseStr(conn, dbName);
		int loop = Constants.MAX_LOOP_SIZE;
		while((subdatabasesStr == null || subdatabasesStr.isEmpty()) && loop -- > 0) {
			try {
				Thread.sleep(Constants.WAITINGTIME_EACH_LOOP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			subdatabasesStr = getSubDatabaseStr(conn, dbName);
		}
		if(subdatabasesStr == null || subdatabasesStr.isEmpty())
			return subdatabases;
		String[] subdatabaseArray = subdatabasesStr.split(";");
		String subdatabaseInstanceStr = null;
		String[] subdatabasesInstanceInfos = null;
		SubDatabase subdatabase = null;
		/* 用英文分号分隔之后, 数组的每个元素形式为: 000@modify.demo#007,192.168.101.243,5555 */
		for(int i=0; i<subdatabaseArray.length; i++) {
			subdatabaseInstanceStr = subdatabaseArray[i];
			/* 把 @ 和 # 替换成半角英文逗号, 这样单个串变为 000,modify.demo,007,192.168.101.243,5555 */
			subdatabaseInstanceStr = subdatabaseInstanceStr.replaceAll("[@#]", ",");
			subdatabasesInstanceInfos = subdatabaseInstanceStr.split(",");
			subdatabase = SubDatabase.build();
			/* 再把这个串按照逗号分隔, 能分成4~5个元素
			 * 
			 * 视图类型数据库会分隔成
			 * 0:   000
			 * 1:   modify.demo
			 * 2:   007
			 * 3:   192.168.101.243
			 * 4:   5555  
			 * 
			 * 单节点数据库会分割成
			 * 0:   000
			 * 1:   modify.demo
			 * 2:   192.168.105.191
			 * 3:   5555 */
			if(subdatabasesInstanceInfos.length == 4){
				subdatabase.setSubDatabaseName(subdatabasesInstanceInfos[0]);
				subdatabase.setDatabaseName(subdatabasesInstanceInfos[1]);
				subdatabase.setPartName("000");
				subdatabase.setHost(subdatabasesInstanceInfos[2]);
				subdatabase.setPort(subdatabasesInstanceInfos[3]);
			}else {
				subdatabase.setSubDatabaseName(subdatabasesInstanceInfos[0]);
				subdatabase.setDatabaseName(subdatabasesInstanceInfos[1]);
				subdatabase.setPartName(subdatabasesInstanceInfos[2]);
				subdatabase.setHost(subdatabasesInstanceInfos[3]);
				subdatabase.setPort(subdatabasesInstanceInfos[4]);
			}
			subdatabases.add(subdatabase);
		}
		return subdatabases;
	}
	
	private static String getSubDatabaseStr(TRSConnection conn, String dbName) throws TRSException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("dbname", dbName);
		params.put("subdb.location.output", "true");
		return conn.executeCommand("list_subdb", params);
	}
	/**
	 * 主分区映射表按key排序, 每个value按数值升序排序
	 * @param table
	 * @return
	 */
	public static Map<String, List<Integer>> sortMainCopyTable(Map<String, List<Integer>> table){
		List<Entry<String, List<Integer>>> tableEntries = new ArrayList<Entry<String, List<Integer>>>();
		for(Entry<String, List<Integer>> entry : table.entrySet())
			tableEntries.add(entry);
		Collections.sort(tableEntries, new Comparator<Entry<String, List<Integer>>>(){
			public int compare(Entry<String, List<Integer>> o1, Entry<String, List<Integer>> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		LinkedHashMap<String, List<Integer>> result = new LinkedHashMap<String,List<Integer>>();
		String key = null; List<Integer> value = null;
		for(int i=0, size=tableEntries.size(); i<size; i++) {
			key = tableEntries.get(i).getKey();
			value = tableEntries.get(i).getValue();
			Collections.sort(value);
			result.put(key, value);
		}
		return result;
	}
}
