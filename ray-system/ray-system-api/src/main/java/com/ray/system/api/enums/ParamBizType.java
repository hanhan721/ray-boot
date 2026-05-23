package com.ray.system.api.enums;

import lombok.Getter;

/**
 * 系统参数业务分类枚举
 */
@Getter
public enum ParamBizType {

    SYSTEM(0, "系统"),
    USER(1, "用户管理"),
    ORDER(2, "订单管理"),
    PAYMENT(3, "支付"),
    NOTICE(4, "通知"),
    OTHER(99, "其他");

    private final Integer code;
    private final String name;

    ParamBizType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ParamBizType of(Integer code) {
        for (ParamBizType type : ParamBizType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return SYSTEM;
    }
}
