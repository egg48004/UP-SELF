package com.upself.framework.web.service;

import com.upself.common.utils.SecurityUtils;

import org.springframework.stereotype.Service;

@Service("ss")
public class PermissionService {
    public boolean hasPermi(String p) {
        java.util.Set<String> ps = SecurityUtils.getLoginUser().getPermissions();
        return ps != null && (ps.contains("*:*:*") || ps.contains(p));
    }
}
