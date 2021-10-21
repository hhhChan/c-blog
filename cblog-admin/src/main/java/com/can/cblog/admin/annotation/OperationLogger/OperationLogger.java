package com.can.cblog.admin.annotation.OperationLogger;

import com.can.cblog.base.enums.PlatformEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注该该注解的方法需要记录操作日志
 * @author ccc
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogger {

    /**
     * 业务名称
     */
    String value() default "";

    /**
     * 平台，默认为WEB端
     */
    PlatformEnum platform() default PlatformEnum.ADMIN;

    /**
     * 是否将当前日志记录到数据库中
     */
    boolean save() default true;
}