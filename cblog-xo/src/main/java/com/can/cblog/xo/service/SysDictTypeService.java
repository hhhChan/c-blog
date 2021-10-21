package com.can.cblog.xo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.can.cblog.base.service.SuperService;
import com.can.cblog.common.entity.SysDictType;
import com.can.cblog.xo.vo.SysDictTypeVO;

import java.util.List;

/**
 * 字典类型 服务类
 * @author ccc
 */
public interface SysDictTypeService extends SuperService<SysDictType> {
    /**
     * 获取字典类型列表
     *
     * @param sysDictTypeVO
     * @return
     */
    public IPage<SysDictType> getPageList(SysDictTypeVO sysDictTypeVO);

    /**
     * 新增字典类型
     *
     * @param sysDictTypeVO
     */
    public String addSysDictType(SysDictTypeVO sysDictTypeVO);

    /**
     * 编辑字典类型
     *
     * @param sysDictTypeVO
     */
    public String editSysDictType(SysDictTypeVO sysDictTypeVO);

    /**
     * 批量删除字典类型
     *
     * @param sysDictTypeVOList
     */
    public String deleteBatchSysDictType(List<SysDictTypeVO> sysDictTypeVOList);
}
