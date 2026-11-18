package com.canteen.aspect;

import com.canteen.entity.SysOperationLog;
import com.canteen.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @OpLog 操作日志切面：
 * 1) 写文件（logback）——排障用
 * 2) 落库 sys_operation_log——管理端可查询，体现系统可追溯性
 * 日志失败一律吞异常，绝不影响业务主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperationLogMapper operationLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        String operator = currentUsername();
        String method = joinPoint.getSignature().toShortString();
        String args = abbreviate(Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            log.info("[操作日志] 模块={}, 操作={}, 操作人={}, 方法={}, 参数={}, 耗时={}ms, 结果=成功",
                    opLog.module(), opLog.action(), operator, method, args, System.currentTimeMillis() - start);
            save(opLog, operator, method, args, System.currentTimeMillis() - start, true, "");
            return result;
        } catch (Throwable e) {
            log.error("[操作日志] 模块={}, 操作={}, 操作人={}, 方法={}, 参数={}, 耗时={}ms, 结果=失败({})",
                    opLog.module(), opLog.action(), operator, method, args, System.currentTimeMillis() - start, e.getMessage());
            save(opLog, operator, method, args, System.currentTimeMillis() - start, false, e.getMessage());
            throw e;
        }
    }

    private void save(OpLog opLog, String operator, String method, String args,
                      long cost, boolean success, String error) {
        try {
            SysOperationLog record = new SysOperationLog();
            record.setOperator(operator);
            record.setModule(opLog.module());
            record.setAction(opLog.action() == null ? "" : opLog.action());
            record.setMethod(method.length() > 200 ? method.substring(0, 200) : method);
            record.setParams(args);
            record.setCostMs((int) cost);
            record.setSuccess(success ? 1 : 0);
            record.setError(error == null ? "" : (error.length() > 300 ? error.substring(0, 300) : error));
            operationLogMapper.insert(record);
        } catch (Exception e) {
            log.warn("操作日志落库失败（不影响业务）: {}", e.getMessage());
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.canteen.security.LoginUser) {
            return ((com.canteen.security.LoginUser) authentication.getPrincipal()).getUsername();
        }
        return "匿名";
    }

    private String abbreviate(String args) {
        return args.length() > 300 ? args.substring(0, 300) + "...(截断)" : args;
    }
}
