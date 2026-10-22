package com.upself.web.config;

import com.upself.common.core.domain.entity.SysUser;
import com.upself.common.utils.PasswordUtils;
import com.upself.system.mapper.SysUserMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LocalAccountInitializer implements ApplicationRunner {
    @Autowired private SysUserMapper users;
    @Autowired private Environment env;

    @Transactional
    public void run(ApplicationArguments args) {
        if (!env.getProperty("bootstrap.enabled", Boolean.class, false) || users.count() != 0)
            return;
        String[] names = {"maintenance", "student01", "mentor01", "admin01"};
        long[] roles = {1, 10, 20, 30};
        for (int i = 0; i < names.length; i++) {
            String pass = env.getRequiredProperty("bootstrap.passwords." + names[i]);
            if (pass.length() < 10)
                throw new IllegalStateException("The initial password is too short.");
            SysUser u = new SysUser();
            u.setUserName(names[i]);
            u.setNickName(names[i]);
            u.setPassword(PasswordUtils.encode(pass));
            u.setRoleId(roles[i]);
            users.insert(u);
            if (i == 0 && u.getId() != 1L)
                throw new IllegalStateException("The maintenance account ID must be 1.");
        }
    }
}
