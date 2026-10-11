package com.evernight.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件路径安全检查注解
 * 用于标注需要进行路径安全检查的参数
 */
@Target(ElementType.PARAMETER)  // 参数注解
@Retention(RetentionPolicy.RUNTIME)  // 运行时注解
public @interface CheckFilePath {

    /**
     * 路径前缀（可选）
     * 用于强制添加安全前缀，如"uploads/"
     */
    String prefix() default "";

    /**
     * 是否为目录（以斜杠结尾）
     */
    boolean isDirectory() default false;

    /**
     * 是否为格式矫正
     */
    boolean formatCorrection() default false;

    /**
     * 允许的扩展名（可选）
     * 为空表示不限制文件类型
     */
    String[] allowedExtensions() default {};

    /**
     * 最大路径长度
     */
    int maxLength() default 1024;

    /**
     * 自定义验证错误信息
     */
    String message() default "文件路径不安全或包含非法字符";
}
