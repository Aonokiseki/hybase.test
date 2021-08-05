package com.trs.hybase.test.multiple;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.google.gson.Gson;
import com.trs.hybase.test.util.FileOperator;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@ComponentScan(basePackageClasses = {
		com.trs.hybase.test.multiple.Statistic.class
})
@PropertySource("file:./config/config.ini")
@EnableScheduling
public class Configurer {
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	@Bean(name="setting")
	public Setting setting(@Value("${properties.path}") String propertiesPath) throws IOException{
		Gson gson = new Gson();
		Setting setting = gson.fromJson(new InputStreamReader(new FileInputStream(propertiesPath), "utf-8"), Setting.class);
		setting.setQueryFiles(FileOperator.traversal(setting.getQueryPath(), null, false));
		setting.scanSearchParams();
		return setting;
	}
	
	@Bean("selectionPool")
	@DependsOn("setting")
	public ThreadPoolExecutor selectionPool(Setting setting) {
		return new ThreadPoolExecutor(setting.getThreadPoolCoreSize(), setting.getThreadPoolCoreSize(),0L,
				TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
	}
	
	@Bean("queriesReadingPool")
	public ThreadPoolExecutor queriesReadingPool() {
		return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
	}
	
	@Bean("queriesQueue")
	@DependsOn("setting")
	public EntityQueue<String> queriesQueue(Setting setting){
		return new EntityQueue<String>(setting.getQueryQueueSize());
	}
}
