package com.can.cblog.xo.service.impl;

import com.can.cblog.base.service.impl.SuperServiceImpl;
import com.can.cblog.common.entity.Visitor;
import com.can.cblog.xo.mapper.VisitorMapper;
import com.can.cblog.xo.service.VisitorService;
import org.springframework.stereotype.Service;

/**
 * 博主表 服务实现类
 * @author ccc
 */
@Service
public class VisitorServiceImpl extends SuperServiceImpl<VisitorMapper, Visitor> implements VisitorService {

}
