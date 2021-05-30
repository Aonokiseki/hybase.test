package com.trs.hybase.test.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.trs.hybase.test.util.StringOperator.ILineExecutor;


public final class FileOperator {
	
	private final static class Key{
		private Key(){}
		private final static String IS_APPEND_CONTENTS = "is.append.contents";
		private final static String FILE_SEPARATOR = "file.separator";
		private final static String CUT_PARENT_PATH = "cut.parent.path";
		private static final String NOT_APPEND_LINESEPARATOR = "not.append.lineserparator";
		private static final String RW_BUFFER_SIZE = "rw.buffer.size";
		private static final String EMPTY_STRING = "";
	}
	
	/*默认缓冲区大小被我调整成了 32MB */
	private static final int DEFAULT_BUFFER_SIZE = 32 /*MiB*/;
	@SuppressWarnings("unused")
	private static final int ONE_KIB = /* 1024 bytes = */ 1024 /* bytes */;
	private static final int ONE_MIB = /* 1024 * 1024 bytes = */ 1048576 /* bytes */;
	@SuppressWarnings("unused")
	private static final int ONE_GIB = /* 1024 * 1024 * 1024 bytes = */ 1073741824 /* bytes */;
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String FILE_SEPARATOR = System.getProperty(Key.FILE_SEPARATOR);
	/*
	 * 防止实例化
	 */
	private FileOperator(){}
	/**
	 * 以UTF-8的编码方式读文件
	 * 
	 * @param sourceFileAbsolutePath 源文件的绝对路径
	 * @return String 文件中的字符串
	 * @throws IOException 源文件不在指定路径下
	 */
	public static String read(String sourceFileAbsolutePath)throws IOException{
		return read(sourceFileAbsolutePath, null, null);
	}
	/**
	 * 以指定的编码方式读文件
	 * 
	 * @param sourceFileAbsolutePath 源文件的绝对路径
	 * @param encoding 以该参数保存的编码读文件
	 * @return String 文件中的字符串
	 * @throws IOException 源文件不在指定路径下
	 */
	public static String read(String sourceFileAbsolutePath, String encoding)throws IOException{
		return read(sourceFileAbsolutePath, encoding, null);
	}
	/**
	 * 以指定的编码方式读文件
	 * 
	 * @param sourceFileAbsolutePath 源文件的绝对路径
	 * @param encoding 以该参数保存的编码读文件
	 * @param options 额外参数<br>
	 * <code>not.append.lineserparator</code> 每行后方是否追加换行符, true-不追加|false-追加, 默认false<br>
	 * <code>rw.buffer.size</code> 缓冲区大小, 范围[1, 1024], 单位MB, 默认32M, 值非法时取默认值.
	 * @return String 文件中的字符串
	 * @throws IOException 源文件不在指定路径下
	 */
	public static String read(String sourceFileAbsolutePath, String encoding, Map<String,String>options) 
			throws IOException{
		FileInputStream fileInputStream = new FileInputStream(sourceFileAbsolutePath);
		if(encoding == null || Key.EMPTY_STRING.equals(encoding.trim()))
			encoding = DEFAULT_ENCODING;
		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, encoding);
		int bufferSize = Integer.valueOf(MapOperator.safetyGet(options, Key.RW_BUFFER_SIZE, "32"));
		if(bufferSize <= 0 || bufferSize > 1024 /* MB */)
			bufferSize = DEFAULT_BUFFER_SIZE;
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader, bufferSize * ONE_MIB);
		StringBuilder stringBuilder = new StringBuilder();
		boolean doNotAppendLineSeparator = 
				Boolean.valueOf(MapOperator.safetyGet(options, Key.NOT_APPEND_LINESEPARATOR, "false"));
		String currentLineText = null;
		while((currentLineText = bufferedReader.readLine()) != null){
			stringBuilder.append(currentLineText);
			if(!doNotAppendLineSeparator)
				stringBuilder.append(System.lineSeparator());
		}
		bufferedReader.close();
		return stringBuilder.toString();
	}
	
	/**
	 * 以指定规则读取文件
	 * @param sourceFileAbsolutePath 源文件的绝对路径
	 * @param encoding 以该参数保存的编码读文件
	 * @param iLineExecutor 接口, 指明如何处理单行文本
	 * @param options 选项, 目前有<br>
	 * <code>rw.buffer.size</code> 缓冲区大小, 范围[1, 1024], 单位MB, 默认32M, 值非法时取默认值.
	 * @return String 文件中的字符串
	 * @throws IOException
	 */
	public static String read(String sourceFileAbsolutePath, String encoding, 
			com.trs.hybase.test.util.StringOperator.ILineExecutor iLineExecutor, Map<String,String> options) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(sourceFileAbsolutePath);
		if(encoding == null || Key.EMPTY_STRING.equals(encoding.trim()))
			encoding = DEFAULT_ENCODING;
		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, encoding);
		int bufferSize = Integer.valueOf(MapOperator.safetyGet(options, Key.RW_BUFFER_SIZE, "32"));
		if(bufferSize <= 0 || bufferSize > 1024)
			bufferSize = DEFAULT_BUFFER_SIZE;
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader, bufferSize * ONE_MIB);
		StringBuilder stringBuilder = new StringBuilder();
		String currentLineText = null;
		while((currentLineText = bufferedReader.readLine()) != null){
			if(!iLineExecutor.accept(currentLineText))
				continue;
			currentLineText = iLineExecutor.process(currentLineText);
			if(currentLineText == null)
				continue;
			stringBuilder.append(currentLineText);
		}
		bufferedReader.close();
		return stringBuilder.toString();
	}
	
	/**
	 * 以指定编码, 按照列表方式读文件
	 * 
	 * @param sourceFileAbsolutePath 源文件绝对路径
	 * @param encoding 以该编码读取文件
	 * @return <code>List&ltString&gt</code>, 列表方式保存的文件
	 * @throws IOException 源文件不在指定路径下
	 */
	public static LinkedList<String> readAsList(String sourceFileAbsolutePath, String encoding)throws IOException{
		return readAsList(sourceFileAbsolutePath, encoding, new ILineExecutor(){
				@Override
				public boolean accept(String currentLine) {
					if(currentLine.isEmpty())
						return false;
					return true;
				}
				@Override
				public String process(String currentLine) {
					return currentLine;
				}
			}, null);
	}
	/**
	 * 以指定编码, 按照列表方式读取文件
	 * @param sourceFileAbsolutePath 源文件绝对路径
	 * @param encoding 以该编码读取文件
	 * @param iLineExecutor 接口, 指出如何过滤并处理单行
	 * @param options 参数<br>
	 * <code>rw.buffer.size</code> 缓冲区大小, 范围[1, 1024], 单位MB, 默认32M, 值非法时取默认值.
	 * @return LinkedList 文本列表, 每个元素对应文本的一行
	 * @throws IOException
	 */
	public static LinkedList<String> readAsList(String sourceFileAbsolutePath, String encoding, 
			ILineExecutor iLineExecutor, Map<String,String> options) throws IOException{
		FileInputStream fileInputStream = new FileInputStream(sourceFileAbsolutePath);
		if(encoding == null || Key.EMPTY_STRING.equals(encoding.trim()))
			encoding = DEFAULT_ENCODING;
		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, encoding);
		int bufferSize = Integer.valueOf(MapOperator.safetyGet(options, Key.RW_BUFFER_SIZE, "32"));
		if(bufferSize <= 0 || bufferSize > 1024)
			bufferSize = DEFAULT_BUFFER_SIZE;
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader, bufferSize * ONE_MIB);
		LinkedList<String> contentList = new LinkedList<String>();
		String currentLineText = null;
		while((currentLineText = bufferedReader.readLine()) != null){
			if(!iLineExecutor.accept(currentLineText))
				continue;
			currentLineText = iLineExecutor.process(currentLineText);
			if(currentLineText == null)
				continue;
			contentList.add(currentLineText);
		}
		bufferedReader.close();
		return contentList;
	}
	
	/**
	 * 以二进制方式读取文件(文件不能超过2GB)
	 * 
	 * @param sourceFileAbsolutePath 源文件的绝对路径
	 * @return <code>List&ltByte&gt</code> 每个元素是此文件的一个字节
	 * @throws IOException
	 */
	public static List<Byte> toBinaryArray(String sourceFileAbsolutePath) throws IOException{
		List<Byte> bytes = new LinkedList<Byte>();
		InputStream in = new FileInputStream(sourceFileAbsolutePath);
		int tempByte = Integer.MIN_VALUE;
		while((tempByte = in.read())!= -1)
			bytes.add((byte)tempByte);
		in.close();
		return bytes;
	}
	/**
	 * 以UTF-8编码方式写文件
	 * 
	 * @param targetSavingPath 文件的输出绝对路径
	 * @param text 输出文本
	 * @throws IOException 路径不存在
	 * 
	 */
	public static void write(String targetSavingPath, String text) throws IOException{
		write(targetSavingPath, text, null, null);
	}
	/**
	 * 以指定编码方式写文件
	 * 
	 * @param targetSavingPath 文件的输出绝对路径
	 * @param text 输出文本
	 * @param encoding 编码方式
	 * @throws IOException 路径不存在
	 * 
	 */
	public static void write(String targetSavingPath, String text, String encoding) throws IOException{
		write(targetSavingPath, text, encoding, null);
	}
	/**
	 * 以指定编码方式写文件
	 * 
	 * @param targetSavingPath 文件的输出绝对路径
	 * @param text 输出文本
	 * @param encoding 编码方式
	 * @param options 其它参数, 目前:<br>
	 * <code>is.append.contents</code> 是否追加内容到末尾,true/false,默认false,不追加;空值视为false.<br>
	 * 注意追加文本需要自行调整文本格式.<br>
	 * <code>rw.buffer.size</code> 缓冲区大小, 范围[1, 1024], 单位MB, 默认32M, 值非法时取默认值.
	 * @throws IOException 路径不存在
	 * 
	 */
	public static void write(String targetSavingPath, String text, String encoding, Map<String,String> options) 
			throws IOException{
		boolean isAppend = Boolean.valueOf(MapOperator.safetyGet(options, Key.IS_APPEND_CONTENTS, "false"));
		int bufferSize = Integer.valueOf(MapOperator.safetyGet(options, Key.RW_BUFFER_SIZE, "32"));
		if(bufferSize <= 0 || bufferSize > 1024)
			bufferSize = DEFAULT_BUFFER_SIZE;
		FileOutputStream fileOutputStream = new FileOutputStream(targetSavingPath, isAppend);
		if(encoding == null || Key.EMPTY_STRING.equals(encoding.trim()))
			encoding = DEFAULT_ENCODING;
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, encoding);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter, bufferSize * ONE_MIB);
		bufferedWriter.write(text);
		bufferedWriter.flush();
		bufferedWriter.close();
	}
	/**
	 * 读取ini配置文件的指定项
	 * 
	 * @param inputFilePath 配置文件的绝对路径
	 * @param encoding 配置文件的编码方式
	 * @return Map key是item, value是property
	 * @throws IOException 配置文件路径不正确时 
	 */
	public static Map<String,String> readConfiguration(String inputFilePath, String encoding) throws IOException{
		Map<String,String> properties = new HashMap<String,String>();
		Properties property = new Properties();
		FileInputStream fileInputStream = new FileInputStream(inputFilePath);
		if(encoding == null || Key.EMPTY_STRING.equals(encoding.trim()))
			encoding = DEFAULT_ENCODING;
		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, encoding);
		property.load(inputStreamReader);
		for(Entry<Object,Object> entry:property.entrySet())
			properties.put(entry.getKey().toString(), entry.getValue().toString());
		inputStreamReader.close();
		fileInputStream.close();
		return properties;
	}
	
	/**
	 * 内部接口, 对遍历中的单个文件的处理
	 * @author zhaoyang
	 *
	 */
	public interface IExecuter{
		/**
		 * 处理遍历中的单个文件
		 * @param file
		 * @return boolean
		 */
		boolean execute(File file);
	}
	/**
	 * 按照指定规则遍历文件
	 * @param inputFilePath 搜索起点,可以是文件也可以是目录;如果是目录则会遍历该目录下的所有子文件
	 * @param pattern 正则表达式编译后的模式, 此参数禁止为空
	 * @param isContainsDirectory 是否包含目录,false-不保存|true-保存, 默认false,即不保存
	 * @param iExecuter 内部接口, 仅含有一个方法execute(File file) 用于处理遍历中的单个文件
	 * @return <code>List&ltFile&gt</code>, 每个元素为符合要求的文件(和目录)
	 * @throws IOException
	 */
	public static List<File> traversal(String inputFilePath, final Pattern pattern, boolean isContainsDirectory, 
			IExecuter iExecuter, Map<String,String> options) throws IOException{
		File filePointer = new File(inputFilePath);
		if(!filePointer.exists())
			throw new IOException(inputFilePath + " is not exist! Please check your path.");
		List<File> files = new ArrayList<File>();
		List<File> fileQueue = new LinkedList<File>();
		fileQueue.add(filePointer);
		File currentFile = null;
		File[] childsOfFile = null;
		while(!fileQueue.isEmpty()){
			currentFile = fileQueue.remove(0);
			if(currentFile.isFile()){
				if(pattern.matcher(currentFile.getName()).find() && iExecuter.execute(currentFile))
					files.add(currentFile);
				continue;
			}
			if(isContainsDirectory)
				files.add(currentFile);
			childsOfFile = currentFile.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File filePointer, String fileName){
					File current = new File(filePointer.getAbsolutePath()+ FILE_SEPARATOR + fileName);
					if(current.isDirectory())
						return true;
					return pattern.matcher(fileName).find();
				}
			});
			if(childsOfFile == null || childsOfFile.length == 0)
				continue;
			for(File f:childsOfFile)
				fileQueue.add(f);
		}
		return files;
	}
	
	/**
	 * 按照指定规则遍历文件
	 * @param inputFilePath
	 * @param pattern
	 * @param isContainsDirectory
	 * @return
	 * @throws IOException
	 */
	public static List<File> traversal2(String inputFilePath, final Pattern pattern, boolean isContainsDirectory) 
			throws IOException{
		return traversal(inputFilePath, pattern, isContainsDirectory, ((File file) -> {return true;}), null);
	}
	/**
	 * 遍历指定拓展名的文件
	 * 
	 * @param inputFilePath 搜索起点,可以是文件也可以是目录;如果是目录则会遍历该目录下的所有子文件
	 * @param suffix 目标拓展名,为空则输出所有文件
	 * @param isContainsDirectory 是否包含目录,false-不保存|true-保存, 默认false,即不保存
	 * @return <code>List&ltFile&gt</code> 每个目标文件指针组成的表
	 * @throws IOException 当搜索起点不存在时
	 */
	public static List<File> traversal(String inputFilePath, final String suffix, boolean isContainsDirectory) 
			throws IOException{
		File filePointer = new File(inputFilePath);
		if(!filePointer.exists())
			throw new IOException(inputFilePath + " is not exist! Please check your path.");
		List<File> files = new ArrayList<File>();
		List<File> fileQueue = new LinkedList<File>();
		fileQueue.add(filePointer);
		File currentFile = null;
		File[] childsOfFile = null;
		while(!fileQueue.isEmpty()){
			currentFile = fileQueue.remove(0);
			if(currentFile.isFile()){
				if(suffix == null || suffix.trim().equals(Key.EMPTY_STRING) || currentFile.getName().endsWith(suffix))
					files.add(currentFile);
				continue;
			}
			if(isContainsDirectory)
				files.add(currentFile);
			childsOfFile = currentFile.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File filePointer, String fileName){
					if(suffix == null)
						return true;
					if(suffix.trim().equals(Key.EMPTY_STRING))
						return true;
					File current = new File(filePointer.getAbsolutePath()+FILE_SEPARATOR+fileName);
					if(current.isDirectory())
						return true;
					return (current.isFile() && current.getName().endsWith(suffix.trim()));
				}
			});
			if(childsOfFile == null || childsOfFile.length == 0)
				continue;
			for(File f:childsOfFile)
				fileQueue.add(f);
		}
		return files;
	}
	/**
	 * 压缩文件(夹)
	 * @param targetDirectory 压缩操作的目标文件(夹)
	 * @param outputDirectory 输出目录,必须填写目录,压缩包的名称由被压缩的文件决定
	 * @param option 其它参数<br>
	 * <code>cut.parent.path</code> - 绝对路径裁剪时去掉当前文件(目录)的父级目录,仅当设置为true时生效。空值为默认值false。
	 * @return String 压缩包的绝对路径
	 * @throws IOException 当targetDirectory不存在或outputDirectory下已存在压缩包时
	 */
	public static String fileToZip(String targetDirectory, String outputDirectory, Map<String,String>option) 
			throws IOException{
		//======================================================//
		File filePointer = new File(targetDirectory);
		if(!filePointer.exists()){
			throw new IOException(targetDirectory+" is not exist! please check your path.");
		}
		File zipPointer = new File(outputDirectory+FILE_SEPARATOR+filePointer.getName()+".zip");
		if(zipPointer.exists()){
			throw new IOException(zipPointer.getAbsolutePath()+" already existed!");
		}
		boolean cutParentPath = false;
		if(option != null && option.get(Key.CUT_PARENT_PATH) != null && 
				"true".equals(option.get(Key.CUT_PARENT_PATH).trim())){
			cutParentPath = true;
		}
		//======================================================//
		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedOutputStream = null;
		ZipOutputStream zipOutputStream = null;
		FileInputStream fileInputStream = null;
		BufferedInputStream bufferedInputStream = null;
		ZipEntry zipEntry = null;
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE * ONE_MIB];
		int read = 0;
		fileOutputStream = new FileOutputStream(zipPointer);
		bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
		zipOutputStream = new ZipOutputStream(bufferedOutputStream);
		//======================================================//
		/*
		 * inputDirectory这个变量记录着targetDirectory的父级目录,用于裁剪绝对路径
		 * 
		 * 这样一来可以使用相对路径做压缩。
		 * 
		 * relativePath是裁剪后的根路径, 可真正用于压缩中
		 */
		//======================================================//
		String inputDirectory = Key.EMPTY_STRING;
		if(cutParentPath){
			inputDirectory = filePointer.getAbsolutePath() + FILE_SEPARATOR;
		}else{
			inputDirectory = filePointer.getParent() + FILE_SEPARATOR;
		}
		String relativePath = null;
		//======================================================//
		/*
		 * 广度优先遍历文件(夹)
		 * 
		 * 将targetDirectory放入队列中,每次取出第一个文件
		 * 
		 * 如果是文件, 使用裁剪后的路径直接做压缩操作
		 * 
		 * 如果是目录, 先在压缩包内创建一个目录,然后将该目录的所有子文件(夹)放入队列中
		 * 
		 * 重复以上操作直至队列为空,即压缩完成
		 */
		//======================================================//
		List<File> fileQueue = new LinkedList<File>();
		fileQueue.add(filePointer);
		File fPointer = null;
		while(!fileQueue.isEmpty()){
			fPointer = fileQueue.remove(0);
			if(fPointer.isDirectory()){
				fPointer.mkdirs();
				for(File f:fPointer.listFiles()){
					fileQueue.add(f);
				}
			}else{
				relativePath = fPointer.getAbsolutePath().replace(inputDirectory, Key.EMPTY_STRING);
				zipEntry = new ZipEntry(relativePath);
				zipOutputStream.putNextEntry(zipEntry);
				fileInputStream = new FileInputStream(fPointer);
				bufferedInputStream = new BufferedInputStream(fileInputStream, DEFAULT_BUFFER_SIZE * ONE_MIB);
				while((read=bufferedInputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE * ONE_MIB)) != -1){
					zipOutputStream.write(buffer,0,read);
		        }
			}
		}
		//======================================================//
		if(null != bufferedInputStream){
			bufferedInputStream.close();
		}
		if(null != zipOutputStream){
			zipOutputStream.close();
		}
		return zipPointer.getAbsolutePath();
	}
	/**
	 * 解压缩文件
	 * @param zipAbsolutePath 压缩包绝对路径
	 * @param targetDirectory 解压的目标目录
	 * @param option 其它参数，目前没有使用
	 * @throws IOException 压缩包的绝对路径不存在或错误，或者目标目录是个已存在的文件时
	 */
	public static void zipToFile(String zipAbsolutePath, String targetDirectory, Map<String,String> option) 
			throws IOException{
		//======================================================//
		File sourcePointer = new File(zipAbsolutePath);
		if(!sourcePointer.exists()){
			throw new IOException(zipAbsolutePath + " not exists, please check your parameter.");
		}if(!sourcePointer.isFile()){
			throw new IOException(zipAbsolutePath + " is not a file.");
		}if(!sourcePointer.getName().endsWith(".zip")){
			throw new IOException(zipAbsolutePath + " is not a zip.");
		}
		File targetPointer = new File(targetDirectory);
		if(!targetPointer.exists()){
			targetPointer.mkdirs();
		}else{
			if(!targetPointer.isDirectory()){
				throw new IOException(targetDirectory + " is not a directory");
			}else{}
		}
		//======================================================//
		ZipEntry zipEntry = null;
		String entryFilePath = Key.EMPTY_STRING;
		File entryFilePointer = null;
		File entryFileParent = null;
		BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE * ONE_MIB];
        int count = 0;
        //======================================================//
        /*
         * 遍历压缩包下的所有实体
         * 
         * 情况1:
         * 
         * 获得的实体是个目录 -> 拼接到目标目录后方,成为新目录 -> 1.不存在 -> 创建目录
         * 													   |
         * 													   -> 2.已存在 -> 什么都不做
         * 
         * 情况2:
         * 
         * 获得的实体是个文件 -> 获得这个文件的父级目录,然后拼接到目标目录后方,成为新目录 -> 1.不存在 -> 创建目录
         * 													                              |	
         * 													                              -> 2.已存在 -> 什么都不做
         * 
         * 然后, 解压这个实体
         */
        //======================================================//
		ZipFile zipFile = new ZipFile(sourcePointer);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while(entries.hasMoreElements()){
			zipEntry = entries.nextElement();
			entryFilePath = targetDirectory + FILE_SEPARATOR + zipEntry.getName();
			entryFilePointer = new File(entryFilePath);
			if(zipEntry.isDirectory()){
				if(!entryFilePointer.exists()){
					entryFilePointer.mkdirs();
					continue;
				}
			}else{
				entryFileParent = entryFilePointer.getParentFile();
				if(!entryFileParent.exists()){
					entryFileParent.mkdirs();
				}
				bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(entryFilePointer));
				bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
				while((count = bufferedInputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE * ONE_MIB))!= -1){
					bufferedOutputStream.write(buffer, 0, count);
				}
				bufferedOutputStream.flush();
				bufferedOutputStream.close();
				bufferedInputStream.close();
			}
		}
		zipFile.close();
	}
	/**
	 * 复制文件, 需要自行校验from和to是否合法
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static void fileCopy(File from, File to) throws IOException{
		FileInputStream fileInputStream = new FileInputStream(from);
		FileOutputStream fileOutputStream = new FileOutputStream(to);
		FileChannel inputChannel = fileInputStream.getChannel();
		FileChannel outputChannel = fileOutputStream.getChannel();
		outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		inputChannel.close();
		fileInputStream.close();
		outputChannel.close();
		fileOutputStream.close();
	}
	/**
	 * 复制文件(夹)
	 * @param source 源文件(夹), 不存在时抛出异常
	 * @param destination 目标文件夹,如果是已存在的单个文件则抛出异常
	 * @param options 可选参数<nr>
	 * <b>is.delete.from</>
	 * @throws IOException
	 */
	public static void filesCopy(String source, String destination, Map<String,String> options) throws IOException{
		File from = new File(source);
		if(!from.exists())
			throw new NullPointerException("File ["+source+"] is not exist.");
		File to = new File(destination);
		if(to.exists() && to.isFile())
			throw new IllegalArgumentException("File ["+destination+"] is a file.");
		String name = null;
		File current = null;
		File newFilePointer = null;
		File[] childsOfCurrent = null;
		List<File> files = new LinkedList<File>();
		files.add(from);
		while(!files.isEmpty()){
			current = files.remove(0);
			name = current.getName();
			newFilePointer = new File(to.getAbsolutePath() + FILE_SEPARATOR + name);
			if(current.isFile()){
				if(current.equals(newFilePointer))
					newFilePointer = new File(handleDuplicateName(current));
				fileCopy(current, newFilePointer);
				continue;
			}
			newFilePointer.mkdirs();
			childsOfCurrent = current.listFiles();
			for(File fPointer : childsOfCurrent)
				files.add(fPointer);
		}
	}
	/**
	 * 复制单个文件时，如果该文件所在的父级目录等同于其复制的目的地时, 在其后方添加一个copy字符<br>
	 * 例:  C:/A.txt  ->  C:/A_copy.txt
	 * @param current
	 * @return
	 */
	private static String handleDuplicateName(File current){
		StringBuilder sb = new StringBuilder();
		String currentName = current.getName();
		int lastIndexOfPoint = currentName.lastIndexOf(".");
		String extension = currentName.substring(lastIndexOfPoint);
		String firstName = currentName.substring(0, lastIndexOfPoint);
		sb.append(current.getParent())
		  .append(FILE_SEPARATOR)
		  .append(firstName)
		  .append("_")
		  .append("copy")
		  .append(extension);
		return sb.toString();
	}
}
