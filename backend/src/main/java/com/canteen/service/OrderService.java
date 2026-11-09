package com.canteen.service;

import com.canteen.common.PageResult;
import com.canteen.dto.OrderCreateDTO;
import com.canteen.vo.OrderCreateVO;
import com.canteen.vo.OrderVO;

/**
 * 订单业务：下单主链（FR-STU-04）、防超卖（FR-STU-05）、订单流转（FR-MER-03）
 */
public interface OrderService {

    /**
     * ★下单主链（A4）：校验→算价→选券→Redis 预减→分布式锁→事务建单（快照+取餐码+扣款+流水+券核销+销量+热销榜）
     */
    OrderCreateVO create(OrderCreateDTO dto);

    /** 学生端我的订单（按状态筛选） */
    PageResult<OrderVO> myOrders(Integer status, Integer pageNum, Integer pageSize);

    /** 订单详情（学生校验本人） */
    OrderVO detail(Long orderId);

    /** 学生取消未完成订单（退回钱包+退回用券） */
    void cancel(Long orderId);

    /** 商家端出餐看板：本档口订单队列（status null=待处理优先） */
    PageResult<OrderVO> ordersForMerchant(Long merchantUserId, Integer status, Integer pageNum, Integer pageSize);

    /** 商家核销：0待处理 -> 1已完成（仅自取单，配送单走管理员骑手流转） */
    void verify(Long merchantUserId, Long orderId);
}
