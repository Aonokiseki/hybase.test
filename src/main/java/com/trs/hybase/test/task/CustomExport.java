package com.trs.hybase.test.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSExport;
import com.trs.hybase.client.TRSExport.IRecordListener;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.params.ConnectParams;

public class CustomExport {
	
	public static void main(String[] args) {
		String host = args[0]; String databaseName = args[1];
		String outputDirectoryPath = args[2]; int recordCountPerFile = Integer.valueOf(args[3]);
		int start = Integer.valueOf(args[4]); int number = Integer.valueOf(args[5]);
		
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		ExportToJson etj = new ExportToJson(outputDirectoryPath, recordCountPerFile);
		TRSExport export = new TRSExport(conn, etj);
		try {
			export.export(databaseName, start, number);
		} catch (TRSException e) {
			e.printStackTrace();
		}
	}
	
	private static class ExportToJson implements IRecordListener{
		
		private final static Gson GSON = new Gson();
		private String outputDirectory;
		private int recordCountPerFile;
		private int currentFileId;
		private int recordCountCurrentFile;
		private BufferedWriter writer;
		private Map<String,String> map;
		private String[] columnNames;
		
		public ExportToJson(String outputDirectory, int recordCountPerFile) {
			this.outputDirectory = outputDirectory;
			this.recordCountPerFile = recordCountPerFile;
		}
		
		@Override
		public String getDataDir() { return null;}

		@Override
		public void onBegin() throws Exception {
			if(recordCountPerFile < 1) 
				recordCountPerFile = 1;
			File directory = new File(outputDirectory);
			if(directory.exists() && directory.isFile())
				throw new IllegalArgumentException(String.format("[%s] is not a directory path.", outputDirectory));
			if(!directory.exists())
				directory.mkdirs();
			outputDirectory = directory.getAbsolutePath();
			map = new HashMap<String,String>();
		}

		@Override
		public void onEnd() throws Exception {
			if(writer != null) writer.close();
		}

		@Override
		public boolean onRecord(TRSRecord arg0) throws Exception {
			if(writer == null || recordCountCurrentFile >= recordCountPerFile) {
				String filePath = outputDirectory + "/" + String.format("%08d", currentFileId) + ".json";
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "utf-8"));
				currentFileId++;
				recordCountCurrentFile = 0;
			}
			map.clear();
			columnNames = arg0.getColumnNames();
			for(int i=0; i<columnNames.length; i++)
				map.put(columnNames[i], arg0.getString(columnNames[i]));
			writer.append(GSON.toJson(map)).append(System.lineSeparator());
			recordCountCurrentFile++;
			return true;
		}
	}
}
