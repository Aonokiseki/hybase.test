package com.trs.hybase.test.annotation.database;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.trs.hybase.client.TRSDatabase;

@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface Database {
	String name();
	int type() default TRSDatabase.TYPE_VIEW;
	String policy() default "fastest";
	String parser() default TRSDatabase.ANALYZER_TRSSTD;
	String engineType() default "common";
	String partColumnName();
	String splitColumnName();
	String splitter();
	String splitParams();
}
