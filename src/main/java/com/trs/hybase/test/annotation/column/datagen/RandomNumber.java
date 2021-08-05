package com.trs.hybase.test.annotation.column.datagen;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface RandomNumber {
	int lowest() default Integer.MIN_VALUE;
	int highest() default Integer.MAX_VALUE;
}
