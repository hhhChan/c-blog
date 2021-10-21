package com.can.cblog.admin.restapi;

import com.can.cblog.admin.annotation.AuthorityVerify.AuthorityVerify;
import com.can.cblog.admin.annotation.AvoidRepeatableCommit.AvoidRepeatableCommit;
import com.can.cblog.admin.annotation.OperationLogger.OperationLogger;
import com.can.cblog.base.exception.ThrowableUtils;
import com.can.cblog.base.validator.group.Delete;
import com.can.cblog.base.validator.group.GetList;
import com.can.cblog.base.validator.group.Insert;
import com.can.cblog.base.validator.group.Update;
import com.can.cblog.utils.ResultUtil;
import com.can.cblog.xo.service.ResourceSortService;
import com.can.cblog.xo.vo.ResourceSortVO;
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
 * 资源分类表 RestApi
 * @author ccc
 */
@Api(value = "资源分类相关接口", tags = {"资源分类相关接口"})
@RestController
@RequestMapping("/resourceSort")
@Slf4j
public class ResourceSortRestApi {

    @Autowired
    private ResourceSortService resourceSortService;

    @AuthorityVerify
    @ApiOperation(value = "获取资源分类列表", notes = "获取资源分类列表", response = String.class)
    @PostMapping("/getList")
    public String getList(@Validated({GetList.class}) @RequestBody ResourceSortVO resourceSortVO, BindingResult result) {

        ThrowableUtils.checkParamArgument(result);
        log.info("获取资源分类列表:{}", resourceSortVO);
        return ResultUtil.successWithData(resourceSortService.getPageList(resourceSortVO));
    }

    @AvoidRepeatableCommit
    @AuthorityVerify
    @OperationLogger(value = "增加资源分类")
    @ApiOperation(value = "增加资源分类", notes = "增加资源分类", response = String.class)
    @PostMapping("/add")
    public String add(@Validated({Insert.class}) @RequestBody ResourceSortVO resourceSortVO, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("增加资源分类:{}", resourceSortVO);
        return resourceSortService.addResourceSort(resourceSortVO);
    }

    @AuthorityVerify
    @OperationLogger(value = "编辑资源分类")
    @ApiOperation(value = "编辑资源分类", notes = "编辑资源分类", response = String.class)
    @PostMapping("/edit")
    public String edit(@Validated({Update.class}) @RequestBody ResourceSortVO resourceSortVO, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("编辑资源分类:{}", resourceSortVO);
        return resourceSortService.editResourceSort(resourceSortVO);
    }

    @AuthorityVerify
    @OperationLogger(value = "批量删除资源分类")
    @ApiOperation(value = "批量删除资源分类", notes = "批量删除资源分类", response = String.class)
    @PostMapping("/deleteBatch")
    public String delete(@Validated({Delete.class}) @RequestBody List<ResourceSortVO> resourceSortVOList, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("批量删除资源分类:{}", resourceSortVOList);
        return resourceSortService.deleteBatchResourceSort(resourceSortVOList);
    }

    @AuthorityVerify
    @OperationLogger(value = "置顶资源分类")
    @ApiOperation(value = "置顶分类", notes = "置顶分类", response = String.class)
    @PostMapping("/stick")
    public String stick(@Validated({Delete.class}) @RequestBody ResourceSortVO resourceSortVO, BindingResult result) {

        // 参数校验
        ThrowableUtils.checkParamArgument(result);
        log.info("置顶分类:{}", resourceSortVO);
        return resourceSortService.stickResourceSort(resourceSortVO);
    }
}
