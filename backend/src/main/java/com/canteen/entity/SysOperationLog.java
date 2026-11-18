package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** AOP 操作日志：落库留痕，管理端可查询（可追溯性） */
@Data
public class SysOperationLog {
    private Long id;
    private String operator;
    private String module;
    private String action;
    private String method;
    private String params;
    private Integer costMs;
    private Integer success;
    private String error;
    private LocalDateTime createTime;
}
