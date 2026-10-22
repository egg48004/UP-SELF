package com.upself.common.core.domain.model;

import lombok.Data;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Data
public class LoginUser implements UserDetails {
    private Long userId;
    private String username;
    private Long roleId;
    private String roleKey;
    private String password;
    private long authVersion;
    private String channel;
    private String token;
    private long loginTime;
    private long expireTime;
    private Set<String> permissions = new HashSet<>();

    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(channel));
    }

    public String getUsername() {
        return username;
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }
}
