package com.can.cblog.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.can.cblog.base.entity.SuperEntity;
import lombok.Data;

/**
 * @author ccc
 */
@Data
@TableName("t_todo")
public class Todo extends SuperEntity<Todo> {

    private static final long serialVersionUID = 1L;

    /**
     * 内容
     */
    private String text;

    /**
     * 管理员UID
     */
    private String adminUid;

    /**
     * 表示事项是否完成
     */
    private Boolean done;
}
