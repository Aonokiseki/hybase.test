package com.trs.hybase.test.pojo;

public class SubDatabase {
	private String subDatabaseName;
	private String partName;
	private String databaseName;
	private String host;
	private String port;
	
	public static SubDatabase build() {
		return new SubDatabase();
	}
	private SubDatabase() {}
	public String getSubDatabaseName() {
		return subDatabaseName;
	}
	public void setSubDatabaseName(String subDatabaseName) {
		this.subDatabaseName = subDatabaseName;
	}
	public String getPartName() {
		return partName;
	}
	public void setPartName(String partName) {
		this.partName = partName;
	}
	public String getDatabaseName() {
		return databaseName;
	}
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	@Override
	public String toString() {
		return "Subdb [subDatabaseName=" + subDatabaseName + ", partName=" + partName + ", databaseName=" + databaseName
				+ ", host=" + host + ", port=" + port + "]";
	}
}
