package com.upself.common.utils;

import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static LoginUser getLoginUser() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(p instanceof LoginUser)) throw new ServiceException(401, "Please sign in.");
        return (LoginUser) p;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }
}
