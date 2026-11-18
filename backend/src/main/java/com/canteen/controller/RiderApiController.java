package com.canteen.controller;

import com.canteen.common.BizException;
import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.entity.Rider;
import com.canteen.mapper.OrderInfoMapper;
import com.canteen.mapper.RiderMapper;
import com.canteen.service.RiderService;
import com.canteen.vo.OrderVO;
import com.canteen.vo.RiderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 骑手端：自主抢单模式（/api/rider/** → ROLE_RIDER）
 *
 * 设计取舍：纯管理员派单会让管理员成为瓶颈、骑手缺乏自主性；
 * 纯抢单又可能出现冷门单无人接。因此采用「骑手抢单为主 + 管理员兜底派单」混合模式。
 * 并发安全：Redisson 分布式锁（订单维度）+ 条件更新 SQL（WHERE delivery_status=0 AND status=0）双保险，
 * 多个配送员同时接同一单时只有一人成功；Redis 不可用时自动降级，条件更新依旧零冲突。
 */
@Tag(name = "校园配送端-接单配送（勤工助学）")
@RestController
@RequestMapping("/api/rider")
@RequiredArgsConstructor
public class RiderApiController {

    private final RiderService riderService;
    private final RiderMapper riderMapper;
    private final OrderInfoMapper orderInfoMapper;

    private Long currentRiderId() {
        Long userId = SecurityUtil.getUserId();
        Rider rider = riderMapper.selectByUserId(userId);
        if (rider == null) {
            throw new BizException("当前账号未绑定校园配送员档案");
        }
        return rider.getId();
    }

    @Operation(summary = "当前校园配送员档案")
    @GetMapping("/me")
    public Result<RiderVO> me() {
        return Result.success(riderService.me(SecurityUtil.getUserId()));
    }

    @Operation(summary = "抢单大厅：待分配的配送单")
    @GetMapping("/orders/pending")
    public Result<List<OrderVO>> pending() {
        return Result.success(orderInfoMapper.selectPageDelivery(0));
    }

    @Operation(summary = "我接的配送单")
    @GetMapping("/orders/mine")
    public Result<List<OrderVO>> mine() {
        return Result.success(riderService.myOrders(currentRiderId()));
    }

    @Operation(summary = "抢单（并发只有一人成功）")
    @PostMapping("/orders/{orderId}/grab")
    public Result<Void> grab(@PathVariable Long orderId) {
        riderService.grab(currentRiderId(), orderId);
        return Result.success();
    }

    @Operation(summary = "我接的单标记送达")
    @PostMapping("/orders/{orderId}/deliver")
    public Result<Void> deliver(@PathVariable Long orderId) {
        riderService.deliverMine(currentRiderId(), orderId);
        return Result.success();
    }
}
