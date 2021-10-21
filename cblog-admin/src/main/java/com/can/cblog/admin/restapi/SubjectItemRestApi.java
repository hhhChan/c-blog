package com.can.cblog.admin.restapi;

import com.can.cblog.admin.annotation.AuthorityVerify.AuthorityVerify;
import com.can.cblog.admin.annotation.AvoidRepeatableCommit.AvoidRepeatableCommit;
import com.can.cblog.admin.annotation.OperationLogger.OperationLogger;
import com.can.cblog.base.exception.ThrowableUtils;
import com.can.cblog.base.validator.group.GetList;
import com.can.cblog.utils.ResultUtil;
import com.can.cblog.xo.service.SubjectItemService;
import com.can.cblog.xo.vo.SubjectItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专题Item表 RestApi
 * @author ccc
 */
@Api(value = "专题Item相关接口", tags = {"专题Item相关接口"})
@RestController
@RequestMapping("/subjectItem")
@Slf4j
public class SubjectItemRestApi {

    @Autowired
    private SubjectItemService subjectItemService;

    @AuthorityVerify
    @ApiOperation(value = "获取专题Item列表", notes = "获取专题Item列表", response = String.class)
    @PostMapping("/getList")
    public String getList(@Validated({GetList.class}) @RequestBody SubjectItemVO subjectItemVO, BindingResult result) {
        ThrowableUtils.checkParamArgument(result);
        return ResultUtil.successWithData(subjectItemService.getPageList(subjectItemVO));
    }

    @AvoidRepeatableCommit
    @AuthorityVerify
    @OperationLogger(value = "增加专题Item")
    @ApiOperation(value = "增加专题Item", notes = "增加专题Item", response = String.class)
    @PostMapping("/add")
    public String add(@RequestBody List<SubjectItemVO> subjectItemVOList, BindingResult result) {
        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        return subjectItemService.addSubjectItemList(subjectItemVOList);
    }

    @AuthorityVerify
    @OperationLogger(value = "编辑专题Item")
    @ApiOperation(value = "编辑专题Item", notes = "编辑专题Item", response = String.class)
    @PostMapping("/edit")
    public String edit(@RequestBody List<SubjectItemVO> subjectItemVOList, BindingResult result) {
        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        return subjectItemService.editSubjectItemList(subjectItemVOList);
    }

    @AuthorityVerify
    @OperationLogger(value = "批量删除专题Item")
    @ApiOperation(value = "批量删除专题Item", notes = "批量删除专题Item", response = String.class)
    @PostMapping("/deleteBatch")
    public String delete(@RequestBody List<SubjectItemVO> subjectItemVOList, BindingResult result) {
        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        return subjectItemService.deleteBatchSubjectItem(subjectItemVOList);
    }

    @AuthorityVerify
    @OperationLogger(value = "通过创建时间排序专题列表")
    @ApiOperation(value = "通过创建时间排序专题列表", notes = "通过创建时间排序专题列表", response = String.class)
    @PostMapping("/sortByCreateTime")
    public String sortByCreateTime(@ApiParam(name = "subjectUid", value = "专题uid") @RequestParam(name = "subjectUid", required = true) String subjectUid,
                                   @ApiParam(name = "isDesc", value = "是否从大到小排列") @RequestParam(name = "isDesc", required = false, defaultValue = "false") Boolean isDesc) {
        log.info("通过点击量排序博客分类");
        return subjectItemService.sortByCreateTime(subjectUid, isDesc);
    }
}
