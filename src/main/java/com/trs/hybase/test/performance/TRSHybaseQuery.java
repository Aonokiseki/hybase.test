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
public class TRSHybaseQuery extends AbstractJavaSamplerClient{
	
	/* 主方法, 用于调试 */
	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		arguments.addArgument("host", "http://192.168.105.67:5555");
		arguments.addArgument("user", "admin");
		arguments.addArgument("password", "trsadmin");
		arguments.addArgument("dbName", "system.demo");
		arguments.addArgument("searchWord", "*:*");
		arguments.addArgument("searchTimeOut", "60");
		arguments.addArgument("searchSyntaxName", "hybase");
		arguments.addArgument("sortMethod", "RELEVANCE");
		JavaSamplerContext context = new JavaSamplerContext(arguments);
		TRSHybaseQuery trshybaseQuery = new TRSHybaseQuery();
		SampleResult result = trshybaseQuery.runTest(context);
		System.out.println(result.getResponseDataAsString());
	}
	
	private final static String SUCCESS_MARK = "TRSHYBASE_QUERY_SUCCESS";
	private final static String FAILURE_MARK = "TRSHYBASE_QUERY_FAILURE";
	private final static String DEFAULT_ENCODING = "utf-8";
	
	/* 主机名 */
	private String host;
	/* 用户名 */
	private String user;
	/* 密码 */
	private String password;
	/* 数据库名 */
	private String dbName;
	/* 检索词 */
	private String searchWord;
	/* 排序方法 */
	private String sortMethod;
	/* search.syntax.name */
	private String searchSyntaxName;
	/* 客户端超时时间 */
	private String searchTimeOut;
	private SearchParams sp;
	private TRSResultSet resultSet;
	
	/**
	 * Jmeter 显示的参数列表和默认的参数值
	 */
	@Override	
	public Arguments getDefaultParameters() {
		Arguments params = new Arguments();
		params.addArgument("host", "http://127.0.0.1:5555");
		params.addArgument("user", "admin");
		params.addArgument("password", "trsadmin");
		params.addArgument("dbName", "system.demo");
		params.addArgument("searchword", "*:*");
		params.addArgument("sortMethod", "RELEVANCE");
		params.addArgument("search.syntax.name", "hybase");
		params.addArgument("search.time.out", "120");
		return params;
	}
	
	@Override
	public void setupTest(JavaSamplerContext context) {}
	
	@Override
	public void teardownTest(JavaSamplerContext context) {}
	/**
	 * 初始化参数列表, 这一步在runTest()完成
	 * @param context
	 */
	private void initializeParameters(JavaSamplerContext context) {
		host = context.getParameter("host");
		user = context.getParameter("user");
		password = context.getParameter("password");
		dbName = context.getParameter("dbName");
		searchWord = context.getParameter("searchword");
		searchSyntaxName = context.getParameter("search.syntax.name");
		searchTimeOut = context.getParameter("search.time.out");
		sortMethod = context.getParameter("sortMethod");
		sp = new SearchParams();
		if(searchSyntaxName != null && !searchSyntaxName.isEmpty())
			sp.setProperty("search.syntax.name", searchSyntaxName);
		if(searchTimeOut != null && !searchTimeOut.isEmpty())
			sp.setTimeOut(Integer.valueOf(searchTimeOut));
		if(sortMethod != null && !sortMethod.isEmpty())
			sp.setSortMethod(sortMethod);
	}
	/**
	 * 运行测试
	 */
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		/* 初始化参数 */
		initializeParameters(context);
		SampleResult result = new SampleResult();
		/* 初始化 TRSConnection  构造器 */
		TRSConnection conn = new TRSConnection(host, user, password, new ConnectParams());
		/* 设置采集器任务信息 */
		String samplerData = String.format("[%s].executeSelect(%s, %s, 0, 10, %s)", 
				conn.getURL(), dbName, searchWord, sp);
		result.setSampleLabel(samplerData);
		result.setSamplerData(samplerData);
		try {
			/*开始测试*/
			result.sampleStart();
			resultSet = conn.executeSelect(dbName, searchWord, 0, 10, sp);
			/* 终止计时 */
			result.sampleEnd();
			/* 设置单条case状态, 拼装responseData */
			result.setSuccessful(true);
			String responseData = String.format("%s%s%s", SUCCESS_MARK, System.lineSeparator(), buildResponseData());
			result.setResponseData(responseData, DEFAULT_ENCODING);
		} catch (TRSException e) {
			/* 异常处理 */
			result.sampleEnd();
			String errorLog = String.format("%s%serrorCode=%d, errorString=%s%s%s",
					FAILURE_MARK, System.lineSeparator(),
					e.getErrorCode(), e.getErrorString(), System.lineSeparator(), stackTraceToString(e));
			result.setSuccessful(false);
			result.setResponseData(errorLog, DEFAULT_ENCODING);
		} finally {
			conn.close();
		}
		return result;
	}
	/**
	 * 构造 ResponseData 
	 * @return
	 * @throws TRSException
	 */
	private String buildResponseData() throws TRSException {
		if(resultSet == null)
			return "resultSet == null";
		/* 先打印结果集大小, 命中记录数, 数据库命中记录信息 */
		StringBuilder responseDataBuilder = new StringBuilder();
		responseDataBuilder.append("resultSet.size=").append(resultSet.size())
		  .append(", resultSet.getNumFound=").append(resultSet.getNumFound()).append(System.lineSeparator())
		  .append("resultSet.getDBHitsNum()=").append(resultSet.getDBHitsNum()).append(System.lineSeparator());
		TRSRecord record = null;
		String[] columnsName = null;
		/* 遍历结果集 */
		for(int i=0, size=resultSet.size(); i<size; i++) {
			resultSet.moveNext();
			record = resultSet.get();
			/* 获取字段列表 */
			columnsName = record.getColumnNames();
			responseDataBuilder.append("{").append(System.lineSeparator());
			/* 每个字段分别获取一次值 */
			for(int j=0; j<columnsName.length; j++) {
				/* 拼装字段名和字段值 */
				responseDataBuilder.append("    ").append(columnsName[j]).append("=").append(record.getString(columnsName[j]));
				if(j < columnsName.length - 1)
					responseDataBuilder.append(", ");
				responseDataBuilder.append(System.lineSeparator());
			}
			responseDataBuilder.append("}");
			if(i < size - 1)
				responseDataBuilder.append(",");
			responseDataBuilder.append(System.lineSeparator());
		}
		return responseDataBuilder.toString();
	}
	/**
     * 返回堆栈字符串
     * 
     * @param throwable
     * @return String 堆栈信息
     */
    private static String stackTraceToString(Throwable throwable){
    	StringWriter sw = new StringWriter();
 	    throwable.printStackTrace(new PrintWriter(sw, true));
 	    return sw.getBuffer().toString();
    }
}
