package com.trs.hybase.test.pojo;

import java.io.File;
import java.time.LocalDateTime;

import com.trs.hybase.client.TRSDatabase;
import com.trs.hybase.client.TRSDatabaseColumn;
import com.trs.hybase.test.annotation.column.Column;
import com.trs.hybase.test.annotation.column.IndexRedundDate;
import com.trs.hybase.test.annotation.column.IndexSchema;
import com.trs.hybase.test.annotation.database.Database;

@Database(
		name="system.view",
		type=TRSDatabase.TYPE_VIEW,
		policy="fastest",
		partColumnName="id",
		splitColumnName="日期",
		splitter="date",
		splitParams="{\"split.date.start\"=\"2021.01.01\", \"split.date.end\"=\"2021.12.31\", \"split.date.level\"=\"day\"}")
public class SimpleView{
	
	@Column(name="id", type=TRSDatabaseColumn.TYPE_CHAR)
	private String id;
	
	@Column(name="版次", type=TRSDatabaseColumn.TYPE_NUMBER)
	private Integer version;
	
	@Column(name="日期", type=TRSDatabaseColumn.TYPE_DATE)
	@IndexRedundDate
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
	
	public static SimpleView create() {
		return new SimpleView();
	}

	public String getId() {
		return id;
	}

	public SimpleView setId(String id) {
		this.id = id;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public SimpleView setVersion(Integer version) {
		this.version = version;
		return this;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public SimpleView setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
		return this;
	}

	public String getAuthor() {
		return author;
	}

	public SimpleView setAuthor(String author) {
		this.author = author;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public SimpleView setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getContent() {
		return content;
	}

	public SimpleView setContent(String content) {
		this.content = content;
		return this;
	}

	public File getBit() {
		return bit;
	}

	public SimpleView setBit(File bit) {
		this.bit = bit;
		return this;
	}

	public String getObject() {
		return object;
	}

	public SimpleView setObject(String object) {
		this.object = object;
		return this;
	}
}
