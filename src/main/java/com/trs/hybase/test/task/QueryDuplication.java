package com.trs.hybase.test.task;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
/**
 * 检索表达式去重
 * @author zhaoyang
 *
 */
public class QueryDuplication {
	/*
	 * trshadoop/bin/hadoop jar [这个jar的路径] com.trs.hybase.test.mapreduce.QueryDuplication [HDFS上的输入文件绝对路径] [HDFS上的输出目录]
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration configuration = new Configuration();
		Job job = Job.getInstance(configuration);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setJarByClass(QueryDuplication.class);
		job.setMapperClass(QueryMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setReducerClass(QueryReducer.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.waitForCompletion(true);
	}
	private static class QueryMapper extends Mapper<LongWritable, Text, Text, NullWritable>{
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			context.write(value, NullWritable.get());
		}
	}
	private static class QueryReducer extends Reducer<Text, NullWritable, NullWritable, Text>{
		@Override
		public void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
			context.write(NullWritable.get(), key);
		}
	}
}