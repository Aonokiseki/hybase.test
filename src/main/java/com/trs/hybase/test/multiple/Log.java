package com.trs.hybase.test.multiple;

import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Scope("singleton")
@Component
public class Log {
	
	private Statistic statistic;
	private final static Logger logger = LogManager.getLogger(Log.class);
	private LocalDateTime startTime;
	
	public Log(Statistic statistic) {
		this.statistic = statistic;
		this.startTime = LocalDateTime.now();
	}
	/**
	 * 定时任务方法, 每隔${log.fixed.delay}毫秒打印一次日志
	 */
	@Scheduled(fixedDelayString = "${log.fixed.delay}")
	public void write() {
		LocalDateTime now = LocalDateTime.now();
		long seconds = Duration.between(startTime, now).getSeconds();
		String baseInfo = String.format("duration:%d(s), success:%d, failure:%d", seconds, statistic.getSuccessCount(), statistic.getFailureCount());
		String exceptionInfo = statistic.getExceptionCategory().toString();
		logger.info(baseInfo);
		logger.debug(exceptionInfo);
	}
}
