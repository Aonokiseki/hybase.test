package com.trs.hybase.test.annotation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabase.DBPOLICY;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.client.TRSException;
import com.trs.hybase.test.annotation.column.Alias;
import com.trs.hybase.test.annotation.column.CategoryValueVariety;
import com.trs.hybase.test.annotation.column.CentStore;
import com.trs.hybase.test.annotation.column.Column;
import com.trs.hybase.test.annotation.column.Comment;
import com.trs.hybase.test.annotation.column.IndexAnalyzer;
import com.trs.hybase.test.annotation.column.IndexBinary;
import com.trs.hybase.test.annotation.column.IndexCaseSensitive;
import com.trs.hybase.test.annotation.column.IndexChineseSimplified;
import com.trs.hybase.test.annotation.column.IndexCjkwidthConvert;
import com.trs.hybase.test.annotation.column.IndexGraph;
import com.trs.hybase.test.annotation.column.IndexGraphModel;
import com.trs.hybase.test.annotation.column.IndexIdeoAll;
import com.trs.hybase.test.annotation.column.IndexIdeoSingle;
import com.trs.hybase.test.annotation.column.IndexIpaddress;
import com.trs.hybase.test.annotation.column.IndexPinyin;
import com.trs.hybase.test.annotation.column.IndexPinyinFirst;
import com.trs.hybase.test.annotation.column.IndexPrefixRules;
import com.trs.hybase.test.annotation.column.IndexRedundDate;
import com.trs.hybase.test.annotation.column.IndexRedundIdeo;
import com.trs.hybase.test.annotation.column.IndexRegexRules;
import com.trs.hybase.test.annotation.column.IndexSchema;
import com.trs.hybase.test.annotation.column.IndexStemSnowball;
import com.trs.hybase.test.annotation.column.IndexStopNone;
import com.trs.hybase.test.annotation.column.IndexTextualFeature;
import com.trs.hybase.test.annotation.column.IndexTextualFeatureModel;
import com.trs.hybase.test.annotation.column.IndexVirtualColumns;
import com.trs.hybase.test.annotation.column.MultiValue;
import com.trs.hybase.test.annotation.column.NotIndex;
import com.trs.hybase.test.annotation.column.NotStore;
import com.trs.hybase.test.annotation.column.StoreFile;
import com.trs.hybase.test.annotation.column.ValueMust;
import com.trs.hybase.test.annotation.column.WildcardLeading;
import com.trs.hybase.test.annotation.database.Database;

public class DatabaseAnalyzer {
	private final static Gson GSON = new Gson();
	private DatabaseAnalyzer() {}
	/**
	 * 根据注解解析并组装TRSDatabase实例对象
	 * @param className
	 * @return
	 * @throws ClassNotFoundException
	 * @throws TRSException
	 */
	public static TRSDatabase parse(String className) throws ClassNotFoundException, TRSException {
		Class<?> clazz = Class.forName(className);
		Database database = clazz.getDeclaredAnnotation(Database.class);
		Field[] fields = clazz.getDeclaredFields();
		List<TRSDatabaseColumn> dbCols = generateTRSDatabaseColumns(fields);
		TRSDatabase db = trsdatabaseNewInstance(database);
		db.addColumns(dbCols);
		db = addDatabaseProperties(database, db);
		return db;
	}
	
