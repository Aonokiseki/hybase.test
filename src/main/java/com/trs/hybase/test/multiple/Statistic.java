package com.trs.hybase.test.multiple;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.trs.hybase.client.TRSException;
import com.trs.hybase.test.util.Other;

@Component
@Scope("singleton")
public class Statistic {
	private AtomicLong success;
	private AtomicLong failure;
	private ConcurrentHashMap<String,Long> exceptionCategory;
	
	public Statistic() {
		this.success = new AtomicLong(0);
		this.failure = new AtomicLong(0);
		this.exceptionCategory = new ConcurrentHashMap<String, Long>();
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
		String exceptionInfo = String.format("errorCode=%d, errorString=%s%s%s", e.getErrorCode(), e.getErrorString(),
				System.lineSeparator(), Other.stackTraceToString(e));
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
}
