package com.upself.framework.config;

import com.alibaba.fastjson2.JSON;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.framework.security.filter.*;
import com.upself.framework.web.service.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider(
      UserDetailsServiceImpl details, BCryptPasswordEncoder encoder) {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(details);
    p.setPasswordEncoder(encoder);
    return p;
  }

  private static void common(
      HttpSecurity http, TokenService tokens, StringRedisTemplate redis, String channel)
      throws Exception {
    http.csrf()
        .disable()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(
            (req, res, e) -> {
              res.setStatus(500);
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter()
                  .write(
                      JSON.toJSONString(
                          AjaxResult.error(
                              401, "Please sign in. Your session may have" + " expired.")));
            })
        .accessDeniedHandler(
            (req, res, e) -> {
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter()
                  .write(
                      JSON.toJSONString(
                          AjaxResult.error(
                              403, "You do not have permission to perform" + " this action.")));
            });
    JwtAuthenticationTokenFilter jwt = new JwtAuthenticationTokenFilter(tokens, channel);
    http.addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);

    http.logout()
        .logoutUrl(channel.equals("student") ? "/client/logout" : "/logout")
        .logoutSuccessHandler(
            (req, res, a) -> {
              String raw = req.getHeader("Authorization");
              if (raw != null) {
                if (raw.startsWith("Bearer ")) raw = raw.substring(7);
                LoginUser logoutUser = tokens.getLoginUser(raw, channel);
                if (logoutUser != null) tokens.delLoginUser(logoutUser);
              }
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter().write(JSON.toJSONString(AjaxResult.success()));
            });
  }

  @Configuration
  @Order(1)
  public static class ClientSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired private TokenService tokens;
    @Autowired private StringRedisTemplate redis;

    protected void configure(HttpSecurity h) throws Exception {
      h.antMatcher("/client/**");
      common(h, tokens, redis, "student");
      h.authorizeRequests()
          .antMatchers("/client/login", "/client/learning/media/*")
          .permitAll()
          .anyRequest()
          .hasAuthority("student");
    }
  }

  @Configuration
  public static class AdminSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired private TokenService tokens;
    @Autowired private StringRedisTemplate redis;
    @Autowired private DaoAuthenticationProvider provider;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
    }

    protected void configure(AuthenticationManagerBuilder a) {
      a.authenticationProvider(provider);
    }

    protected void configure(HttpSecurity h) throws Exception {
      common(h, tokens, redis, "admin");
      h.authorizeRequests()
          .antMatchers(org.springframework.http.HttpMethod.GET, "/accessibility/defaults")
          .permitAll()
          .antMatchers(
              "/",
              "/index.html",
              "/app.js",
              "/style.css",
              "/favicon.ico",
              "/empLogin",
              "/empLogin/totp",
              "/error",
              "/learning/media/*")
          .permitAll()
          .anyRequest()
          .hasAuthority("admin");
    }
  }
}
