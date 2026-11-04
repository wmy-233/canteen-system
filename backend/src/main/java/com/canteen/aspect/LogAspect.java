package com.canteen.aspect;

import com.canteen.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 操作日志切面（组员 C 维护）：凡标注 @OpLog 的方法自动记录
 * 输出到 logs/canteen.log（logback 按天滚动 + 错误单独文件），满足作业日志框架要求
 *
 * 记录内容：模块 / 操作 / 操作人 / 方法 / 参数(截断) / 耗时 / 成功或失败原因
 * 注意：DTO 的密码字段已加 @ToString.Exclude，不会明文进日志
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        String operator = currentUsername();
        String method = joinPoint.getSignature().toShortString();
        String args = abbreviate(Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            log.info("[操作日志] 模块={}, 操作={}, 操作人={}, 方法={}, 参数={}, 耗时={}ms, 结果=成功",
                    opLog.module(), opLog.action(), operator, method, args,
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("[操作日志] 模块={}, 操作={}, 操作人={}, 方法={}, 参数={}, 耗时={}ms, 结果=失败({})",
                    opLog.module(), opLog.action(), operator, method, args,
                    System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    /** 未登录场景（如注册）操作人显示"匿名" */
    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return ((LoginUser) authentication.getPrincipal()).getUsername();
        }
        return "匿名";
    }

    /** 参数过长截断，避免日志爆炸 */
    private String abbreviate(String args) {
        return args.length() > 300 ? args.substring(0, 300) + "...(截断)" : args;
    }
}
