package com.canteen.common;

import lombok.Data;

/**
 * 统一返回体：所有接口返回此结构
 */
@Data
public class Result<T> {

    private Integer code;      // 200 成功, 其他失败
    private String message;    // 提示信息
    private T data;            // 业务数据

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
