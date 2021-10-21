package com.can.cblog.base.validator.annotation;

import com.can.cblog.base.validator.Messages;
import com.can.cblog.base.validator.constraint.BooleanValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * @author ccc
 */
@Target({TYPE, ANNOTATION_TYPE, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {BooleanValidator.class})
public @interface BooleanNotNULL {

    boolean required() default true;

    //定义消息模板，校验失败时输出
    String message() default Messages.CK_NOT_NULL_DEFAULT;

    String value() default "";

    //用于校验分组
    Class<?>[] groups() default {};

    //Bean Validation API 的使用者可以通过此属性来给约束条件指定严重级别. 这个属性并不被API自身所使用
    Class<? extends Payload>[] payload() default {};
}
