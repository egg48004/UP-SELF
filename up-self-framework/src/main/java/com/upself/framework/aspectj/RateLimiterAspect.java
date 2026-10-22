package com.upself.framework.aspectj;

import com.upself.common.annotation.RateLimiter;
import com.upself.common.enums.LimitType;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.SecurityUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RateLimiterAspect {
    @Autowired private StringRedisTemplate redis;
    // Same atomic Redis limiter script as the reference RedisConfig.
    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(
                    "local key=KEYS[1];local count=tonumber(ARGV[1]);local"
                        + " time=tonumber(ARGV[2]);local current=redis.call('get',key);if current"
                        + " and tonumber(current)>count then return tonumber(current)"
                        + " end;current=redis.call('incr',key);if tonumber(current)==1 then"
                        + " redis.call('expire',key,time) end;return tonumber(current)",
                    Long.class);

    @Before("@annotation(limiter)")
    public void doBefore(JoinPoint point, RateLimiter limiter) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();
        String identity =
                limiter.limitType() == LimitType.IP
                        ? request.getRemoteAddr()
                        : limiter.limitType() == LimitType.USER_ID
                                ? SecurityUtils.getUserId().toString()
                                : "global";
        MethodSignature method = (MethodSignature) point.getSignature();
        String key =
                limiter.key()
                        + identity
                        + "-"
                        + method.getMethod().getDeclaringClass().getName()
                        + "-"
                        + method.getName();
        Long count;
        try {
            count =
                    redis.execute(
                            SCRIPT,
                            Collections.singletonList(key),
                            String.valueOf(limiter.count()),
                            String.valueOf(limiter.time()));
        } catch (Exception e) {
            throw new ServiceException(500, "The login service is temporarily unavailable.");
        }
        if (count == null || count > limiter.count())
            throw new ServiceException(429, "Too many requests. Please try again later.");
    }
}
