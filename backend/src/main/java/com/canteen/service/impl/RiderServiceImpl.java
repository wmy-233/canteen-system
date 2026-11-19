package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.PageResult;
import com.canteen.dto.RiderAssignDTO;
import com.canteen.dto.RiderSaveDTO;
import com.canteen.dto.RiderUpdateDTO;
import com.canteen.entity.OrderInfo;
import com.canteen.entity.Rider;
import com.canteen.entity.User;
import com.canteen.mapper.OrderInfoMapper;
import com.canteen.mapper.RiderMapper;
import com.canteen.mapper.RoleMapper;
import com.canteen.mapper.UserMapper;
import com.canteen.mapper.WalletRecordMapper;
import com.canteen.service.RedisSupport;
import com.canteen.service.RiderService;
import com.canteen.vo.OrderVO;
import com.canteen.vo.RiderVO;
import com.canteen.entity.WalletRecord;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 骑手管理实现（A11：管理员"负责骑手"的功能集合）
 * 状态机：未分配(0) -> 配送中(1) -> 已送达(2)；不可逆、不可跳级；指派必须是在岗骑手
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {

    private final RiderMapper riderMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final com.canteen.common.MessageSender messageSender;
    private final RedisSupport redisSupport;
    private final WalletRecordMapper walletRecordMapper;

    @Override
    public PageResult<RiderVO> page(String keyword, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<RiderVO> list = riderMapper.selectPage(keyword);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RiderSaveDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BizException("账号已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getName());
        user.setPhone(dto.getPhone());
        user.setBalance(java.math.BigDecimal.ZERO);
        user.setStatus(1);
        userMapper.insert(user);

        Long roleId = roleMapper.selectIdByCode("RIDER");
        if (roleId == null) {
            throw new BizException("RIDER 角色未初始化，请先执行 sql/patch_rider.sql");
        }
        roleMapper.insertUserRole(user.getId(), roleId);

        Rider rider = new Rider();
        rider.setUserId(user.getId());
        rider.setName(dto.getName());
        rider.setPhone(dto.getPhone());
        rider.setStatus(1);
        riderMapper.insert(rider);
        log.info("新增骑手: username={}, riderId={}", dto.getUsername(), rider.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long riderId, RiderUpdateDTO dto) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw new BizException("配送员不存在");
        }
        Rider update = new Rider();
        update.setId(riderId);
        if (dto.getName() != null) {
            update.setName(dto.getName());
        }
        if (dto.getPhone() != null) {
            update.setPhone(dto.getPhone());
        }
        update.setStatus(dto.getStatus());
        riderMapper.updateSelective(update);

        // 停用/启用同步登录账号状态（停用即禁登录+不可接单）
        if (rider.getStatus() != null && rider.getStatus() != dto.getStatus()) {
            userMapper.updateStatus(rider.getUserId(), dto.getStatus());
        }
        log.info("骑手信息更新: riderId={}, status={}", riderId, dto.getStatus());
    }

    @Override
    public PageResult<OrderVO> pendingOrders(Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<OrderVO> list = orderInfoMapper.selectPageDelivery(0);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(RiderAssignDTO dto) {
        Rider rider = riderMapper.selectById(dto.getRiderId());
        if (rider == null) {
            throw new BizException("配送员不存在");
        }
        if (rider.getStatus() == null || rider.getStatus() != 1) {
            throw new BizException("配送员已停用，不可指派");
        }
        // 条件更新：仅"未分配"可指派（并发/重复指派安全）
        int rows = orderInfoMapper.assignRider(dto.getOrderId(), dto.getRiderId());
        if (rows == 0) {
            checkOrderExists(dto.getOrderId());
            throw new BizException("该配送单当前不可指派");
        }
        // 站内消息：通知学生配送安排
        com.canteen.entity.OrderInfo order = orderInfoMapper.selectById(dto.getOrderId());
        messageSender.send(order.getUserId(), "你的外卖开始配送 🛵",
                "订单 " + order.getOrderNo() + " 已安排校园配送员「" + rider.getName() + "」配送"
                        + (rider.getPhone() != null ? "，联系方式 " + rider.getPhone() : ""),
                com.canteen.common.MessageSender.TYPE_DELIVERY);
        log.info("配送单指派: orderId={}, riderId={}", dto.getOrderId(), dto.getRiderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long orderId) {
        int rows = orderInfoMapper.markDelivered(orderId);
        if (rows == 0) {
            throw new BizException("仅配送中的订单可标记送达");
        }
        // 与骑手端送达共用同一套担保结算逻辑，避免管理员送达漏结算跑腿费
        com.canteen.entity.OrderInfo order = orderInfoMapper.selectById(orderId);
        settleDeliveryFee(order, order.getRiderId());
        // 站内消息：通知学生已送达
        messageSender.send(order.getUserId(), "配送已完成 🎉",
                "订单 " + order.getOrderNo() + " 已送达，祝用餐愉快！如遇问题可对该订单发起投诉",
                com.canteen.common.MessageSender.TYPE_DELIVERY);
        log.info("配送送达: orderId={}", orderId);
    }

    // ---------------- 骑手端自主接单 ----------------

    @Override
    public RiderVO me(Long userId) {
        Rider rider = riderMapper.selectByUserId(userId);
        if (rider == null) {
            throw new BizException("当前账号未绑定校园配送员档案，请联系管理员");
        }
        RiderVO vo = new RiderVO();
        vo.setId(rider.getId());
        vo.setUserId(rider.getUserId());
        vo.setName(rider.getName());
        vo.setPhone(rider.getPhone());
        vo.setStatus(rider.getStatus());
        vo.setCreateTime(rider.getCreateTime());
        User user = userMapper.selectById(userId);
        if (user != null) vo.setUsername(user.getUsername());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grab(Long riderId, Long orderId) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null || !Integer.valueOf(1).equals(rider.getStatus())) {
            throw new BizException("配送员不在岗，无法接单");
        }
        // 高并发接单：Redisson 分布式锁（订单维度）+ 数据库条件更新双保险。
        // Redis 不可用时 withLock 自动降级直跑，条件更新依旧保证只有一人接到，零超卖。
        redisSupport.withLock("lock:delivery:grab:" + orderId, 8, () -> {
            int rows = orderInfoMapper.assignRider(orderId, riderId);
            if (rows == 0) {
                checkOrderExists(orderId);
                throw new BizException("手慢了，该单已被其他配送员接走");
            }
            OrderInfo order = orderInfoMapper.selectById(orderId);
            messageSender.send(order.getUserId(), "你的外卖开始配送 🛵",
                    "订单 " + order.getOrderNo() + " 已被校园配送员「" + rider.getName() + "」接单，正在为你配送"
                            + (rider.getPhone() != null ? "，联系方式 " + rider.getPhone() : ""),
                    com.canteen.common.MessageSender.TYPE_DELIVERY);
            log.info("校园配送员接单成功: orderId={}, riderId={}", orderId, riderId);
            return null;
        });
    }

    @Override
    public List<OrderVO> myOrders(Long riderId) {
        return orderInfoMapper.selectMyDelivery(riderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverMine(Long riderId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !riderId.equals(order.getRiderId())) {
            throw new BizException("只能完成自己接的配送单");
        }
        int rows = orderInfoMapper.markDelivered(orderId);
        if (rows == 0) {
            throw new BizException("仅配送中的订单可标记送达");
        }
        // 担保交易结算（管理员/骑手端统一口径）
        settleDeliveryFee(order, riderId);
        messageSender.send(order.getUserId(), "配送已完成 🎉",
                "订单 " + order.getOrderNo() + " 已送达，祝用餐愉快！如遇问题可对该订单发起投诉",
                com.canteen.common.MessageSender.TYPE_DELIVERY);
        log.info("校园配送员完成配送: orderId={}, riderId={}, 跑腿费结算={}",
                orderId, riderId, order.getDeliveryFee());
    }

    /** 跑腿费结算：仅送达成功后调用；按 riderId 查档案，管理员派单与骑手自主送达共用 */
    private void settleDeliveryFee(OrderInfo order, Long riderId) {
        BigDecimal fee = order.getDeliveryFee() == null ? BigDecimal.ZERO : order.getDeliveryFee();
        if (riderId == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            return;
        }
        if (userMapper.addBalance(rider.getUserId(), fee) == 0) {
            throw new BizException("配送员账号不存在，跑腿费结算失败");
        }
        WalletRecord income = new WalletRecord();
        income.setUserId(rider.getUserId());
        income.setType(4); // 4跑腿收入
        income.setAmount(fee);
        income.setRefOrderNo(order.getOrderNo());
        walletRecordMapper.insert(income);
        log.info("跑腿费结算: orderNo={}, riderUserId={}, fee={}", order.getOrderNo(), rider.getUserId(), fee);
    }

    private void checkOrderExists(Long orderId) {
        if (orderInfoMapper.selectById(orderId) == null) {
            throw new BizException("订单不存在");
        }
    }
}
