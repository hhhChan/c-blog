package com.can.cblog.xo.mapper;

import com.can.cblog.base.mapper.SuperMapper;
import com.can.cblog.common.entity.Admin;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员表 Mapper 接口
 * @author ccc
 */
public interface AdminMapper extends SuperMapper<Admin> {

    /**
     * 通过uid获取管理员
     *
     * @return
     */
    public Admin getAdminByUid(@Param("uid") String uid);
}