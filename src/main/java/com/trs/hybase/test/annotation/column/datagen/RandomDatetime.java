package com.trs.hybase.test.annotation.column.datagen;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface RandomDatetime {
	String start() default "1970-01-01 00:00:00";
	String end() default "2100-12-31 23:59:59";
	String format() default "yyyy-MM-dd HH:mm:ss";
}
