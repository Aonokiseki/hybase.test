package com.trs.hybase.test.bean;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSPermissionClient;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.graph.TRSGraphFeatureConnection;
import com.trs.hybase.test.util.Tools;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@ComponentScan(basePackageClasses= {
		com.trs.hybase.test.bean.TRSConnectionLogAspect.class,
		com.trs.hybase.test.bean.GlobalSetting.class,
})
@PropertySource("file:./config/config.ini")
public class Configurer {
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	@Bean(name="first")
	public TRSConnection firstConnection(
			@Value("${first.cluster.hosts}") String hosts,
			@Value("${first.cluster.user}") String user,
			@Value("${first.cluster.password}") String password){
		hosts = Tools.hostsAppendHttpHeader(hosts);
		TRSConnection conn = new TRSConnection(hosts, user, password, new ConnectParams());
		return conn; 
	}
	
	@Bean(name="firstTrspermission")
	@DependsOn("first")
	public TRSPermissionClient firstTrspermissionClient(@Qualifier(value="first") TRSConnection conn) {
		return new TRSPermissionClient(conn);
	}
	
	@Bean(name="second")
	public TRSConnection secondConnection(
			@Value("${second.cluster.hosts}") String hosts,
			@Value("${second.cluster.user}") String user,
			@Value("${second.cluster.password}") String password) {
		hosts = Tools.hostsAppendHttpHeader(hosts);
		TRSConnection conn = new TRSConnection(hosts, user, password, new ConnectParams());
		return conn;
	}
	
	@Bean
	public TRSGraphFeatureConnection trsGraphFeatureConnection(
			@Value("${global.graph.feature.server.address}") String serverAddress) {
		return new TRSGraphFeatureConnection(serverAddress);
	}
	
	@Bean
	public TRSResultSet resultSet() {
		return null;
	}
}
