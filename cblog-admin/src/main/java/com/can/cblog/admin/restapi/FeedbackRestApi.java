package com.can.cblog.admin.restapi;

import com.can.cblog.admin.annotation.AuthorityVerify.AuthorityVerify;
import com.can.cblog.admin.annotation.OperationLogger.OperationLogger;
import com.can.cblog.admin.global.SysConf;
import com.can.cblog.base.exception.ThrowableUtils;
import com.can.cblog.base.validator.group.Delete;
import com.can.cblog.base.validator.group.GetList;
import com.can.cblog.base.validator.group.Update;
import com.can.cblog.utils.ResultUtil;
import com.can.cblog.xo.service.FeedbackService;
import com.can.cblog.xo.vo.FeedbackVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 反馈表 RestApi
 * @author ccc
 */
@RestController
@Api(value = "用户反馈相关接口", tags = {"用户反馈相关接口"})
@RequestMapping("/feedback")
@Slf4j
public class FeedbackRestApi {

    @Autowired
    private FeedbackService feedbackService;

    @AuthorityVerify
    @ApiOperation(value = "获取反馈列表", notes = "获取反馈列表", response = String.class)
    @PostMapping("/getList")
    public String getList(@Validated({GetList.class}) @RequestBody FeedbackVO feedbackVO, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("获取反馈列表: {}", feedbackVO);
        return ResultUtil.result(SysConf.SUCCESS, feedbackService.getPageList(feedbackVO));
    }

    @AuthorityVerify
    @OperationLogger(value = "编辑反馈")
    @ApiOperation(value = "编辑反馈", notes = "编辑反馈", response = String.class)
    @PostMapping("/edit")
    public String edit(@Validated({Update.class}) @RequestBody FeedbackVO feedbackVO, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("编辑反馈: {}", feedbackVO);
        return feedbackService.addFeedback(feedbackVO);
    }

    @AuthorityVerify
    @OperationLogger(value = "批量删除反馈")
    @ApiOperation(value = "批量删除反馈", notes = "批量删除反馈", response = String.class)
    @PostMapping("/deleteBatch")
    public String delete(@Validated({Delete.class}) @RequestBody List<FeedbackVO> feedbackVOList, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("批量删除反馈: {}", feedbackVOList);
        return feedbackService.deleteBatchFeedback(feedbackVOList);
    }

}