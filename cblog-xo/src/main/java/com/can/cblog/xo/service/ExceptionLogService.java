package com.can.cblog.xo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.can.cblog.base.service.SuperService;
import com.can.cblog.common.entity.ExceptionLog;
import com.can.cblog.xo.vo.ExceptionLogVO;

/**
 * 操作异常日志 服务类
 * @author ccc
 */
public interface ExceptionLogService extends SuperService<ExceptionLog> {

    /**
     * 获取异常日志列表
     *
     * @param exceptionLogVO
     * @return
     */
    public IPage<ExceptionLog> getPageList(ExceptionLogVO exceptionLogVO);
}
