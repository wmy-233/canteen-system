package com.canteen.mapper;

import com.canteen.entity.OrderInfo;
import com.canteen.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单主表 Mapper
 */
@Mapper
public interface OrderInfoMapper {

    int insert(OrderInfo order);

    OrderInfo selectById(@Param("id") Long id);

    /** 学生端：我的订单（status null 全部） */
    List<OrderVO> selectPageByUser(@Param("userId") Long userId, @Param("status") Integer status);

    /** 商家端：档口集合（本档口实时队列 + 历史，status null=全部） */
    List<OrderVO> selectPageByStallIds(@Param("stallIds") List<Long> stallIds, @Param("status") Integer status);

    /** 单条 VO（详情页，带 user/stall/rider 信息） */
    OrderVO selectVoById(@Param("id") Long id);

    /** 管理端骑手侧：按配送状态查配送单（0待分配 / 1配送中 / null 全部） */
    List<OrderVO> selectPageDelivery(@Param("deliveryStatus") Integer deliveryStatus);

    /** 当日该档口已用取餐码（保证当日档口内唯一，O-4 口径） */
    List<String> selectTodayPickupCodes(@Param("stallId") Long stallId);

    /**
     * 状态机条件流转（0待处理 1已完成 2已取消）：影响行数=0 即非法流转
     */
    int updateStatusConditional(@Param("id") Long id, @Param("oldStatus") Integer oldStatus, @Param("newStatus") Integer newStatus);

    /** 指派骑手：0未分配 -> 1配送中 */
    int assignRider(@Param("id") Long id, @Param("riderId") Long riderId);

    /** 标记送达：1配送中 -> 2已送达 且订单置已完成；返回 0 即非法流转 */
    int markDelivered(@Param("id") Long id);
}
