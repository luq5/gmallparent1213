package com.atguigu.gmall1213.common.cache;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//注解方法上使用
@Retention(RetentionPolicy.RUNTIME)
public @interface GmallCache {
    //表示前缀
    String prefix() default "cache";


}
