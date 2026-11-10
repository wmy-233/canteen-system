package com.canteen.common;

import com.canteen.entity.SysMessage;
import com.canteen.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内消息发送组件：业务事件 → 铃铛消息的统一出口
 * （type: 0系统 1订单 2入驻 3投诉 4配送）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

    public static final int TYPE_SYSTEM = 0;
    public static final int TYPE_ORDER = 1;
    public static final int TYPE_AUDIT = 2;
    public static final int TYPE_COMPLAINT = 3;
    public static final int TYPE_DELIVERY = 4;

    private final SysMessageMapper sysMessageMapper;

    /** 发给一个人（吞异常：消息失败不影响业务事务） */
    public void send(Long userId, String title, String content, int type) {
        try {
            SysMessage m = new SysMessage();
            m.setUserId(userId);
            m.setTitle(title);
            m.setContent(content);
            m.setType(type);
            sysMessageMapper.insert(m);
        } catch (Exception e) {
            log.warn("站内消息发送失败(userId={}): {}", userId, e.getMessage());
        }
    }

    /** 发给某角色全部启用账号 */
    public void sendToRole(String roleCode, String title, String content, int type) {
        try {
            int n = sysMessageMapper.insertForRole(roleCode, title, content, type);
            log.info("站内消息群发: role={}, 数量={}, title={}", roleCode, n, title);
        } catch (Exception e) {
            log.warn("站内消息群发失败(role={}): {}", roleCode, e.getMessage());
        }
    }
}
