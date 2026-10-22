package com.upself.framework.web.service;

import com.upself.common.core.domain.entity.SysUser;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.system.mapper.SysUserMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service("adminUserDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired private SysUserMapper mapper;

    public UserDetails loadUserByUsername(String name) {
        SysUser u = mapper.selectByName(name);
        if (u == null || !"0".equals(u.getStatus()))
            throw new UsernameNotFoundException("The username or password is incorrect.");
        return from(u);
    }

    public LoginUser from(SysUser u) {
        LoginUser l = new LoginUser();
        l.setUserId(u.getId());
        l.setUsername(u.getUserName());
        l.setPassword(u.getPassword());
        l.setRoleId(u.getRoleId());
        l.setRoleKey(u.getRoleKey());
        l.setChannel("student".equals(u.getRoleKey()) ? "student" : "admin");
        l.setAuthVersion(u.getAuthVersion());
        l.setPermissions(
                u.isSuperAdmin()
                        ? new java.util.HashSet<>(java.util.Collections.singleton("*:*:*"))
                        : mapper.selectPermissions(u.getRoleId()));
        return l;
    }
}
