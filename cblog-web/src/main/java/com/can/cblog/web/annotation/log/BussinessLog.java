package com.can.cblog.web.annotation.log;

import com.can.cblog.base.enums.EBehavior;
import com.can.cblog.base.enums.PlatformEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志记录、自定义注解
 * @author ccc
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BussinessLog {

    /**
     * 业务名称
     */
    String value() default "";

    /**
     * 用户行为
     *
     * @return
     */
    EBehavior behavior();

    /**
     * 平台，默认为WEB端
     */
    PlatformEnum platform() default PlatformEnum.WEB;

    /**
     * 是否将当前日志记录到数据库中
     */
    boolean save() default true;
}