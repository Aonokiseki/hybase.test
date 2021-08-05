package com.trs.hybase.test.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GlobalSetting {
	@Autowired
	@Value("${sleep.time.millis}")
	private long sleepTimeMillis;
	@Autowired
	@Value("${hadoop.is.active}")
	private boolean hadoopIsActive;
	@Autowired
	@Value("${cluster.engine.type}")
	private String engineType;
	@Autowired
	@Value("${delete.db.finally}")
	private boolean deleteDbFinally;
	
	public long sleepTimeMillis() {
		return sleepTimeMillis;
	}
	public boolean hadoopIsActive() {
		return hadoopIsActive;
	}
	public String engineType() {
		return engineType;
	}
	public boolean deleteDbFinally() {
		return deleteDbFinally;
	}
}
