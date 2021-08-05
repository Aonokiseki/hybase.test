package com.trs.hybase.test.pojo;

import java.io.File;
import java.time.LocalDateTime;

import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.test.annotation.column.Column;
import com.trs.hybase.test.annotation.column.IndexRedundDate;
import com.trs.hybase.test.annotation.column.IndexSchema;
import com.trs.hybase.test.annotation.column.datagen.ValueCount;
import com.trs.hybase.test.annotation.column.datagen.Id;
import com.trs.hybase.test.annotation.column.datagen.RandomDatetime;
import com.trs.hybase.test.annotation.column.datagen.RandomNumber;
import com.trs.hybase.test.annotation.database.Database;
import com.trs.hybase.test.annotation.database.GenerateRecords;

@Database(
		name="system.view",
		type=TRSDatabase.TYPE_VIEW,
		policy="fastest",
		partColumnName="id",
		splitColumnName="日期",
		splitter="date",
		splitParams="{\"split.date.start\"=\"2021.01.01\", \"split.date.end\"=\"2021.12.31\", \"split.date.level\"=\"day\"}")
@GenerateRecords(count=10)
public class SimpleViewDebug{
	
	@Column(name="id", type=TRSDatabaseColumn.TYPE_CHAR)
	@Id
	private String id;
	
	@Column(name="版次", type=TRSDatabaseColumn.TYPE_NUMBER)
	@RandomNumber
	@ValueCount(count=3)
	private Integer version;
	
	@Column(name="日期", type=TRSDatabaseColumn.TYPE_DATE)
	@IndexRedundDate
	@RandomDatetime
	private LocalDateTime dateTime;
	
	@Column(name="作者", type=TRSDatabaseColumn.TYPE_CHAR)
	private String author;
	
	@Column(name="标题", type=TRSDatabaseColumn.TYPE_PHRASE)
	private String title;
	
	@Column(name="正文", type=TRSDatabaseColumn.TYPE_DOCUMENT)
	private String content;
	
	@Column(name="图像", type=TRSDatabaseColumn.TYPE_BIT)
	private File bit;
	
	@Column(name="对象", type=TRSDatabaseColumn.TYPE_OBJECT)
	@IndexSchema
	private String object;

	@Override
	public String toString() {
		return "SimpleView [id=" + id + ", version=" + version + ", dateTime=" + dateTime + ", author=" + author
				+ ", title=" + title + ", content=" + content + ", bit=" + bit + ", object=" + object + "]";
	}
	
	public static SimpleViewDebug create() {
		return new SimpleViewDebug();
	}

	public String getId() {
		return id;
	}

	public SimpleViewDebug setId(String id) {
		this.id = id;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public SimpleViewDebug setVersion(Integer version) {
		this.version = version;
		return this;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public SimpleViewDebug setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
		return this;
	}

	public String getAuthor() {
		return author;
	}

	public SimpleViewDebug setAuthor(String author) {
		this.author = author;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public SimpleViewDebug setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getContent() {
		return content;
	}

	public SimpleViewDebug setContent(String content) {
		this.content = content;
		return this;
	}

	public File getBit() {
		return bit;
	}

	public SimpleViewDebug setBit(File bit) {
		this.bit = bit;
		return this;
	}

	public String getObject() {
		return object;
	}

	public SimpleViewDebug setObject(String object) {
		this.object = object;
		return this;
	}
}
