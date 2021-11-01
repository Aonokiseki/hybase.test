package com.trs.hybase.test.multiple;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Scope("singleton")
@Component
public class StatisticWriter {
	
	private Statistic statistic;
	private String categoryPath;
	private String infosPath;
	
	public StatisticWriter(
			@Autowired Statistic statistic,
			@Value("${exception.category.path}") String categoryPath,
			@Value("${exception.infos.path}") String infosPath) {
		this.statistic = statistic;
		this.categoryPath = categoryPath;
		this.infosPath = infosPath;
	}
	/**
	 * 定时任务方法, 每隔${log.fixed.delay}毫秒打印一次日志
	 */
	@Scheduled(fixedDelayString = "${log.fixed.delay}")
	public void write() {
		writeCategory();
		writeInfos();
	}
	
	private void writeCategory() {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(categoryPath)));
			Map<String,Long> map = statistic.getExceptionCategory();
			for(Entry<String, Long> e : map.entrySet())
				writer.append(e.getKey()).append(":").append(e.getValue().toString()).append(System.lineSeparator());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeInfos() {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(infosPath)));
			ConcurrentLinkedQueue<String> infos = statistic.getExceptionInfos();
			for(String info : infos)
				writer.append(info).append(System.lineSeparator());
			writer.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
