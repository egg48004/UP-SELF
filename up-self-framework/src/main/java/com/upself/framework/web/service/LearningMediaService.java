package com.upself.framework.web.service;

import com.alibaba.fastjson2.*;
import com.upself.biz.learning.domain.LearningResource;
import com.upself.biz.learning.service.*;
import com.upself.common.core.domain.model.LoginUser;
import com.upself.common.exception.ServiceException;
import com.upself.common.utils.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class LearningMediaService {
    @Autowired private TokenService tokens;
    @Autowired private ILearningService learning;
    @Autowired private StringRedisTemplate redis;
    @Autowired private Messages messages;

    public Map<String, Object> issue(Long resourceId) {
        LoginUser actor = SecurityUtils.getLoginUser();
        learning.readMedia(resourceId, actor);
        String ticket = UUID.randomUUID().toString().replace("-", "");
        JSONObject data = new JSONObject();
        data.put("resourceId", resourceId);
        data.put("session", actor.getToken());
        data.put("channel", actor.getChannel());
        redis.opsForValue()
                .set("upself:media:" + ticket, data.toJSONString(), 30, TimeUnit.MINUTES);
        Map<String, Object> result = new HashMap<>();
        String prefix =
                "student".equals(actor.getChannel())
                        ? "/client/learning/media/"
                        : "/learning/media/";
        result.put("url", prefix + ticket);
        result.put("expiresIn", 1800);
        return result;
    }

    public LearningResource read(String ticket, String channel) {
        if (!ticket.matches("[a-f0-9]{32}")) throw denied();
        String raw = redis.opsForValue().get("upself:media:" + ticket);
        if (raw == null) throw denied();
        JSONObject data = JSON.parseObject(raw);
        if (!channel.equals(data.getString("channel"))) throw denied();
        LoginUser actor = tokens.getLoginUserBySession(data.getString("session"), channel);
        if (actor == null) throw denied();
        return learning.readMedia(data.getLong("resourceId"), actor);
    }

    private ServiceException denied() {
        return new ServiceException(403, messages.get("media.expired"));
    }
}
