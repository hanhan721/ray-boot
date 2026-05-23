package com.ray.common.core.domain;

import com.ray.common.core.constant.ResultCode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @param <T> 数据类型
 */
@Data
@Accessors(chain = true)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<T>()
                .setCode(ResultCode.SUCCESS.getCode())
                .setMsg(ResultCode.SUCCESS.getMsg())
                .setData(data);
    }

    public static <T> R<T> ok(T data, String msg) {
        return new R<T>()
                .setCode(ResultCode.SUCCESS.getCode())
                .setMsg(msg)
                .setData(data);
    }

    public static <T> R<T> fail() {
        return fail(ResultCode.BUSINESS_ERROR);
    }

    public static <T> R<T> fail(String msg) {
        return new R<T>()
                .setCode(ResultCode.BUSINESS_ERROR.getCode())
                .setMsg(msg);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<T>()
                .setCode(resultCode.getCode())
                .setMsg(resultCode.getMsg());
    }

    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return new R<T>()
                .setCode(resultCode.getCode())
                .setMsg(msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<T>()
                .setCode(code)
                .setMsg(msg);
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
