package com.evernight.entity.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户信息校验注解
 */
@Target(ElementType.PARAMETER)// 参数注解
@Retention(RetentionPolicy.RUNTIME)// 运行时注解
public @interface UserInfoValidation {
    // 是否必须校验，默认为true
    boolean required() default true;
}

