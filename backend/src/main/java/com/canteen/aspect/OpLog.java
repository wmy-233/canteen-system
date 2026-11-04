package com.canteen.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在需要留痕的关键业务方法上（登录/注册/下单/支付等）
 * 由 LogAspect 拦截并写入 logs/canteen.log，满足作业"关键操作日志留痕"要求
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 业务模块，如 用户/订单/支付 */
    String module();

    /** 操作名，如 登录/注册/下单 */
    String action() default "";
}
