package com.can.cblog.base.validator.constraint;

import com.can.cblog.base.global.Constants;
import com.can.cblog.base.validator.annotation.IdValid;
import com.can.cblog.utils.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author ccc
 */
public class IdValidator implements ConstraintValidator<IdValid, String> {

    @Override
    public void initialize(IdValid constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || StringUtils.isBlank(value) || StringUtils.isEmpty(value.trim()) || value.length() != Constants.THIRTY_TWO) {
            return false;
        }
        return true;
    }
}
