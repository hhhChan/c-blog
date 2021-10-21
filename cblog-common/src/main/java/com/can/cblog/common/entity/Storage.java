package com.can.cblog.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.can.cblog.base.entity.SuperEntity;
import lombok.Data;

/**
 * 存储信息实体类
 * @author ccc
 */
@TableName("t_storage")
@Data
public class Storage extends SuperEntity<Storage> {

    /**
     * 管理员UID
     */
    private String adminUid;

    /**
     * 当前网盘容量
     */
    private long storageSize;

    /**
     * 最大网盘容量
     */
    private long maxStorageSize;
}

