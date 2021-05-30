package com.trs.hybase.test.annotation.column;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexVirtualColumns {
	public final static String NAME = "index.virtual.columns"; 
	String value();
}