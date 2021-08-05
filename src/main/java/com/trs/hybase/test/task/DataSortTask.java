package com.trs.hybase.test.task;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import com.google.gson.Gson;

public class DataSortTask {
	
	private final static int DEFAULT_FILE_COUNT = 1;
	private static int fileCount = DEFAULT_FILE_COUNT;
	private final static Gson GSON = new Gson();
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		JobConf jobConf = new JobConf(new Configuration());
		Job job = Job.getInstance(jobConf);
		job.setJarByClass(DataSortTask.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		fileCount = Integer.valueOf(args[2]);
		job.setMapperClass(DataMapper.class);
		job.setPartitionerClass(DataPartitioner.class);
		job.setSortComparatorClass(DataComparator.class);
		job.setGroupingComparatorClass(DataComparator.class);
		job.setReducerClass(DataReducer.class);
		job.setOutputKeyClass(Data.class);
		job.setOutputValueClass(NullWritable.class);
		job.waitForCompletion(true);
	}
	
	public static class DataComparator extends WritableComparator{
		
		protected DataComparator() {
			super(Data.class, true);
		}
		
		@Override
		@SuppressWarnings("rawtypes")
		public int compare(WritableComparable w1, WritableComparable w2) {
			Data data1 = (Data) w1;
			Data data2 = (Data) w2;
			return data1.compareTo(data2);
		}
	}
	
	private static class DataMapper extends Mapper<LongWritable, Text, Data, NullWritable>{
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			Data data = GSON.fromJson(value.toString(), Data.class);
			context.write(data, NullWritable.get());
		}
	}
	
	private static class DataPartitioner extends Partitioner<Data, NullWritable>{
		@Override
		public int getPartition(Data key, NullWritable value, int numPartitions) {
			return Math.floorMod(key.hashCode(), numPartitions);
		}
	}
	
	private static class DataReducer extends Reducer<Data, NullWritable, NullWritable, String>{
		private MultipleOutputs<NullWritable, String> multipleOutputs;
		
		@Override
		protected void setup(Context context) {
			multipleOutputs = new MultipleOutputs<NullWritable, String>(context);
		}
		
		@Override
		public void reduce(Data key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
			String json = GSON.toJson(key);
			multipleOutputs.write(NullWritable.get(), json, String.valueOf(Math.floorMod(key.hashCode(), fileCount)));
		}
		
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			multipleOutputs.close();
		}
	}
	
	public static class Data implements WritableComparable<Data>{
		private Long id;
		private Integer number;
		private String date;
		private String info;
		
		public Data() {
			id = Long.MAX_VALUE;
			number = Integer.MAX_VALUE;
			date = "9999-12-31 23:59:59";
			info = "";
		}
		
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public Integer getNumber() {
			return number;
		}
		public void setNumber(Integer number) {
			this.number = number;
		}
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
		}
		public String getInfo() {
			return info;
		}
		public void setInfo(String info) {
			this.info = info;
		}
		@Override
		public String toString() {
			return "Data [id=" + id + ", number=" + number + ", date=" + date + ", info=" + info + "]";
		}
		
		@Override
		public void write(DataOutput out){
			try {
				out.writeLong(id);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out.writeInt(number);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out.writeUTF(date);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out.writeUTF(info);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void readFields(DataInput in){
			try {
				id = in.readLong();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				number = in.readInt();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				date = in.readUTF();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				info = in.readUTF();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public int compareTo(Data o) {
			return this.id.compareTo(o.id);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
		
		@Override
		public boolean equals(Object o) {
			if(o == null)
				return false;
			if(!(o instanceof Data))
				return false;
			Data data = (Data) o;
			return this.id.equals(data.id);
		}
	}
}