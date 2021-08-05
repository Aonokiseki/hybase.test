package com.trs.hybase.test.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;


public final class Other {
	private Other(){}
	
	public enum SizeUnit{
		B("B"),KB("KB"),MB("MB"),GB("GB"),TB("TB");
		
		private String unit;
		private SizeUnit(String unit){
			this.unit = unit;
		}
		@Override
		public String toString(){
			return this.unit;
		}
	}
	    
	public final static double ONE_KB = 1024.0;
	public final static double ONE_MB = 1048576.0;
	public final static double ONE_GB = 1073741824.0;
	public final static double ONE_TB = 1099511627776.0;
	 /**
     * 获得当前正在执行的方法名称
     */
    public static String getMethodName(){
    	StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();  
	    StackTraceElement e = stacktrace[2];  
	    String methodName = e.getMethodName();  
	    return methodName;
    }
    /**
     * 序列指定两端倒置
     * @param array
     * @param left
     * @param right
     */
    public static void sequenceInversion(double[] array, int left, int right){
    	if(left < 0 || right > array.length-1 || left >= right)
    		return;
    	int i = left;
    	int j = right;
    	double temp;
    	while(i++<j--){
    		temp = array[i];
    		array[i] = array[j];
    		array[j] = temp;
    	}
    }
    /**
     * 序列指定两端倒置
     * @param array
     * @param left
     * @param right
     */
    public static void sequenceInversion(int[] array, int left, int right){
    	if(left < 0 || right > array.length-1 || left >= right)
    		return;
    	int i = left;
    	int j = right;
    	int temp;
    	while(i++<j--){
    		temp = array[i];
    		array[i] = array[j];
    		array[j] = temp;
    	}
    }
    /**
     * 交换数组中两个元素的位置
     * @param array
     * @param index1
     * @param index2
     */
    public static  void exchangeTwoElementOfArray(int[] array, int index1, int index2){
    	if(index1 < 0 || index1 > (array.length - 1) || index2 < 0 || index2 > (array.length - 1))
    		return;
    	int temp = array[index1];
    	array[index1] = array[index2];
    	array[index2] = temp;
    }
    /**
     * 交换数组中两个元素的位置
     * @param array
     * @param index1
     * @param index2
     */
    public static void exchangeTwoElementOfArray(double[] array, int index1, int index2){
    	if(index1 < 0 || index1 > (array.length - 1) || index2 < 0 || index2 > (array.length - 1))
    		return;
    	double temp = array[index1];
    	array[index1] = array[index2];
    	array[index2] = temp;
    }
    /**
     * 返回指定圆形范围内的一个坐标<br><br>
     * 二维圆坐标公式<br>
     * θ∈[0, 2π]<br>
     * x = R * cos(θ) + x<br>
     * y = R * sin(θ) + y<br><br>
     * 返回一个坐标, 位于以 (x, y) 为圆心, 长度为maxRadius的圆上
     * 
     * @param x 
     * @param y 
     * @param maxRadius 
     * @return TwoTuple
     */
    public static Tuple.Two<Double,Double> getRandomCoordinateByCircle(double x, double y, double maxRadius){
    	double theta = Math.random() * 2 * Math.PI;
    	x = Math.random() * maxRadius * Math.cos(theta) + x;
    	y = Math.random() * maxRadius * Math.sin(theta) + y;
    	return new Tuple.Two<Double, Double>(x, y);
    }
    /**
     * 返回指定球形范围内的一个坐标<br><br>
     * 三维求坐标公式<br>
     * θ∈[0, 2π], φ∈[0, π]<br>
     * x = R * cos(θ) * sin(φ)<br>
     * y = R * sin(θ) * sin(φ)<br>
     * z = R * cos(φ)<br><br>
     * 返回一个坐标, 这个坐标位于以(x, y, z)为球心, 长度为maxRadius的球内
     * @param x 
     * @param y 
     * @param z 
     * @param maxRadius 
     * @return ThreeTuple
     */
    public static Tuple.Three<Double,Double,Double> getRandomCoordinateBySphere(
    		double x, double y, double z, double maxRadius){
    	double theta = Math.random() * 2 * Math.PI;
    	double phi = Math.random() * Math.PI;
    	x = Math.random() * maxRadius * Math.cos(theta) * Math.sin(phi) + x;
    	y = Math.random() * maxRadius * Math.sin(theta) * Math.sin(phi) + y;
        z = Math.random() * maxRadius * Math.cos(phi) + z;
        return new Tuple.Three<Double, Double, Double>(x, y, z);
    }
    /**
     * 返回平面矩形范围内一个随机坐标<br>
     * 坐标位于 (x, y) 和 (x+length, y+width) 之间
     * @param x 
     * @param y 
     * @param length 
     * @param width 
     * @return TwoTuple
     */
    public static Tuple.Two<Double,Double> getRandomCoordinateBySquare(
    		double x, double y, double length, double width){
    	double pX = Math.random() * length + x;
    	double pY = Math.random() * width + y;
    	return new Tuple.Two<Double,Double>(pX, pY);
    }
    /**
     * 返回空间范围内的一个随机坐标<br>
     * 坐标位于 (x, y, z) 到 (x+length, y+width, z+height) 围成的一个长方体中
     * @param x 
     * @param y 
     * @param z 
     * @param length 
     * @param width 
     * @param heigh 
     * @return ThreeTuple
     */
    public static Tuple.Three<Double,Double,Double> getRandomCoordinateByCuboid(
    		double x, double y, double z, double length, double width, double heigh){
    	double pX = Math.random() * length + x;
    	double pY = Math.random() * width + y;
    	double pZ = Math.random() * heigh + z;
    	return new Tuple.Three<Double,Double,Double>(pX, pY, pZ);
    }
    
