package com.upself.web.controller.system;

import com.upself.common.core.domain.AjaxResult;
import com.upself.common.utils.SecurityUtils;
import com.upself.framework.web.service.*;
import com.upself.system.mapper.SysUserMapper;
import com.upself.system.service.ISysUserService;
import com.upself.web.model.AuthBodies.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class SysLoginController {
    @Autowired private SysLoginService login;
    @Autowired private SysUserMapper users;
    @Autowired private ISysUserService service;
    @Autowired private TotpService totp;

    @com.upself.common.annotation.RateLimiter(
            key = "upself:rate_limit:/empLogin:",
            time = 60,
            count = 20,
            limitType = com.upself.common.enums.LimitType.IP)
    @PostMapping("/empLogin")
    public AjaxResult login(@Valid @RequestBody Login b) {
        AjaxResult r = AjaxResult.success();
        r.putAll(login.login(b.getUsername(), b.getPassword(), "admin"));
        return r;
    }

    @com.upself.common.annotation.RateLimiter(
            key = "upself:rate_limit:/client/login:",
            time = 60,
            count = 20,
            limitType = com.upself.common.enums.LimitType.IP)
    @PostMapping("/client/login")
    public AjaxResult studentLogin(@Valid @RequestBody Login b) {
        AjaxResult r = AjaxResult.success();
        r.putAll(login.login(b.getUsername(), b.getPassword(), "student"));
        return r;
    }

    @com.upself.common.annotation.RateLimiter(
            key = "upself:rate_limit:/empLogin/totp:",
            time = 60,
            count = 20,
            limitType = com.upself.common.enums.LimitType.IP)
    @PostMapping("/empLogin/totp")
    public AjaxResult totp(@Valid @RequestBody Totp b) {
        AjaxResult r = AjaxResult.success();
        r.putAll(login.loginTotp(b.getTicket(), b.getCode()));
        return r;
    }

    @GetMapping({"/getInfo", "/client/getInfo"})
    public AjaxResult info() {
        AjaxResult r = AjaxResult.success();
        r.put("user", users.selectById(SecurityUtils.getUserId()));
        r.put("roles", java.util.Collections.singleton(SecurityUtils.getLoginUser().getRoleKey()));
        r.put("permissions", SecurityUtils.getLoginUser().getPermissions());
        r.put("totpEnabled", totp.isEnabled());
        return r;
    }

    @GetMapping({"/getRouters", "/client/getRouters"})
    public AjaxResult routers() {
        return AjaxResult.success(users.selectMenus(SecurityUtils.getLoginUser().getRoleId()));
    }

    @PutMapping({"/system/user/profile/updatePwd", "/client/profile/password"})
    public AjaxResult password(@Valid @RequestBody Password b) {
        service.changePassword(SecurityUtils.getUserId(), b.getOldPassword(), b.getNewPassword());
        return AjaxResult.success();
    }
}
