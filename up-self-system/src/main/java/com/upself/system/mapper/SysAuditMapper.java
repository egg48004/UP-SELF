package com.upself.system.mapper;

import org.apache.ibatis.annotations.Param;

public interface SysAuditMapper {
    int insert(
            @Param("actor") Long actor,
            @Param("action") String action,
            @Param("target") String target,
            @Param("result") String result);
}