    /**
     * 返回堆栈字符串
     * 
     * @param throwable
     * @return String 堆栈信息
     */
    public static String stackTraceToString(Throwable throwable){
    	StringWriter sw = new StringWriter();
 	    throwable.printStackTrace(new PrintWriter(sw, true));
 	    return sw.getBuffer().toString();
    }
    
    /**
     * 获取当前正在运行的线程列表
     * 
     * @return Thread[]
     */
    public static Thread[] getListThreads(){
		Thread[] lstThreads;
		ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
		int noThreads = currentGroup.activeCount();
	    lstThreads = new Thread[noThreads];
	    currentGroup.enumerate(lstThreads);
	    return lstThreads;
    }
    
    /**
     * 交换列表中两个元素
     * @param list 目标列表
     * @param index1 第一个交换元素的位置
     * @param index2 第二个交换元素的位置
     */
    public static <T> void exchangeTwoElementOfList(List<T> list, int index1, int index2){
    	if(index1 < 0 || (index1 > list.size() - 1) || index2 < 0 || (index2 > list.size() - 1))
    		return;
    	list.add(index1, list.get(index2));
		list.add(index2 + 1, list.get(index1));
		list.remove(index1);
		list.remove(index2);
    }
    /**
     * 获取磁盘空间信息
     * @param disks 目标磁盘列表,为空时获取全部磁盘
     * @param sizeUnit 空间大小返回值的单位, 可选B|KB|MB|GB|TB
     * @return <code>Map&ltString, Map&ltString,String&gt&gt</code> <br/>
     * 外层键值对表示每个盘符和其对应的信息;<br>
     * 内层键值对表示单个盘符不同信息的结果,取值如下:<br>
     * <code>TotalSpace</code> 总空间<br>
     * <code>FreeSpace</code> 剩余空间<br>
     * <code>Free/Total</code> 二者的比值<br>
     */
    public static Map<String,Map<String,String>> diskSpaceInfo(List<String> disks, SizeUnit sizeUnit){
    	Map<String,Map<String,String>> result = new HashMap<String,Map<String,String>>();
    	Map<String,String> eachDisk = null;
    	FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    	File[] files = File.listRoots();
    	for(int i=0; i<files.length; i++){
    		if(disks == null || disks.isEmpty());
    		else if(!disks.contains(files[i].getName()))
    			continue;
    		eachDisk = new HashMap<String,String>();
    		eachDisk.put("TotalSpace", FormatFileSize(files[i].getTotalSpace(), sizeUnit));
    		eachDisk.put("FreeSpace",  FormatFileSize(files[i].getFreeSpace(), sizeUnit));
    		eachDisk.put("Free/Total", new DecimalFormat("#0.00").format((double)files[i].getFreeSpace() /
    				(double)files[i].getTotalSpace()));
    		result.put(fileSystemView.getSystemDisplayName(files[i]), eachDisk);
    	}
    	return result;
    }
    /**
     * 格式化显示磁盘空间大小, 保留2位小数
     * @param fileSize 磁盘空间
     * @param sizeUnit 空间大小返回值的单位, 可选B|KB|MB|GB|TB
     * @return
     */
    public static String FormatFileSize(double fileSize, SizeUnit sizeUnit){
    	 DecimalFormat df = new DecimalFormat("#.00");
         if(sizeUnit.equals(SizeUnit.B))
        	 return df.format(fileSize);
         if(sizeUnit.equals(SizeUnit.KB))
        	 return df.format(fileSize / ONE_KB);
         if(sizeUnit.equals(SizeUnit.MB))
        	 return df.format(fileSize/ ONE_MB);
         if(sizeUnit.equals(SizeUnit.GB))
        	 return df.format(fileSize / ONE_GB);
         return df.format(fileSize / ONE_TB);
    }
    
