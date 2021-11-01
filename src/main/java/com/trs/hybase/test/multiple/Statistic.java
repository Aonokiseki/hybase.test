package com.trs.hybase.test.multiple;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.trs.hybase.client.TRSException;
import com.trs.hybase.test.util.Other;

@Component
@Scope("singleton")
public class Statistic {
	private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private AtomicLong success;
	private AtomicLong failure;
	private ConcurrentHashMap<String,Long> exceptionCategory;
	private ConcurrentLinkedQueue<String> exceptionInfos;
	
	public Statistic() {
		this.success = new AtomicLong(0);
		this.failure = new AtomicLong(0);
		this.exceptionCategory = new ConcurrentHashMap<String, Long>();
		this.exceptionInfos = new ConcurrentLinkedQueue<String>();
	}
	
	public long addSuccessCount() {
		return this.success.addAndGet(1);
	}
	public long getSuccessCount() {
		return this.success.longValue();
	}
	public long addFailureCount() {
		return this.failure.addAndGet(1);
	}
	public long getFailureCount() {
		return this.failure.longValue();
	}
	public void updateExceptionCategory(TRSException e) {
		String exceptionInfo = String.format("errorCode=%d, errorString=%s", e.getErrorCode(), e.getErrorString());
		if(!exceptionCategory.containsKey(exceptionInfo)) {
			exceptionCategory.put(exceptionInfo, 1L);
			return;
		}
		long number = exceptionCategory.get(exceptionInfo);
		exceptionCategory.put(exceptionInfo, number + 1L);
	}
	public ConcurrentHashMap<String, Long> getExceptionCategory(){
		return this.exceptionCategory;
	}
	public void updateExceptionInfos(TRSException e) {
		String exceptionInfo = String.format("%s, errorCode=%d, errorString=%s%s%s",
				LocalDateTime.now().format(DATE_TIME_FORMATTER),
				e.getErrorCode(), e.getErrorString(),System.lineSeparator(), Other.stackTraceToString(e));
		this.exceptionInfos.add(exceptionInfo);
	}
	public ConcurrentLinkedQueue<String> getExceptionInfos(){
		return this.exceptionInfos;
	}
}
