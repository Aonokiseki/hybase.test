package com.trs.hybase.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
/**
 * 简单的远程Shell类<br><br>
 * 
 * 部分接口可能需要读取服务器上的图片或文件, 或结果在服务器上生成, 不方便加入自动化测试.<br>
 * 有了这个类就可以调用简单的linux命令, 或上传/下载文件<br>
 * 
 * 只有最基本的发送命令, 上传/下载单个文件, 不要试图用这个类取代shell工具.
 */
public class FakeShell {
	
	private final static JSch JSCH = new JSch();
	private final static String DEFAULT_ENCODING = "utf-8";
	private final static String FILE_SEPARATOR = "/";
	private String host;
	private int port;
	private String user;
	private String password;
	private Session session;
	
	public FakeShell(String host, int port, String user, String password) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}
	/**
	 * 连接远程服务器
	 * @throws JSchException
	 */
	public void connect() throws JSchException {
		session = JSCH.getSession(user, host, port);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
	}
	/**
	 * 发送远程命令
	 * @param command 命令, 多条命令参考shell写法, 如<code>cd /home/user; touch /home/user/a.txt</code>
	 * @param encoding
	 * @return
	 * @throws JSchException
	 * @throws IOException
	 */
	public String executeCommand(String command, String encoding) throws JSchException, IOException {
		if(command == null || command.isEmpty())
			throw new NullPointerException("[command] is null or empty.");
		if(encoding == null || encoding.isEmpty())
			encoding = DEFAULT_ENCODING;
		String result = "";
		Charset charset = Charset.forName(encoding);
		Channel channel = session.openChannel("exec"); 
		ChannelExec channelExec = (ChannelExec) channel;
		channelExec.setCommand(command);
		channelExec.connect();
		result = recording(channelExec.getInputStream(), charset);
		channelExec.disconnect();
		return result;
	}
	private static String recording(InputStream in, Charset charset) throws IOException {
		if(in == null || in.available() != 0)
			return "";
		StringBuilder result = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
		String line = null;
		while((line = reader.readLine()) != null)
			result.append(line).append(System.lineSeparator());
		reader.close();
		return result.toString();
	}
	/**
	 * 上传文件至服务器
	 * @param remoteDirectory 服务器目录
	 * @param localFilePath 待上传的文件路径
	 * @throws JSchException
	 * @throws FileNotFoundException
	 * @throws SftpException
	 */
	public void upload(String remoteDirectory, String localFilePath) throws JSchException, FileNotFoundException, SftpException {
		if(remoteDirectory == null || localFilePath == null || 
				remoteDirectory.isEmpty() || localFilePath.isEmpty())
			throw new NullPointerException("[remoteFileDirecotry] or [localFilePath] is null or empty.");
		Channel channel = session.openChannel("sftp");
		ChannelSftp sftp = (ChannelSftp) channel;
		sftp.connect();
		File source = new File(localFilePath);
		sftp.put(new FileInputStream(source), remoteDirectory + FILE_SEPARATOR + source.getName());
		sftp.disconnect();
	}
	/**
	 * 从远程服务器下载文件到本地
	 * @param remoteFilePath 远程文件路径
	 * @param localDirectoryPath 本地文件目录, 必须是目录
	 * @throws JSchException
	 * @throws FileNotFoundException
	 * @throws SftpException
	 */
	public void download(String remoteFilePath, String localDirectoryPath) throws JSchException, FileNotFoundException, SftpException {
		if(remoteFilePath == null || localDirectoryPath == null || remoteFilePath.isEmpty() || localDirectoryPath.isEmpty())
			throw new NullPointerException("[remoteFilePath] or [localDirectory] is null or empty.");
		File localDirectory = new File(localDirectoryPath);
		if(!localDirectory.exists())
			throw new IllegalArgumentException(String.format("[%s] not exist.", localDirectoryPath));
		if(localDirectory.isFile())
			throw new IllegalArgumentException(String.format("[%s] is not a directory.", localDirectoryPath));
		String fileName = remoteFilePath.substring(remoteFilePath.lastIndexOf(FILE_SEPARATOR), remoteFilePath.length());
		FileOutputStream fileOutputStream = new FileOutputStream(localDirectory.getAbsolutePath() + fileName);
		Channel channel = session.openChannel("sftp");
		ChannelSftp sftp = (ChannelSftp) channel;
		sftp.connect();
		sftp.get(remoteFilePath, fileOutputStream);
		sftp.disconnect();
	}
	/**
	 * 断开连接
	 */
	public void disconnect() {
		if(session == null || !session.isConnected())
			return;
		session.disconnect();
	}
	
	public static void main(String[] args) {
		FakeShell fShell = new FakeShell("192.168.51.166", 22, "root", "Trsadmin123.");
		try {
			fShell.connect();
			fShell.upload("/data/lhy/qatest", "C:/Users/trs/Desktop/DL-VRS-Data/BTV.jpg");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (SftpException e) {
			e.printStackTrace();
		} finally {
			fShell.disconnect();
		}
	}
}
