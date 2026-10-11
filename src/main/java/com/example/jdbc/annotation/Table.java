package com.example.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表名注解：标注在类上，用于指定类所映射的数据库表名。
 * 若不使用该注解，默认使用「类名的驼峰转下划线小写」作为表名。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    /** 数据库表名 */
    String name() default "";
}
