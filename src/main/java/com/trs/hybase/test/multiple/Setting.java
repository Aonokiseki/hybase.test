package com.trs.hybase.test.multiple;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.trs.hybase.client.params.SearchParams;

public class Setting {
	private String hosts;
	private String user;
	private String password;
	private String databases;
	private Integer start;
	private Long num;
	private Map<String,String> searchParams;
	private String queryPath;
	private String queryFileEncoding;
	private Integer threadPoolCoreSize;
	private List<File> queryFiles;
	private Integer queryQueueSize;
	private SearchParams sp;
	
	public String getHosts() {
		return hosts;
	}
	public void setHosts(String hosts) {
		this.hosts = hosts;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDatabases() {
		return databases;
	}
	public void setDatabases(String databases) {
		this.databases = databases;
	}
	public Integer getStart() {
		return start;
	}
	public void setStart(Integer start) {
		this.start = start;
	}
	public Long getNum() {
		return num;
	}
	public void setNum(Long num) {
		this.num = num;
	}
	public Map<String, String> getSearchParamsMap() {
		return searchParams;
	}
	public void setSearchParamsMap(Map<String, String> searchParams) {
		this.searchParams = searchParams;
	}
	public void scanSearchParams() {
		sp = new SearchParams();
		if(this.searchParams == null ||this.searchParams.isEmpty())
			return;
		for(Entry<String,String> e : searchParams.entrySet())
			sp.setProperty(e.getKey(), e.getValue());
	}
	public SearchParams getSearchParams() {
		return sp;
	}
	public String getQueryPath() {
		return queryPath;
	}
	public void setQueryPath(String queryPath) {
		this.queryPath = queryPath;
	}
	public String getQueryFileEncoding() {
		return queryFileEncoding;
	}
	public void setQueryFileEncoding(String queryFileEncoding) {
		this.queryFileEncoding = queryFileEncoding;
	}
	public Integer getThreadPoolCoreSize() {
		return threadPoolCoreSize;
	}
	public void setThreadPoolCoreSize(Integer threadPoolCoreSize) {
		this.threadPoolCoreSize = threadPoolCoreSize;
	}
	public List<File> getQueryFiles() {
		return queryFiles;
	}
	public void setQueryFiles(List<File> queryFiles) {
		this.queryFiles = queryFiles;
	}
	public Integer getQueryQueueSize() {
		return queryQueueSize;
	}
	public void setQueryQueueSize(Integer queryQueueSize) {
		this.queryQueueSize = queryQueueSize;
	}
	@Override
	public String toString() {
		return "Setting [hosts=" + hosts + ", user=" + user + ", password=" + password + ", databases=" + databases
				+ ", start=" + start + ", num=" + num + ", searchParams=" + searchParams + ", queryPath=" + queryPath
				+ ", queryFileEncoding=" + queryFileEncoding + ", threadPoolCoreSize=" + threadPoolCoreSize
				+ ", queryFiles=" + queryFiles + ", queryQueueSize=" + queryQueueSize + "]";
	}
}