	private static DBPOLICY policy(String policyStr) {
		if(policyStr == null || policyStr.trim().isEmpty())
			return DBPOLICY.FASTEST;
		policyStr = policyStr.trim().toLowerCase();
		switch(policyStr) {
			case "fastest" : return DBPOLICY.FASTEST;
			case "normal" : return DBPOLICY.NORMAL;
			case "safest" : return DBPOLICY.SAFEST;
			default : return DBPOLICY.FASTEST;
		}
	}
	/**
	 * 生成 TRSDatabase 实例对象
	 * @param database
	 * @return
	 */
	private static TRSDatabase trsdatabaseNewInstance(Database database) {
		if(database == null)
			return null;
		String dbName = database.name();
		int dbType = database.type();
		DBPOLICY policy = policy(database.policy());
		return new TRSDatabase(dbName, dbType, policy);
	}
	/**
	 * 根据注解解析并初始化多个TRSDatabaseColumn实例对象
	 * @param fields
	 * @return
	 * @throws TRSException
	 */
	private static List<TRSDatabaseColumn> generateTRSDatabaseColumns(Field[] fields) throws TRSException {
		if(fields == null || fields.length == 0)
			return new ArrayList<TRSDatabaseColumn>(0);
		List<TRSDatabaseColumn> trsDatabaseColumns = new ArrayList<TRSDatabaseColumn>(fields.length);
		Field field = null; Column column = null;
		TRSDatabaseColumn trsDatabaseColumn = null;
		for(int i=0; i<fields.length; i++) {
			field = fields[i];
			column = field.getDeclaredAnnotation(Column.class);
			if(column == null)
				continue;
			trsDatabaseColumn = new TRSDatabaseColumn(column.name(), column.type());
			trsDatabaseColumn = addProperties(field, trsDatabaseColumn);
			trsDatabaseColumns.add(trsDatabaseColumn);
		}
		return trsDatabaseColumns;
	}
	/**
	 * 解析字段注解, 并为TRSDatabaseColumn实例对象设置属性
	 * @param field
	 * @param trsDatabaseColumn
	 * @return
	 */
	private static TRSDatabaseColumn addProperties(Field field, TRSDatabaseColumn trsDatabaseColumn) {
		Alias alias = field.getDeclaredAnnotation(Alias.class);
		if(alias != null)
			trsDatabaseColumn.setAlias(alias.value());
		CentStore centStore = field.getDeclaredAnnotation(CentStore.class);
		if(centStore != null)
			trsDatabaseColumn.setCentStore(centStore.value());
		NotStore notStore = field.getDeclaredAnnotation(NotStore.class);
		if(notStore != null)
			trsDatabaseColumn.setNotStore(notStore.value());
		NotIndex notIndex = field.getDeclaredAnnotation(NotIndex.class);
		if(notIndex != null)
			trsDatabaseColumn.setNotIndex(notIndex.value());
		Comment comment = field.getDeclaredAnnotation(Comment.class);
		if(comment != null)
			trsDatabaseColumn.setComment(comment.value());
		MultiValue multiValue = field.getDeclaredAnnotation(MultiValue.class);
		if(multiValue != null)
			trsDatabaseColumn.setMultivalue(multiValue.value());
		StoreFile storeFile = field.getDeclaredAnnotation(StoreFile.class);
		if(storeFile != null)
			trsDatabaseColumn.setStoreFile(storeFile.value());
		ValueMust valueMust = field.getDeclaredAnnotation(ValueMust.class);
		if(valueMust != null)
			trsDatabaseColumn.setValueMust(valueMust.value());
		WildcardLeading wildcardLeading = field.getDeclaredAnnotation(WildcardLeading.class);
		if(wildcardLeading != null)
			trsDatabaseColumn.setProperty(WildcardLeading.NAME, String.valueOf(wildcardLeading.value()));
		CategoryValueVariety categoryValueVariety = field.getDeclaredAnnotation(CategoryValueVariety.class);
		if(categoryValueVariety != null)
			trsDatabaseColumn.setProperty(CategoryValueVariety.NAME, String.valueOf(categoryValueVariety.value()));
		IndexAnalyzer indexAnalyzer = field.getDeclaredAnnotation(IndexAnalyzer.class);
		if(indexAnalyzer != null)
			trsDatabaseColumn.setProperty(IndexAnalyzer.NAME, indexAnalyzer.value());
	    IndexBinary indexBinary = field.getDeclaredAnnotation(IndexBinary.class);
	    if(indexBinary != null)
	    	trsDatabaseColumn.setProperty(IndexBinary.NAME, String.valueOf(indexBinary.value()));
	    IndexCaseSensitive indexCaseSensitive = field.getDeclaredAnnotation(IndexCaseSensitive.class);
	    if(indexCaseSensitive != null)
	    	trsDatabaseColumn.setProperty(IndexCaseSensitive.NAME, String.valueOf(indexCaseSensitive.value()));
	    IndexChineseSimplified indexChineseSimplified = field.getDeclaredAnnotation(IndexChineseSimplified.class);
	    if(indexChineseSimplified != null)
	    	trsDatabaseColumn.setProperty(IndexChineseSimplified.NAME, String.valueOf(indexChineseSimplified.value()));
		IndexCjkwidthConvert indexCjkwidthConvert = field.getDeclaredAnnotation(IndexCjkwidthConvert.class);
		if(indexCjkwidthConvert != null)
			trsDatabaseColumn.setProperty(IndexCjkwidthConvert.NAME, String.valueOf(indexCjkwidthConvert.value()));
		IndexGraph indexGraph = field.getDeclaredAnnotation(IndexGraph.class);
		if(indexGraph != null)
			trsDatabaseColumn.setProperty(IndexGraph.NAME, indexGraph.value());
		IndexGraphModel indexGraphModel = field.getDeclaredAnnotation(IndexGraphModel.class);
		if(indexGraphModel != null)
			trsDatabaseColumn.setProperty(IndexGraphModel.NAME, indexGraphModel.value());
		IndexIdeoAll indexIdeoAll = field.getDeclaredAnnotation(IndexIdeoAll.class);
		if(indexIdeoAll != null)
			trsDatabaseColumn.setProperty(IndexIdeoAll.NAME, String.valueOf(indexIdeoAll.value()));
		IndexIdeoSingle indexIdeoSingle = field.getDeclaredAnnotation(IndexIdeoSingle.class);
		if(indexIdeoSingle != null)
			trsDatabaseColumn.setProperty(IndexIdeoSingle.NAME, String.valueOf(indexIdeoSingle.value()));
		IndexIpaddress indexIpaddress = field.getDeclaredAnnotation(IndexIpaddress.class);
		if(indexIpaddress != null)
			trsDatabaseColumn.setProperty(IndexIdeoSingle.NAME, String.valueOf(indexIpaddress.value()));
		IndexPinyin indexPinyin = field.getDeclaredAnnotation(IndexPinyin.class);
		if(indexPinyin != null)
			trsDatabaseColumn.setProperty(IndexPinyin.NAME, String.valueOf(indexPinyin.value()));
		IndexPinyinFirst indexPinyinFirst = field.getDeclaredAnnotation(IndexPinyinFirst.class);
		if(indexPinyinFirst != null)
			trsDatabaseColumn.setProperty(IndexPinyinFirst.NAME, String.valueOf(indexPinyinFirst.value()));
		IndexPrefixRules indexPrefixRules = field.getDeclaredAnnotation(IndexPrefixRules.class);
		if(indexPrefixRules != null)
			trsDatabaseColumn.setProperty(IndexPrefixRules.NAME, String.valueOf(indexPrefixRules.value()));
		IndexRedundDate indexRedundDate = field.getDeclaredAnnotation(IndexRedundDate.class);
		if(indexRedundDate != null)
			trsDatabaseColumn.setProperty(IndexRedundDate.NAME, String.valueOf(indexRedundDate.value()));
		IndexRedundIdeo indexRedundIdeo = field.getDeclaredAnnotation(IndexRedundIdeo.class);
		if(indexRedundIdeo != null)
			trsDatabaseColumn.setProperty(IndexRedundIdeo.NAME, String.valueOf(indexRedundIdeo.value()));
		IndexRegexRules indexRegexRules = field.getDeclaredAnnotation(IndexRegexRules.class);
		if(indexRegexRules != null)
			trsDatabaseColumn.setProperty(IndexRegexRules.NAME, indexRegexRules.value());
		IndexSchema indexSchema = field.getDeclaredAnnotation(IndexSchema.class);
		if(indexSchema != null)
			trsDatabaseColumn.setProperty(IndexSchema.NAME, indexSchema.value());
		IndexStemSnowball indexStemSnowball = field.getDeclaredAnnotation(IndexStemSnowball.class);
		if(indexStemSnowball != null)
			trsDatabaseColumn.setProperty(IndexStemSnowball.NAME, String.valueOf(indexStemSnowball.value()));
		IndexStopNone indexStopNone = field.getDeclaredAnnotation(IndexStopNone.class);
		if(indexStopNone != null)
			trsDatabaseColumn.setProperty(IndexStopNone.NAME, String.valueOf(indexStemSnowball.value()));
		IndexTextualFeature indexTextualFeature = field.getDeclaredAnnotation(IndexTextualFeature.class);
		if(indexTextualFeature != null)
			trsDatabaseColumn.setProperty(IndexTextualFeature.NAME, String.valueOf(indexTextualFeature.value()));
		IndexTextualFeatureModel indexTextualFeatureModel = field.getDeclaredAnnotation(IndexTextualFeatureModel.class);
		if(indexTextualFeatureModel != null)
			trsDatabaseColumn.setProperty(IndexTextualFeatureModel.NAME, indexTextualFeatureModel.value());
		IndexVirtualColumns indexVirtualColumns = field.getDeclaredAnnotation(IndexVirtualColumns.class);
		if(indexVirtualColumns != null)
			trsDatabaseColumn.setProperty(IndexVirtualColumns.NAME, indexVirtualColumns.value());
		return trsDatabaseColumn;
	}
	/**
	 * 根据注解, 为TRSDatabase 实例对象设置注解
	 * @param database
	 * @param db
	 * @return
	 */
	private static TRSDatabase addDatabaseProperties(Database database, TRSDatabase db) {
		db.setEngineType(database.engineType());
		db.setParser(database.parser());
		String partColumnName = database.partColumnName();
		if(partColumnName != null)
			db.setParter(partColumnName);
		String splitter = database.splitter();
		String splitColumn = database.splitColumnName();
		String splitParams = database.splitParams();
		if(splitter != null && splitColumn != null) {
			if(splitParams == null) {
				db.setSplitter(splitter, partColumnName, null);
			}else {
				@SuppressWarnings("unchecked")
				HashMap<String,String> splitMap = GSON.fromJson(splitParams, HashMap.class);
				db.setSplitter(splitter, splitColumn, splitMap);
			}
		}
		return db;
	}
}
