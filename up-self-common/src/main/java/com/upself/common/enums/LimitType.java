package com.upself.common.enums;

/**
 * 限流类型
 *
 * @author saas
 */
public enum LimitType {
    /** 默认策略全局限流 */
    DEFAULT,

    /** 根据请求者IP进行限流 */
    IP,

    /** 根据请求者用户id进行限流 */
    USER_ID
}
