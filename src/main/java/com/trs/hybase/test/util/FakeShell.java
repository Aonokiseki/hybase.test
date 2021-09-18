package com.trs.hybase.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class FakeShell {
	private final static JSch JSCH = new JSch();
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
	 * 执行一条远程命令<br>
	 * 例如 <code>mv a.jpg b.jpg</code>
	 * @param command
	 * @return
	 * @throws JSchException
	 * @throws IOException
	 */
	public String executeCommand(String command) throws JSchException, IOException {
		if(command == null || command.isEmpty())
			throw new NullPointerException("[command] is null or empty.");
		Channel channel = session.openChannel("exec");
		ChannelExec channelExec = (ChannelExec) channel;
		channelExec.setCommand(command);
		channelExec.setInputStream(null);
		channelExec.connect();
		BufferedReader reader = new BufferedReader(new InputStreamReader(channelExec.getInputStream()));
		String line;
		StringBuilder result = new StringBuilder();
		while((line = reader.readLine()) != null)
			result.append(line).append(System.lineSeparator());
		channelExec.disconnect();
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
}
