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
import com.canteen.service.RiderService;
import com.canteen.vo.OrderVO;
import com.canteen.vo.RiderVO;
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
            throw new BizException("骑手不存在");
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
            throw new BizException("骑手不存在");
        }
        if (rider.getStatus() == null || rider.getStatus() != 1) {
            throw new BizException("骑手已停用，不可指派");
        }
        // 条件更新：仅"未分配"可指派（并发/重复指派安全）
        int rows = orderInfoMapper.assignRider(dto.getOrderId(), dto.getRiderId());
        if (rows == 0) {
            checkOrderExists(dto.getOrderId());
            throw new BizException("该配送单当前不可指派");
        }
        log.info("配送单指派: orderId={}, riderId={}", dto.getOrderId(), dto.getRiderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long orderId) {
        int rows = orderInfoMapper.markDelivered(orderId);
        if (rows == 0) {
            throw new BizException("仅配送中的订单可标记送达");
        }
        log.info("配送送达: orderId={}", orderId);
    }

    private void checkOrderExists(Long orderId) {
        if (orderInfoMapper.selectById(orderId) == null) {
            throw new BizException("订单不存在");
        }
    }
}
