package com.can.cblog.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.can.cblog.base.entity.SuperEntity;
import lombok.Data;

/**
 *
 * <p>
 * 操作日志记录表
 * </p>
 * @author ccc
 */
@Data
@TableName("t_sys_log")
public class SysLog extends SuperEntity<SysLog> {

    /**
     * 序列化ID
     */
    private static final long serialVersionUID = -4851055162892178225L;

    /**
     * 操作用户名
     */
    private String userName;

    /**
     * 操作人uid
     */
    private String adminUid;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * ip来源
     */
    private String ipSource;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 请求方式 GET POST
     */
    private String type;

    /**
     * 请求类路径
     */
    private String classPath;

    /**
     * 方法名
     */
    private String method;

    /**
     * 参数
     */
    private String params;

    /**
     * 描述
     */
    private String operation;

    /**
     * 方法请求花费的时间，单位毫秒
     */
    private Long spendTime;
}
