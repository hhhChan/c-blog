package com.can.cblog.base.vo;

import com.can.cblog.base.validator.annotation.IdValid;
import com.can.cblog.base.validator.group.Delete;
import com.can.cblog.base.validator.group.Update;
import lombok.Data;

/**
 * @author ccc
 */
@Data
public class BaseVO<T> extends PageInfo<T> {

    /**
     * 唯一UID
     */
    @IdValid(groups = {Update.class, Delete.class})
    private String uid;

    private Integer status;
}
