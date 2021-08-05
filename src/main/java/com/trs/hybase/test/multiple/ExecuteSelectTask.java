package com.trs.hybase.test.multiple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
import com.trs.hybase.test.util.Other;

public class ExecuteSelectTask implements Runnable{
	
	private final static Logger logger = LogManager.getLogger(ExecuteSelectTask.class);
	
	private String hosts;
	private String user;
	private String password;
	private String databases;
	private EntityQueue<String> queries;
	private Integer start;
	private Long num;
	private SearchParams sp;
	private Statistic statistic;
	
	public ExecuteSelectTask(String hosts, String databases, EntityQueue<String> queries, Statistic statistic) {
		this.hosts = hosts;
		this.user = "admin";
		this.password = "trsadmin";
		this.databases = databases;
		this.queries = queries;
		this.start = 0;
		this.num = 10L;
		this.sp = new SearchParams();
		this.statistic = statistic;
	}
	
	public ExecuteSelectTask(String hosts, String databases, EntityQueue<String> queries, SearchParams sp, Statistic statistic) {
		this(hosts, databases, queries, statistic);
		this.sp = sp;
	}
	
	public ExecuteSelectTask(String hosts, String user, String password, String databases, EntityQueue<String> queries, 
			Integer start,Long num, SearchParams sp, Statistic statistic) {
		this(hosts, databases, queries, sp, statistic);
		this.user = user;
		this.password = password;
		this.start = start;
		this.num = num;
	}

	@Override
	public void run() {
		TRSConnection conn = new TRSConnection(hosts, user, password, new ConnectParams());
		try {
			String query = queries.removeHead();
			TRSResultSet resultSet = conn.executeSelect(databases, query, start, num, sp);
			logger.debug(String.format("[%s].executeSelect(%s, %s, %d, %d, %s).getNumFound()==%d", 
					conn.getURL(), databases, query, start, num, sp, resultSet.getNumFound()));
			statistic.addSuccessCount();
		} catch (TRSException e) {
			String errorLog = String.format("errorCode=%d, errorString=%s%s%s", 
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), Other.stackTraceToString(e));
			logger.error(errorLog);
			statistic.addFailureCount();
			statistic.updateExceptionCategory(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}
	}
}
