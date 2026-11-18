package com.canteen.task;

import com.canteen.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 投诉超时自动升级：商家超过 N 小时未回应，工单自动升级给管理员。
 * 阈值可通过 application.yml 的 complaint.escalate-hours 配置（默认 24 小时）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplaintTimeoutTask {

    private final ComplaintService complaintService;

    @Value("${complaint.escalate-hours:24}")
    private int escalateHours;

    /** 每 10 分钟扫描一次，启动 1 分钟后先跑一次 */
    @Scheduled(initialDelay = 60_000, fixedDelay = 600_000)
    public void scanOverdue() {
        try {
            complaintService.escalateOverdue(escalateHours);
        } catch (Exception e) {
            log.warn("投诉超时扫描失败: {}", e.getMessage());
        }
    }
}
