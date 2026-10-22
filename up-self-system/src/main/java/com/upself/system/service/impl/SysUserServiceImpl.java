package com.upself.system.service.impl;

import com.upself.common.core.domain.entity.SysUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.*;
import com.upself.system.mapper.*;
import com.upself.system.service.ISysUserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SysUserServiceImpl implements ISysUserService {
    @Autowired private SysUserMapper mapper;
    @Autowired private SysAuditMapper audit;

    public List<SysUser> list(String name) {
        return mapper.selectList(name);
    }

    private void role(Long role) {
        if (role == null || !Arrays.asList(10L, 20L, 30L).contains(role))
            throw new ServiceException(
                    403, "Only Student, Mentor and Platform administrator roles can be assigned.");
    }

    private SysUser target(Long id) {
        SysUser u = mapper.selectById(id);
        if (u == null) throw new ServiceException(404, "The account does not exist.");
        if (u.isSuperAdmin())
            throw new ServiceException(403, "The protected maintenance account cannot be changed.");
        return u;
    }

    public java.util.List<java.util.Map<String, Object>> candidates(String role, String keyword) {
        return mapper.selectCandidates(role, keyword);
    }

    @Transactional
    public Long create(String name, String nick, String password, Long roleId) {
        role(roleId);
        SysUser u = new SysUser();
        u.setUserName(name);
        u.setNickName(nick);
        u.setRoleId(roleId);
        u.setPassword(PasswordUtils.encode(password));
        mapper.insert(u);
        audit.insert(SecurityUtils.getUserId(), "USER_CREATE", u.getId().toString(), "SUCCESS");
        return u.getId();
    }

    @Transactional
    public void update(Long id, String nick, Long roleId, String status) {
        SysUser u = target(id);
        if (Long.valueOf(30).equals(u.getRoleId())
                && !id.equals(SecurityUtils.getUserId())
                && !SecurityUtils.getLoginUser().getRoleKey().equals("admin"))
            throw new ServiceException(
                    403,
                    "Only the maintenance account can change another administrator role or"
                        + " status.");
        role(roleId);
        if (!"0".equals(status) && !"1".equals(status))
            throw new ServiceException(400, "Invalid account status.");
        if (id.equals(SecurityUtils.getUserId())
                && (!u.getRoleId().equals(roleId) || !"0".equals(status)))
            throw new ServiceException(
                    403, "You cannot disable your own account or change your own role.");
        u.setNickName(nick);
        u.setRoleId(roleId);
        u.setStatus(status);
        mapper.update(u);
        audit.insert(SecurityUtils.getUserId(), "USER_UPDATE", id.toString(), "SUCCESS");
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        SysUser u = target(id);
        if (Long.valueOf(30).equals(u.getRoleId())
                && !id.equals(SecurityUtils.getUserId())
                && !SecurityUtils.getLoginUser().getRoleKey().equals("admin"))
            throw new ServiceException(
                    403,
                    "Only the maintenance account can recover another administrator credentials.");
        mapper.changePassword(id, PasswordUtils.encode(password));
        audit.insert(SecurityUtils.getUserId(), "PASSWORD_RESET", id.toString(), "SUCCESS");
    }

    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        SysUser u = mapper.selectById(id);
        if (u == null || !PasswordUtils.matches(oldPassword, u.getPassword()))
            throw new ServiceException(1010, "The current password is incorrect.");
        mapper.changePassword(id, PasswordUtils.encode(newPassword));
        audit.insert(id, "PASSWORD_CHANGE", id.toString(), "SUCCESS");
    }

    @Transactional
    public void resetTotp(Long id) {
        if (!SecurityUtils.getLoginUser().getRoleKey().equals("admin"))
            throw new ServiceException(
                    403, "Only the maintenance account can reset authenticators.");
        target(id);
        mapper.resetTotp(id);
        audit.insert(SecurityUtils.getUserId(), "TOTP_RESET", id.toString(), "SUCCESS");
    }
}
