package com.trs.hybase.test.annotation.column;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexTextualFeatureModel {
	public final static String NAME = "index.textual.feature.model";
	String value();
}