    /**
     * 合并两个数组
     * @param first 第一个数组
     * @param firstStart 第一个数组待复制的起始位置
     * @param firstLength 第一个数组需要复制的长度
     * @param second 第二个数组
     * @param secondStart 第二个数组待复制的起始位置
     * @param secondLength 第二个数组需要复制的长度
     * @return int[] 合并后的新数组
     */
    public static int[] merge(int[] first, int firstStart, int firstLength, int[] second, int secondStart, int secondLength){
    	int[] result = new int[firstLength + secondLength];
    	System.arraycopy(first, firstStart, result, 0, firstLength);
    	System.arraycopy(second, secondStart, result, firstLength, secondLength);
    	return result;
    }
    /**
     * 合并两个数组
     * @param first
     * @param firstStart
     * @param firstLength
     * @param second
     * @param secondStart
     * @param secondLength
     * @return
     */
    public static double[] merge(double[] first, int firstStart, int firstLength, double[] second, int secondStart, int secondLength){
    	double[] result = new double[firstLength + secondLength];
    	System.arraycopy(first, firstStart, result, 0, firstLength);
    	System.arraycopy(second, secondStart, result, firstLength, secondLength);
    	return result;
    }
    /**
     * 获取本机ip地址和网络接口名称
     * @return
     * @throws SocketException
     */
    public static Map<String,String> localAddressAndNetworkName() throws SocketException{
    	Map<String,String> result = new HashMap<String,String>();
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
    			result.put(inetAddress.getHostAddress(), networkInterface.getName()+" | "+networkInterface.getDisplayName());
    		}
    	}
    	return result;
    }
	/**
	 * 将数组的元素拼接为字符串
	 * @param array
	 * @param splitSymbol 
	 * @return
	 */
	public static String arrayToString(String[] array, String splitSymbol) {
		StringBuilder sb = new StringBuilder();
		for(String element : array)
			sb.append(element).append(splitSymbol);
		return sb.toString();
	}
	/**
	 * 将数组元素放入Set中, 自动去重
	 * @param array
	 * @return
	 */
	public static Set<String> arrayToSet(String[] array){
		Set<String> set = new HashSet<String>();
		if(array == null || array.length == 0)
			return set;
		for(String element : array)
			set.add(element);
		return set;
	}
}
