package com.can.cblog.xo.vo;

import com.can.cblog.base.vo.BaseVO;
import lombok.Data;

/**
 * @author ccc
 */
@Data
public class ExceptionLogVO extends BaseVO<ExceptionLogVO> {

    /**
     * 操作IP
     */
    private String ip;

    /**
     * ip来源
     */
    private String ipSource;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 描述
     */
    private String operation;

    /**
     * 参数
     */
    private String params;

    /**
     * 异常对象json格式
     */
    private String exceptionJson;

    /**
     * 异常简单信息,等同于e.getMessage
     */
    private String exceptionMessage;

    /**
     * 开始时间
     */
    private String startTime;
}
