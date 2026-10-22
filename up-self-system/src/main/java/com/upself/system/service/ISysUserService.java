package com.upself.system.service;

import com.upself.common.core.domain.entity.SysUser;

import java.util.*;

public interface ISysUserService {
    List<SysUser> list(String name);

    List<Map<String, Object>> candidates(String role, String keyword);

    Long create(String name, String nick, String password, Long roleId);

    void update(Long id, String nick, Long roleId, String status);

    void resetPassword(Long id, String password);

    void changePassword(Long id, String oldPassword, String newPassword);

    void resetTotp(Long id);
}
