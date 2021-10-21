package com.can.cblog.xo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.can.cblog.base.service.SuperService;
import com.can.cblog.common.entity.SysDictData;
import com.can.cblog.xo.vo.SysDictDataVO;

import java.util.List;
import java.util.Map;

/**
 * 字典数据 服务类
 * @author ccc
 */
public interface SysDictDataService extends SuperService<SysDictData> {
    /**
     * 获取数据字典列表
     *
     * @param sysDictDataVO
     * @return
     */
    public IPage<SysDictData> getPageList(SysDictDataVO sysDictDataVO);

    /**
     * 新增数据字典
     *
     * @param sysDictDataVO
     */
    public String addSysDictData(SysDictDataVO sysDictDataVO);

    /**
     * 编辑数据字典
     *
     * @param sysDictDataVO
     */
    public String editSysDictData(SysDictDataVO sysDictDataVO);

    /**
     * 批量删除数据字典
     *
     * @param sysDictDataVOList
     */
    public String deleteBatchSysDictData(List<SysDictDataVO> sysDictDataVOList);

    /**
     * 根据字典类型获取字典数据
     *
     * @param dictType
     * @return
     */
    public Map<String, Object> getListByDictType(String dictType);

    /**
     * 根据字典类型数组获取字典数据
     *
     * @param dictTypeList
     * @return
     */
    public Map<String, Map<String, Object>> getListByDictTypeList(List<String> dictTypeList);

}
