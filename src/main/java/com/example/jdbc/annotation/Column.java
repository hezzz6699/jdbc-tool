package com.example.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 列名注解：标注在字段上，用于指定字段所映射的数据库列名。
 * 若不使用该注解，默认使用「字段名的驼峰转下划线小写」作为列名。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    /** 数据库列名 */
    String name() default "";
}
