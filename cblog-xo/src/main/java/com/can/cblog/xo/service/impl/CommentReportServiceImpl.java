package com.can.cblog.xo.service.impl;

import com.can.cblog.base.service.impl.SuperServiceImpl;
import com.can.cblog.common.entity.CommentReport;
import com.can.cblog.xo.mapper.CommentReportMapper;
import com.can.cblog.xo.service.CommentReportService;
import org.springframework.stereotype.Service;

/**
 * 评论举报表 服务实现类
 * @author ccc
 */
@Service
public class CommentReportServiceImpl extends SuperServiceImpl<CommentReportMapper, CommentReport> implements CommentReportService {

}
