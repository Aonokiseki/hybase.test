package com.trs.hybase.test.performance;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.trs.hybase.graph.GraphException;
import com.trs.hybase.graph.GraphFeature;
import com.trs.hybase.graph.TRSGraphFeatureConnection;

public class GraphFeatureTestClient extends AbstractJavaSamplerClient{
	
	/* 主方法在Jmeter里不会被调用, 只是方便在eclipse里调试 */
	public static void main(String[] args) {
		Arguments arguments = new Arguments();
		arguments.addArgument("host", "http://192.168.50.51:8141");
		arguments.addArgument("modelName", "general");
		arguments.addArgument("filesPath", "C:/Users/trs/Desktop/0.jpg;C:/Users/trs/Desktop/4.jpg;C:/Users/trs/Desktop/8.jpg");
		JavaSamplerContext context = new JavaSamplerContext(arguments);
		GraphFeatureTestClient client = new GraphFeatureTestClient();
		SampleResult result = client.runTest(context);
		System.out.println(result.getSamplerData());
		System.out.println(result.getResponseDataAsString());
	}
	
	/** 请求主机地址 */
	private String host;
	/** 模型名称 */
	private String modelName;
	/** (多)文件的路径, 路径之间用半角英文分号隔开 */
	private String filesPath;
	private String[] paths;
	private File[] files;
	
	@Override
	public void setupTest(JavaSamplerContext context) {}
	
	@Override
	public void teardownTest(JavaSamplerContext context) {}
	
	@Override
	public Arguments getDefaultParameters() {
		Arguments params = new Arguments();
		params.addArgument("host", "http://192.168.50.51:8141");
		params.addArgument("modelName", "General");
		params.addArgument("filesPath", "");
		return params;
	}
	/**
	 * 从上下文中获取用户输入的参数
	 * @param context
	 */
	private void initializeParameters(JavaSamplerContext context) {
		host = context.getParameter("host");
		modelName = context.getParameter("modelName");
		filesPath = context.getParameter("filesPath");
		paths = filesPath.split(";");
		files = new File[paths.length];
		for(int i=0; i<paths.length; i++)
			files[i] = new File(paths[i]);
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		/* 从上下文获取用户需要的参数 */
		initializeParameters(context);
		SampleResult result = new SampleResult();
		/* 初始化连接 */
		TRSGraphFeatureConnection graphFeatureConn = new TRSGraphFeatureConnection(host); 
		List<GraphFeature> features = null;
		/* 用例说明, 注意这里交换了modelName和filesPath, 它们和真正调用方法时的顺序是反的, 这是因为filesPath太长了 */
		/* 不影响调用 */
		/* 警告: 可能会有大量IO, 或日志文件膨胀的风险 */
		result.setSamplerData(String.format("[%s].listGraphFeature(%s, %s)", host, modelName, filesPath));
		try {
			result.sampleStart();
			/* 发送请求 */
			features = graphFeatureConn.listGraphFeature(files, modelName);
			/* 没有抛出异常, 设置为成功 */
			result.setSuccessful(true);
			/* 获取该请求返回的所有特征值串, 并写入响应数据*/
			/* 警告: 可能会有大量IO, 或日志文件膨胀的风险 */
			result.setResponseData(buildFeaturesString(features), "utf-8");
		} catch (GraphException e) {
			String exceptionLog = String.format("errorCode=%d, errorString=%s%s%s", 
					e.getErrCode(), e.getErrMsg(), System.lineSeparator(), stackTraceToString(e));
			/* 抛出异常时, 设置失败, 然后将异常信息写入响应数据中 */
			result.setSuccessful(false);
			result.setResponseData(exceptionLog, "utf-8");
		} finally {
			graphFeatureConn = null;
			result.sampleEnd();
		}
		return result;
	}
	/**
	 * 把特征值列表整合到一个String类型的变量中
	 * @param features
	 * @return
	 */
	private static String buildFeaturesString(List<GraphFeature> features) {
		StringBuilder sb = new StringBuilder();
		for(int i=0, size=features.size(); i<size; i++)
			sb.append("i==").append(i).append(",  ")
			.append(features.get(i).getFeatureStr()).append(System.lineSeparator());
		return sb.toString();
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
