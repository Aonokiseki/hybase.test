package com.trs.hybase.test.annotation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.trs.hybase.client.TRSInputColumn;
import com.trs.hybase.client.TRSInputRecord;
import com.trs.hybase.test.annotation.column.datagen.ValueCount;
import com.trs.hybase.test.annotation.column.datagen.Id;
import com.trs.hybase.test.annotation.column.datagen.In;
import com.trs.hybase.test.annotation.column.datagen.RandomIp;
import com.trs.hybase.test.annotation.column.datagen.RandomUUID;
import com.trs.hybase.test.annotation.database.GenerateRecords;
import com.trs.hybase.test.util.StringOperator;

public class DataGenerationAnalyzer {
	
	public static void main(String[] args) {
		try {
			parse("com.trs.hybase.test.pojo.SimpleViewDebug");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private DataGenerationAnalyzer() {}
	
	/**
	 * 解析类并返回生成的(多条)记录
	 * @param className
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static List<TRSInputRecord> parse(String className) throws ClassNotFoundException{
		Class<?> clazz = Class.forName(className);
		/* 检查是否有生成记录注解 */
		GenerateRecords generateRecords = clazz.getDeclaredAnnotation(GenerateRecords.class);
		/* 从注解中获取生成的记录数 */
		int recordCount = generateRecords.count();
		/* 获取字段列表(类的成员变量列表) */
		Field[] fields = clazz.getDeclaredFields();
		return innerParse(fields, recordCount);
	}
	/**
	 * 内部解析
	 * @param fields 成员变量列表, 也即字段列表
	 * @param count 目标记录数
	 * @return
	 */
	private static List<TRSInputRecord> innerParse(Field[] fields, int count){
		if(fields == null || fields.length == 0)
			return null;
		TRSInputRecord inputRecord = null;
		List<TRSInputRecord> inputRecords = new ArrayList<TRSInputRecord>();
		for(int i=0; i<count; i++) {
			inputRecord = generateSingleRecord(fields, i);
			if(inputRecord == null)
				continue;
			inputRecords.add(inputRecord);
		}
		return inputRecords;
	}
	/**
	 * 生成单条记录
	 * @param fields
	 * @param recordId 记录编号
	 * @return
	 */
	private static TRSInputRecord generateSingleRecord(Field[] fields, int recordId) {
		Field field = null;
		TRSInputRecord inputRecord = new TRSInputRecord();
		TRSInputColumn inputColumn = null;
		for(int i=0; i<fields.length; i++) {
			field = fields[i];
			inputColumn  = generateSingleColumnValue(field, recordId);
			if(inputColumn == null)
				continue;
			inputRecord.addColumn(inputColumn);
		}
		return inputRecord;
	}
	/**
	 * 生成单个字段的值
	 * @param field
	 * @param recordId 记录编号
	 * @return
	 */
	private static TRSInputColumn generateSingleColumnValue(Field field, int recordId) {
		TRSInputColumn inputColumn = new TRSInputColumn();
		inputColumn.setName(field.getName());
		String fieldTypeName = field.getType().getCanonicalName();
		System.out.println(fieldTypeName);
		String value = null;
		System.out.println(String.format("whereFieldTypeIsString(%s, %d)", field.getName(), recordId));
		if("java.lang.String".equals(fieldTypeName)) {
			value = whereFieldTypeIsString(field, recordId);
		}
		inputColumn.setValue(value);
		return inputColumn;
	}
	
	private static String whereFieldTypeIsString(Field field, int recordId) {
		/* 检测到@Id注解, 说明该成员变量(字段)就是id列 */
		Id id = field.getDeclaredAnnotation(Id.class);
		if(id != null)
			return String.valueOf(id);
		StringBuilder valueBuilder = new StringBuilder();
		/* 检查值的个数, 这个注解不添加时, 和默认值的效果一致都是1 */
		ValueCount valueCount = field.getDeclaredAnnotation(ValueCount.class);
		int count = valueCount.count();
		/* 检查 @In 注解, 说明值就在给定范围内 */
		In in = field.getDeclaredAnnotation(In.class);
		if(in != null) {
			int randomIndex = -1;
			String[] valueWhichCanBeSelected = in.values();
			for(int i=0; i<count; i++) {
				randomIndex = (int)(Math.random() * valueWhichCanBeSelected.length);
				valueBuilder.append(valueWhichCanBeSelected[randomIndex]);
				if(i < count - 1)
					valueBuilder.append(";");
			}
			return valueBuilder.toString();
		}
		/* 检查 @RandomUUID注解 */
		RandomUUID randomUuid = field.getDeclaredAnnotation(RandomUUID.class);
		if(randomUuid != null) {
			for(int i=0; i<count; i++) {
				valueBuilder.append(UUID.randomUUID().toString());
				if(i < count - 1);
					valueBuilder.append(";");
			}
			return valueBuilder.toString();
		}
		/* 检查 @RandomIp 注解 */
		RandomIp randomIp = field.getDeclaredAnnotation(RandomIp.class);
		if(randomIp != null) {
			for(int i=0; i<count; i++) {
				valueBuilder.append(StringOperator.getARandomIp());
				if(i < count - 1)
					valueBuilder.append(";");
			}
			return valueBuilder.toString();
		}
		return null;
	}
	
}
