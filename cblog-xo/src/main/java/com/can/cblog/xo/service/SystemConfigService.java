package com.can.cblog.xo.service;

import com.can.cblog.base.service.SuperService;
import com.can.cblog.common.entity.SystemConfig;
import com.can.cblog.xo.vo.SystemConfigVO;

import java.util.List;

/**
 * @author ccc
 */
public interface SystemConfigService extends SuperService<SystemConfig> {
    /**
     * 获取系统配置
     *
     * @return
     */
    public SystemConfig getConfig();

    /**
     * 通过Key前缀清空Redis缓存
     *
     * @param key
     * @return
     */
    public String cleanRedisByKey(List<String> key);

    /**
     * 修改系统配置
     *
     * @param systemConfigVO
     * @return
     */
    public String editSystemConfig(SystemConfigVO systemConfigVO);

    /**
     * 获取系统配置中的搜索模式
     * @return
     */
    public String getSearchModel();
}
