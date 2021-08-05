package com.trs.hybase.test.performance;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.trs.hybase.client.TRSConnection;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.client.TRSRecord;
import com.trs.hybase.client.TRSResultSet;
import com.trs.hybase.client.params.ConnectParams;
import com.trs.hybase.client.params.SearchParams;
/**
 * Jmeter用的性能测试组件类
 * @author 
 *
 */
public class QueryClient extends AbstractJavaSamplerClient{
	
	private String host;
	private String dbName;
	private String searchWord;
	private String searchSyntaxName;
	private String searchTimeOut;
	private SearchParams sp;
	private TRSResultSet resultSet;
		
	public Arguments getDefaultParameters() {
		Arguments params = new Arguments();
		params.addArgument("search.time.out", "120");
		params.addArgument("search.syntax.name", "hybase");
		params.addArgument("host", "http://127.0.0.1:5555");
		params.addArgument("dbname", "system.demo");
		params.addArgument("searchword", "*:*");
		return params;
	}
	
	@Override
	public void setupTest(JavaSamplerContext context) {}
	
	@Override
	public void teardownTest(JavaSamplerContext context) {}
	
	private void initializeParameters(JavaSamplerContext context) {
		host = context.getParameter("host");
		dbName = context.getParameter("dbname");
		searchWord = context.getParameter("searchword");
		searchSyntaxName = context.getParameter("search.syntax.name");
		searchTimeOut = context.getParameter("search.time.out");
		sp = new SearchParams();
		if(searchSyntaxName != null && !searchSyntaxName.isEmpty())
			sp.setProperty("search.syntax.name", searchSyntaxName);
		if(searchTimeOut != null && !searchTimeOut.isEmpty())
			sp.setTimeOut(Integer.valueOf(searchTimeOut));
		
	}
	
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		initializeParameters(context);
		SampleResult result = new SampleResult();
		TRSConnection conn = new TRSConnection(host, "admin", "trsadmin", new ConnectParams());
		result.setSamplerData(String.format("[%s].executeSelect(%s,%s,0,1,%s)", conn.getURL(), dbName, searchWord, sp));
		result.sampleStart();
		try {
			resultSet = conn.executeSelect(dbName, searchWord, 0, 1, sp);
			result.setSuccessful(true);
			String responseData = buildResponseData(resultSet);
			result.setResponseData(responseData, "utf-8");
		} catch (TRSException e) {
			String errorLog = String.format("errorCode=%d, errorString=%s%s%s", 
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), stackTraceToString(e));
			result.setSuccessful(false);
			result.setResponseData(errorLog, "utf-8");
		} finally {
			conn.close();
			result.sampleEnd();
		}
		return result;
	}
	
	private static String buildResponseData(TRSResultSet resultSet) throws TRSException {
		if(resultSet == null)
			return "resultSet == null";
		StringBuilder sb = new StringBuilder();
		sb.append("resultSet.size=").append(resultSet.size()).append(", resultSet.getNumFound=").append(resultSet.getNumFound()).append(System.lineSeparator());
		TRSRecord record = null;
		String[] columnsName = null;
		for(int i=0, size=resultSet.size(); i<size; i++) {
			resultSet.moveNext();
			record = resultSet.get();
			columnsName = record.getColumnNames();
			sb.append("[");
			for(int j=0; j<columnsName.length; j++) {
				sb.append(columnsName[j]).append("=").append(record.getString(columnsName[j]));
				if(j < columnsName.length - 1)
					sb.append(", ");
			}
			sb.append("]").append(System.lineSeparator());
		}
		return sb.toString();
	}
	/**
     * 返回堆栈字符串
     * 
     * @param throwable
     * @return String 堆栈信息
     */
    public static String stackTraceToString(Throwable throwable){
    	StringWriter sw = new StringWriter();
 	    throwable.printStackTrace(new PrintWriter(sw, true));
 	    return sw.getBuffer().toString();
    }
	
}
