package com.upself.system.mapper;

import com.upself.common.core.domain.entity.SysUser;

import org.apache.ibatis.annotations.Param;

import java.util.*;

public interface SysUserMapper {
    SysUser selectByName(@Param("name") String name);

    SysUser selectById(@Param("id") Long id);

    List<SysUser> selectList(@Param("name") String name);

    List<Map<String, Object>> selectCandidates(
            @Param("role") String role, @Param("keyword") String keyword);

    int count();

    int insert(SysUser user);

    int update(SysUser user);

    int changePassword(@Param("id") Long id, @Param("password") String password);

    int bindTotp(@Param("id") Long id, @Param("secret") String secret);

    int resetTotp(@Param("id") Long id);

    Set<String> selectPermissions(@Param("roleId") Long roleId);

    List<Map<String, Object>> selectMenus(@Param("roleId") Long roleId);

    List<Map<String, Object>> selectRoles();
}
