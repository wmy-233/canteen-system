package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息：type 0系统 1订单 2入驻 3投诉 4配送
 */
@Data
public class SysMessage {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Integer type;
    private Integer isRead;
    private LocalDateTime createTime;
}
