package com.can.cblog.admin.restapi;

import com.can.cblog.admin.annotation.AuthorityVerify.AuthorityVerify;
import com.can.cblog.admin.annotation.OperationLogger.OperationLogger;
import com.can.cblog.base.validator.group.Update;
import com.can.cblog.utils.ResultUtil;
import com.can.cblog.xo.service.WebConfigService;
import com.can.cblog.xo.vo.WebConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 网站配置表 RestApi
 * @author ccc
 */
@Api(value = "网站配置相关接口", tags = {"网站配置相关接口"})
@RestController
@RequestMapping("/webConfig")
@Slf4j
public class WebConfigRestApi {

    @Autowired
    private WebConfigService webConfigService;

    @AuthorityVerify
    @ApiOperation(value = "获取网站配置", notes = "获取网站配置")
    @GetMapping("/getWebConfig")
    public String getWebConfig() {
        return ResultUtil.successWithData(webConfigService.getWebConfig());
    }

    @AuthorityVerify
    @OperationLogger(value = "修改网站配置")
    @ApiOperation(value = "修改网站配置", notes = "修改网站配置")
    @PostMapping("/editWebConfig")
    public String editWebConfig(@Validated({Update.class}) @RequestBody WebConfigVO webConfigVO, BindingResult result) {
        return webConfigService.editWebConfig(webConfigVO);
    }
}
