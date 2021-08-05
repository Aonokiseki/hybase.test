package com.trs.hybase.test.multiple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;

@Scope("singleton")
public class QueryReadingTask implements Runnable{
	private final static Logger logger = LogManager.getLogger(QueryReadingTask.class);
	private LinkedList<File> files = new LinkedList<File>();
	private EntityQueue<String> queries;
	private String encoding;
	
	public QueryReadingTask(List<File> fileList, String encoding, EntityQueue<String> queries) {
		this.encoding = encoding;
		this.queries = queries;
		for(int i=0,size=fileList.size(); i<size; i++)
			this.files.add(fileList.get(i));
	}

	@Override
	public void run() {
		File current = null;
		BufferedReader reader = null;
		String currentLine = null;
		while(!files.isEmpty()) {
			current = files.remove();
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(current.getAbsolutePath()), encoding));
				while((currentLine = reader.readLine()) != null)
					queries.appendTail(currentLine);
			}catch(IOException | InterruptedException e) {
				logger.error(e.toString());
			}finally {
				files.add(current);
			}
		}
	}

}
