package com.can.cblog.xo.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.can.cblog.base.enums.EStatus;
import com.can.cblog.base.global.Constants;
import com.can.cblog.base.holder.RequestHolder;
import com.can.cblog.base.service.impl.SuperServiceImpl;
import com.can.cblog.common.entity.Admin;
import com.can.cblog.common.entity.OnlineAdmin;
import com.can.cblog.common.entity.Role;
import com.can.cblog.common.entity.Storage;
import com.can.cblog.common.feign.PictureFeignClient;
import com.can.cblog.utils.*;
import com.can.cblog.xo.global.MessageConf;
import com.can.cblog.xo.global.RedisConf;
import com.can.cblog.xo.global.SQLConf;
import com.can.cblog.xo.global.SysConf;
import com.can.cblog.xo.mapper.AdminMapper;
import com.can.cblog.xo.service.AdminService;
import com.can.cblog.xo.service.RoleService;
import com.can.cblog.xo.service.SysParamsService;
import com.can.cblog.xo.utils.WebUtil;
import com.can.cblog.xo.vo.AdminVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ccc
 */
@Service
public class AdminServiceImpl extends SuperServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    AdminService adminService;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    SysParamsService sysParamsService;

    @Resource
    private AdminMapper adminMapper;

    @Autowired
    private WebUtil webUtil;

    @Resource
    private PictureFeignClient pictureFeignClient;

    @Autowired
    private RoleService roleService;

    @Override
    public Admin getAdminByUid(String uid) {
        return adminMapper.getAdminByUid(uid);
    }

    @Override
    public String getOnlineAdminList(AdminVO adminVO) {
        // ??????Redis??????????????????key
        Set<String> keys = redisUtil.keys(RedisConf.LOGIN_TOKEN_KEY + "*");
        List<String> onlineAdminJsonList = redisUtil.multiGet(keys);
        // ??????????????????
        int pageSize = adminVO.getPageSize().intValue();
        int currentPage = adminVO.getCurrentPage().intValue();
        int total = onlineAdminJsonList.size();
        int startIndex = Math.max((currentPage - 1) * pageSize, 0);
        int endIndex = Math.min(currentPage * pageSize, total);
        //TODO ???????????????????????????????????????????????????Redis List?????????
        List<String> onlineAdminSubList = onlineAdminJsonList.subList(startIndex, endIndex);
        List<OnlineAdmin> onlineAdminList = new ArrayList<>();
        for (String item : onlineAdminSubList) {
            OnlineAdmin onlineAdmin = JsonUtils.jsonToPojo(item, OnlineAdmin.class);
            // ??????????????????????????????token?????????
            onlineAdmin.setToken("");
            onlineAdminList.add(onlineAdmin);
        }
        Page<OnlineAdmin> page = new Page<>();
        page.setCurrent(currentPage);
        page.setTotal(total);
        page.setSize(pageSize);
        page.setRecords(onlineAdminList);
        return ResultUtil.successWithData(page);
    }

    @Override
    public Admin getAdminByUser(String userName) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.USER_NAME, userName);
        queryWrapper.last(SysConf.LIMIT_ONE);
        //???????????????????????????
        Admin admin = adminService.getOne(queryWrapper);
        admin.setPassWord(null);
        //????????????
        if (StringUtils.isNotEmpty(admin.getAvatar())) {
            String pictureList = this.pictureFeignClient.getPicture(admin.getAvatar(), Constants.SYMBOL_COMMA);
            admin.setPhotoList(webUtil.getPicture(pictureList));
        }
        Admin result = new Admin();
        result.setNickName(admin.getNickName());
        result.setOccupation(admin.getOccupation());
        result.setSummary(admin.getSummary());
        result.setAvatar(admin.getAvatar());
        result.setPhotoList(admin.getPhotoList());
        result.setPersonResume(admin.getPersonResume());
        return result;
    }

    @Override
    public Admin getMe() {
        HttpServletRequest request = RequestHolder.getRequest();
        if (request.getAttribute(SysConf.ADMIN_UID) == null || request.getAttribute(SysConf.ADMIN_UID) == "") {
            return new Admin();
        }
        Admin admin = adminService.getById(request.getAttribute(SysConf.ADMIN_UID).toString());
        //???????????????????????????
        admin.setPassWord(null);
        //????????????
        if (StringUtils.isNotEmpty(admin.getAvatar())) {
            String pictureList = this.pictureFeignClient.getPicture(admin.getAvatar(), Constants.SYMBOL_COMMA);
            admin.setPhotoList(webUtil.getPicture(pictureList));
        }
        return admin;
    }

    @Override
    public void addOnlineAdmin(Admin admin, Long expirationSecond) {
        HttpServletRequest request = RequestHolder.getRequest();
        Map<String, String> map = IpUtils.getOsAndBrowserInfo(request);
        String os = map.get(SysConf.OS);
        String browser = map.get(SysConf.BROWSER);
        String ip = IpUtils.getIpAddr(request);
        OnlineAdmin onlineAdmin = new OnlineAdmin();
        onlineAdmin.setAdminUid(admin.getUid());
        onlineAdmin.setTokenId(admin.getTokenUid());
        onlineAdmin.setToken(admin.getValidCode());
        onlineAdmin.setOs(os);
        onlineAdmin.setBrowser(browser);
        onlineAdmin.setIpaddr(ip);
        onlineAdmin.setLoginTime(DateUtils.getNowTime());
        onlineAdmin.setRoleName(admin.getRole().getRoleName());
        onlineAdmin.setUserName(admin.getUserName());
        onlineAdmin.setExpireTime(DateUtils.getDateStr(new Date(), expirationSecond));
        //???Redis?????????IP??????
        String jsonResult = redisUtil.get(RedisConf.IP_SOURCE + Constants.SYMBOL_COLON + ip);
        if (StringUtils.isEmpty(jsonResult)) {
            String addresses = IpUtils.getAddresses(SysConf.IP + SysConf.EQUAL_TO + ip, SysConf.UTF_8);
            if (StringUtils.isNotEmpty(addresses)) {
                onlineAdmin.setLoginLocation(addresses);
                redisUtil.setEx(RedisConf.IP_SOURCE + Constants.SYMBOL_COLON + ip, addresses, 24, TimeUnit.HOURS);
            }
        } else {
            onlineAdmin.setLoginLocation(jsonResult);
        }
        // ?????????????????????????????????????????????
        redisUtil.setEx(RedisConf.LOGIN_TOKEN_KEY + RedisConf.SEGMENTATION + admin.getValidCode(), JsonUtils.objectToJson(onlineAdmin), expirationSecond, TimeUnit.SECONDS);
        // ??????????????????????????? uuid - token ????????????
        redisUtil.setEx(RedisConf.LOGIN_UUID_KEY + RedisConf.SEGMENTATION + admin.getTokenUid(), admin.getValidCode(), expirationSecond, TimeUnit.SECONDS);
    }

    @Override
    public String getList(AdminVO adminVO) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        String pictureResult = null;
        if (StringUtils.isNotEmpty(adminVO.getKeyword())) {
            queryWrapper.like(SQLConf.USER_NAME, adminVO.getKeyword()).or().like(SQLConf.NICK_NAME, adminVO.getKeyword().trim());
        }
        Page<Admin> page = new Page<>();
        page.setCurrent(adminVO.getCurrentPage());
        page.setSize(adminVO.getPageSize());
        // ????????????
        queryWrapper.select(Admin.class, i -> !i.getProperty().equals(SQLConf.PASS_WORD));
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        IPage<Admin> pageList = adminService.page(page, queryWrapper);
        List<Admin> list = pageList.getRecords();

        final StringBuffer fileUids = new StringBuffer();
        List<String> adminUidList = new ArrayList<>();
        list.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getAvatar())) {
                fileUids.append(item.getAvatar() + SysConf.FILE_SEGMENTATION);
            }
            adminUidList.add(item.getUid());
        });

        Map<String, String> pictureMap = new HashMap<>(Constants.NUM_TEN);
        if (fileUids != null) {
            pictureResult = this.pictureFeignClient.getPicture(fileUids.toString(), SysConf.FILE_SEGMENTATION);
        }
        List<Map<String, Object>> picList = webUtil.getPictureMap(pictureResult);
        picList.forEach(item -> {
            pictureMap.put(item.get(SQLConf.UID).toString(), item.get(SQLConf.URL).toString());
        });

        // ?????????????????????????????????
        String storageListJson = pictureFeignClient.getStorageByAdminUid(adminUidList);
        List<Storage> storageList = webUtil.getList(storageListJson, Storage.class);
        Map<String, Storage> storageMap = new HashMap<>();
        storageList.forEach(item -> {
            storageMap.put(item.getAdminUid(), item);
        });

        for (Admin item : list) {
            Role role = roleService.getById(item.getRoleUid());
            item.setRole(role);

            //????????????
            if (StringUtils.isNotEmpty(item.getAvatar())) {
                List<String> pictureUidsTemp = StringUtils.changeStringToString(item.getAvatar(), SysConf.FILE_SEGMENTATION);
                List<String> pictureListTemp = new ArrayList<>();
                pictureUidsTemp.forEach(picture -> {
                    if (pictureMap.get(picture) != null && pictureMap.get(picture) != "") {
                        pictureListTemp.add(pictureMap.get(picture));
                    }
                });
                item.setPhotoList(pictureListTemp);
            }

            // ???????????????????????????????????????
            Storage storage = storageMap.get(item.getUid());
            if(storage != null) {
                item.setStorageSize(storage.getStorageSize());
                item.setMaxStorageSize(storage.getMaxStorageSize());
            } else {
                // ????????????????????????0
                item.setStorageSize(0L);
                item.setMaxStorageSize(0L);
            }
        }
        return ResultUtil.successWithData(pageList);
    }

    @Override
    public String addAdmin(AdminVO adminVO) {

        String mobile = adminVO.getMobile();
        String userName = adminVO.getUserName();
        String email = adminVO.getEmail();
        if (StringUtils.isEmpty(userName)) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }
        if (StringUtils.isEmpty(email) && StringUtils.isEmpty(mobile)) {
            return ResultUtil.errorWithMessage("??????????????????????????????????????????");
        }
        String defaultPassword = sysParamsService.getSysParamsValueByKey(SysConf.SYS_DEFAULT_PASSWORD);
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.USER_NAME, userName);
        Admin temp = adminService.getOne(queryWrapper);
        if (temp == null) {
            Admin admin = new Admin();
            admin.setAvatar(adminVO.getAvatar());
            admin.setEmail(adminVO.getEmail());
            admin.setGender(adminVO.getGender());
            admin.setUserName(adminVO.getUserName());
            admin.setNickName(adminVO.getNickName());
            admin.setRoleUid(adminVO.getRoleUid());
            // ????????????????????????
            admin.setStatus(EStatus.ENABLE);
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            //??????????????????
            admin.setPassWord(encoder.encode(defaultPassword));
            adminService.save(admin);
            //TODO ??????????????????SMS???????????????????????????????????????

            // ????????????????????????????????????????????????
            String maxStorageSize = sysParamsService.getSysParamsValueByKey(SysConf.MAX_STORAGE_SIZE);
            // ????????????????????????, ?????? B
            pictureFeignClient.initStorageSize(admin.getUid(), StringUtils.getLong(maxStorageSize, 0L) * 1024 * 1024);
            return ResultUtil.successWithMessage(MessageConf.INSERT_SUCCESS);
        }
        return ResultUtil.errorWithMessage(MessageConf.ENTITY_EXIST);
    }

    @Override
    public String editAdmin(AdminVO adminVO) {
        Admin admin = adminService.getById(adminVO.getUid());
        Assert.notNull(admin, MessageConf.PARAM_INCORRECT);
        //??????????????????????????????admin???admin?????????????????????admin
        if (admin.getUserName().equals(SysConf.ADMIN) && !adminVO.getUserName().equals(SysConf.ADMIN)) {
            return ResultUtil.errorWithMessage("?????????????????????????????????admin");
        }
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(SQLConf.USER_NAME, adminVO.getUserName());
        List<Admin> adminList = adminService.list(queryWrapper);
        if (adminList != null) {
            for (Admin item : adminList) {
                if (item.getUid().equals(adminVO.getUid())) {
                    continue;
                } else {
                    return ResultUtil.errorWithMessage("??????????????????????????????");
                }
            }
        }

        // ?????????????????????RoleUid?????????redis???admin???URL????????????
        if (StringUtils.isNotEmpty(adminVO.getRoleUid()) && !adminVO.getRoleUid().equals(admin.getRoleUid())) {
            redisUtil.delete(RedisConf.ADMIN_VISIT_MENU + RedisConf.SEGMENTATION + admin.getUid());
        }
        admin.setUserName(adminVO.getUserName());
        admin.setAvatar(adminVO.getAvatar());
        admin.setNickName(adminVO.getNickName());
        admin.setGender(adminVO.getGender());
        admin.setEmail(adminVO.getEmail());
        admin.setQqNumber(adminVO.getQqNumber());
        admin.setGithub(adminVO.getGithub());
        admin.setGitee(adminVO.getGitee());
        admin.setOccupation(adminVO.getOccupation());
        admin.setUpdateTime(new Date());
        admin.setMobile(adminVO.getMobile());
        admin.setRoleUid(adminVO.getRoleUid());
        // ?????????????????????????????????????????????????????????????????????
        admin.setPassWord(null);
        admin.updateById();

        // ??????????????????????????????????????????????????????
        String result = pictureFeignClient.editStorageSize(admin.getUid(), adminVO.getMaxStorageSize() * 1024 * 1024);
        Map<String, String> resultMap = webUtil.getMessage(result);
        if(SysConf.SUCCESS.equals(resultMap.get(SysConf.CODE))) {
            return ResultUtil.successWithMessage(resultMap.get(SysConf.MESSAGE));
        } else {
            return ResultUtil.errorWithMessage(resultMap.get(SysConf.MESSAGE));
        }
    }

    @Override
    public String editMe(AdminVO adminVO) {
        String adminUid = RequestHolder.getAdminUid();
        if (StringUtils.isEmpty(adminUid)) {
            return ResultUtil.errorWithMessage(MessageConf.INVALID_TOKEN);
        }
        Admin admin = new Admin();
        // ?????????Spring?????????????????????????????????????????????????????????
        BeanUtils.copyProperties(adminVO, admin, SysConf.STATUS);
        admin.setUpdateTime(new Date());
        admin.updateById();
        return ResultUtil.successWithMessage(MessageConf.OPERATION_SUCCESS);
    }

    @Override
    public String changePwd(String oldPwd, String newPwd) {
        String adminUid = RequestHolder.getAdminUid();
        if (StringUtils.isEmpty(oldPwd) || StringUtils.isEmpty(newPwd)) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }
        Admin admin = adminService.getById(adminUid);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean isPassword = encoder.matches(oldPwd, admin.getPassWord());
        if (isPassword) {
            admin.setPassWord(encoder.encode(newPwd));
            admin.setUpdateTime(new Date());
            admin.updateById();
            return ResultUtil.successWithMessage(MessageConf.UPDATE_SUCCESS);
        } else {
            return ResultUtil.errorWithMessage(MessageConf.ERROR_PASSWORD);
        }
    }

    @Override
    public String resetPwd(AdminVO adminVO) {
        String defaultPassword = sysParamsService.getSysParamsValueByKey(SysConf.SYS_DEFAULT_PASSWORD);
        // ??????????????????????????????uid
        String adminUid = RequestHolder.getAdminUid();
        Admin admin = adminService.getById(adminVO.getUid());
        // ???????????????admin???????????????????????????????????????????????????admin????????????
        if (SysConf.ADMIN.equals(admin.getUserName()) && !admin.getUid().equals(adminUid)) {
            return ResultUtil.errorWithMessage(MessageConf.UPDATE_ADMIN_PASSWORD_FAILED);
        } else {
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            admin.setPassWord(encoder.encode(defaultPassword));
            admin.setUpdateTime(new Date());
            admin.updateById();
            return ResultUtil.successWithMessage(MessageConf.UPDATE_SUCCESS);
        }
    }

    @Override
    public String deleteBatchAdmin(List<String> adminUidList) {
        boolean checkResult = StringUtils.checkUidList(adminUidList);
        if (!checkResult) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }
        List<Admin> adminList = new ArrayList<>();
        adminUidList.forEach(item -> {
            Admin admin = new Admin();
            admin.setUid(item);
            admin.setStatus(EStatus.DISABLED);
            admin.setUpdateTime(new Date());
            adminList.add(admin);
        });
        adminService.updateBatchById(adminList);
        return ResultUtil.successWithMessage(MessageConf.DELETE_SUCCESS);
    }

    @Override
    public String forceLogout(List<String> tokenUidList) {
        if (tokenUidList == null || tokenUidList.size() == 0) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }

        // ???Redis?????????TokenUid????????????????????????token
        List<String> tokenList = new ArrayList<>();
        tokenUidList.forEach(item -> {
            String token = redisUtil.get(RedisConf.LOGIN_UUID_KEY + RedisConf.SEGMENTATION + item);
            if(StringUtils.isNotEmpty(token)) {
                tokenList.add(token);
            }
        });

        // ??????token??????Redis??????????????????
        List<String> keyList = new ArrayList<>();
        String keyPrefix = RedisConf.LOGIN_TOKEN_KEY + RedisConf.SEGMENTATION;
        for (String token : tokenList) {
            keyList.add(keyPrefix + token);
        }
        redisUtil.delete(keyList);
        return ResultUtil.successWithMessage(MessageConf.OPERATION_SUCCESS);
    }
}
