package com.upself.framework.security.filter;

import com.alibaba.fastjson2.JSON;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.framework.web.service.TokenService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.*;

public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final String channel;

    public JwtAuthenticationTokenFilter(TokenService tokens, String channel) {
        this.tokens = tokens;
        this.channel = channel;
    }

    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token != null) {
            try {
                LoginUser u = tokens.getLoginUser(token, channel);
                if (u != null) {
                    tokens.verifyToken(u);
                    SecurityContextHolder.getContext()
                            .setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            u, null, u.getAuthorities()));
                }
            } catch (Exception e) {
                logger.error("Session validation failed; access was not granted", e);
                response.setStatus(500);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter()
                        .write(
                                JSON.toJSONString(
                                        AjaxResult.error(
                                                500,
                                                "The login service is temporarily unavailable.")));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
