package com.trs.hybase.test.multiple;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.trs.hybase.test.bean.Constants;

public class Entrance {
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configurer.class);
		/* 指定log4j2的配置文件 */
		Constants.reconfigureLog4j2("./config/MultipleThreadSearchLog4j2.xml");
		/* 获取多线程检索的配置 */
		Setting setting = context.getBean(Setting.class);
		@SuppressWarnings("unchecked")
		/* 初始化检索表达式暂存队列 */
		EntityQueue<String> queriesQueue = context.getBean(EntityQueue.class);
		/* 初始化多线程检索结果统计类 */
		Statistic statistic = context.getBean(Statistic.class);
		/* 初始化一个线程池, 开启一个线程, 该线程循环读取指定目录下的文件, 将文件中的每一行取出当做表达式放入暂存队列中 */
		ThreadPoolExecutor queryReadingPool = (ThreadPoolExecutor) context.getBean("queriesReadingPool");
		queryReadingPool.submit(
				new QueryReadingTask(setting.getQueryFiles(), setting.getQueryFileEncoding(), queriesQueue));
		/* 初始化检索线程池 */
		ThreadPoolExecutor selectionPool = (ThreadPoolExecutor) context.getBean("selectionPool");
		/* 提交检索请求 */
		while(true) {
			if(selectionPool.getActiveCount() > setting.getThreadPoolCoreSize() || selectionPool.getQueue().size() > 0)
				continue;
			selectionPool.submit(new ExecuteSelectTask(setting.getHosts(), 
					setting.getDatabases(), queriesQueue, setting.getSearchParams(), statistic));
		}
	}
}
