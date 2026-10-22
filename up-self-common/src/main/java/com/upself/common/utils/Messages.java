package com.upself.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Messages {
    @Autowired private MessageSource source;

    public String get(String key, Object... args) {
        return source.getMessage(key, args, Locale.ENGLISH);
    }
}
