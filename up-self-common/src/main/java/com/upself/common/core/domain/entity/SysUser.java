package com.upself.common.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.util.Date;

@Data
public class SysUser {
    private Long id;
    private String userName;
    private String nickName;
    private Long roleId;
    private String roleKey;
    private String status;
    private long authVersion;
    private Date createTime;
    @JsonIgnore private String password;
    @JsonIgnore private String totpSecret;

    public boolean isSuperAdmin() {
        return Long.valueOf(1).equals(id) || Long.valueOf(1).equals(roleId);
    }
}
