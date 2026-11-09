package com.canteen.common;

/**
 * 业务异常：带友好提示的业务错误（前端弹提示用）
 * 全局异常处理器单独处理，不打印堆栈噪音
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
