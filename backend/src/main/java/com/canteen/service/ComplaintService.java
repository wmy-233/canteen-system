package com.canteen.service;

import com.canteen.common.BizException;
import com.canteen.common.MessageSender;
import com.canteen.common.SecurityUtil;
import com.canteen.entity.Complaint;
import com.canteen.entity.OrderInfo;
import com.canteen.entity.Stall;
import com.canteen.mapper.ComplaintMapper;
import com.canteen.mapper.OrderInfoMapper;
import com.canteen.mapper.StallMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 投诉工单业务：学生发起 → 商家回应 → 管理员裁定（成立可选退款）
 *
 * 状态机：0待处理 →(商家回应)→ 1已回应 →(管理员裁定)→ 2已裁定
 * 权限：学生只能看/发自己的；商家只能回应本档口；裁定只归管理员。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintMapper complaintMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final StallMapper stallMapper;
    private final com.canteen.mapper.UserMapper userMapper;
    private final com.canteen.mapper.WalletRecordMapper walletRecordMapper;
    private final MessageSender messages;

    // ---------------- 学生 ----------------

    /** 学生发起投诉：必须本人订单；每笔订单限 1 个未完结投诉 */
    @Transactional(rollbackFor = Exception.class)
    public void create(Long orderId, Integer type, String content) {
        Long userId = SecurityUtil.getUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("只能对本人的订单发起投诉");
        }
        if (order.getStatus() == null || order.getStatus() == 2) {
            throw new BizException("已取消的订单无需投诉");
        }
        if (type == null || type < 1 || type > 4) {
            throw new BizException("投诉类型不合法");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("请填写投诉内容");
        }
        // 防重复投诉：同订单存在待处理/已回应的单子则不允许再发
        List<Map<String, Object>> mine = complaintMapper.selectMine(userId);
        boolean open = mine.stream().anyMatch(m ->
                orderId.equals(((Number) m.get("order_id")).longValue())
                        && !Integer.valueOf(2).equals(((Number) m.get("status")).intValue()));
        if (open) {
            throw new BizException("该订单已有处理中的投诉，请等待结论");
        }

        Complaint c = new Complaint();
        c.setOrderId(orderId);
        c.setStudentUserId(userId);
        c.setStallId(order.getStallId());
        c.setType(type);
        c.setContent(content.trim());
        complaintMapper.insert(c);

        Stall stall = stallMapper.selectById(order.getStallId());
        String stallName = stall != null && stall.getName() != null ? stall.getName() : ("档口#" + order.getStallId());
        // 通知商家 + 全体管理员
        if (stall != null && stall.getMerchantUserId() != null) {
            messages.send(stall.getMerchantUserId(), "收到新的投诉",
                    "订单 " + order.getOrderNo() + " 被投诉（" + typeName(type) + "）：" + content, MessageSender.TYPE_COMPLAINT);
        }
        messages.sendToRole("ADMIN", "收到新的投诉",
                "学生投诉 " + stallName + "（订单 " + order.getOrderNo() + "，" + typeName(type) + "），请前往投诉管理处理",
                MessageSender.TYPE_COMPLAINT);
        log.info("投诉发起: orderId={}, userId={}, type={}", orderId, userId, type);
    }

    /** 学生：我的投诉列表（带档口名/订单号） */
    public List<Map<String, Object>> listMine(Long userId) {
        return complaintMapper.selectMine(userId);
    }

    // ---------------- 商家 ----------------

    /** 商家：本店投诉列表（学员侧会传多个本人档口） */
    public List<Map<String, Object>> listForMerchant(List<Long> stallIds, Integer status) {
        if (stallIds == null || stallIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return complaintMapper.selectByStall(stallIds, status);
    }

    /** 商家回应投诉：仅本档口、且处于待处理状态 */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long complaintId, String reply) {
        Long merchantUserId = SecurityUtil.getUserId();
        Complaint c = complaintMapper.selectById(complaintId);
        mustOwnStall(merchantUserId, c);
        int rows = complaintMapper.updateReply(complaintId, reply == null ? "" : reply.trim());
        if (rows == 0) {
            throw new BizException("该投诉当前不可回应（可能已裁定）");
        }
        OrderInfo order = orderInfoMapper.selectById(c.getOrderId());
        messages.send(c.getStudentUserId(), "商家已回应你的投诉",
                "针对订单 " + (order != null ? order.getOrderNo() : "#" + c.getOrderId()) + " 的投诉，商家已提交回应说明，等待管理员裁定",
                MessageSender.TYPE_COMPLAINT);
        log.info("投诉回应: complaintId={}, merchantUserId={}", complaintId, merchantUserId);
    }

    // ---------------- 管理员 ----------------

    /**
     * 管理员裁定：su支持=成立（可选全额退款），不成立则直接结案。
     * 退款走钱包原路退回 + 退款流水；同时通知学生与商家。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long complaintId, boolean pass, String verdict, boolean doRefund) {
        Long adminId = SecurityUtil.getUserId();
        Complaint c = complaintMapper.selectById(complaintId);
        if (c == null) {
            throw new BizException("投诉不存在");
        }
        if (Integer.valueOf(2).equals(c.getStatus())) {
            throw new BizException("该投诉已裁定，不可重复处理");
        }
        BigDecimal refundAmount = BigDecimal.ZERO;
        OrderInfo order = orderInfoMapper.selectById(c.getOrderId());
        String orderNo = order != null ? order.getOrderNo() : ("#" + c.getOrderId());

        if (pass) {
            // 成立：默认全额退款（订单实付），退款已全部退款的单子（已取消）跳过
            if (doRefund && order != null && order.getStatus() != null && order.getStatus() != 2) {
                refundAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    int rows = userMapper.addBalance(c.getStudentUserId(), refundAmount);
                    if (rows == 0) {
                        throw new BizException("退款写入失败：投诉人账号不存在");
                    }
                    com.canteen.entity.WalletRecord rec = new com.canteen.entity.WalletRecord();
                    rec.setUserId(c.getStudentUserId());
                    rec.setType(3); // 退款
                    rec.setAmount(refundAmount);
                    rec.setRefOrderNo(orderNo);
                    walletRecordMapper.insert(rec);
                }
            }
        }

        int rows = complaintMapper.updateVerdict(complaintId,
                verdict == null ? "" : verdict.trim(), pass ? 1 : 0, refundAmount, adminId);
        if (rows == 0) {
            throw new BizException("裁定失败：投诉状态已变化");
        }

        if (pass) {
            messages.send(c.getStudentUserId(), "投诉裁定：成立",
                    "订单 " + orderNo + " 的投诉已成立，" + (refundAmount.compareTo(BigDecimal.ZERO) > 0
                            ? "退款 ¥" + refundAmount + " 已退回你的校园卡" : "管理员已要求商家整改"),
                    MessageSender.TYPE_COMPLAINT);
        } else {
            messages.send(c.getStudentUserId(), "投诉裁定：不成立",
                    "订单 " + orderNo + " 的投诉经核实不成立：" + (verdict == null ? "" : verdict),
                    MessageSender.TYPE_COMPLAINT);
        }
        Stall stall = stallMapper.selectById(c.getStallId());
        if (stall != null && stall.getMerchantUserId() != null) {
            messages.send(stall.getMerchantUserId(), pass ? "投诉裁定：成立" : "投诉裁定：不成立",
                    "订单 " + orderNo + " 的投诉已裁定为" + (pass ? "成立" : "不成立")
                            + (refundAmount.compareTo(BigDecimal.ZERO) > 0 ? "，顾客退款 ¥" + refundAmount : ""),
                    MessageSender.TYPE_COMPLAINT);
        }
        log.info("投诉裁定: complaintId={}, result={}, refund={}", complaintId, pass, refundAmount);
    }

    /** 数据权限：商家只能操作本档口的投诉 */
    private void mustOwnStall(Long merchantUserId, Complaint c) {
        if (c == null) {
            throw new BizException("投诉不存在");
        }
        List<Stall> stalls = stallMapper.selectByMerchantUserId(merchantUserId);
        boolean own = stalls.stream().anyMatch(s -> s.getId().equals(c.getStallId()));
        if (!own) {
            throw new BizException("只能处理本档口的投诉");
        }
    }

    public static String typeName(Integer t) {
        if (t == null) return "其他";
        switch (t) {
            case 1: return "菜品质量";
            case 2: return "漏发缺件";
            case 3: return "配送问题";
            case 4: return "服务态度";
            default: return "其他";
        }
    }
}
