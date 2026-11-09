package com.canteen.mapper;

import com.canteen.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细 Mapper（菜名/价格快照写入、统计聚合共用）
 */
@Mapper
public interface OrderItemMapper {

    int insertBatch(@Param("list") List<OrderItem> items);

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
