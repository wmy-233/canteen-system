package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.ReviewCreateDTO;
import com.canteen.entity.OrderInfo;
import com.canteen.entity.OrderItem;
import com.canteen.entity.Review;
import com.canteen.mapper.OrderInfoMapper;
import com.canteen.mapper.OrderItemMapper;
import com.canteen.mapper.ReviewMapper;
import com.canteen.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价实现（FR-STU-09）：仅本人已完成订单；同一订单同一菜品只能评一次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public void create(ReviewCreateDTO dto) {
        Long userId = SecurityUtil.getUserId();

        OrderInfo order = orderInfoMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("只能评价自己的订单");
        }
        if (order.getStatus() != 1) {
            throw new BizException("仅" + (order.getDeliveryType() != null && order.getDeliveryType() == 1
                    ? "已送达" : "已完成") + "的订单可以评价");
        }
        List<OrderItem> items = orderItemMapper.selectByOrderId(dto.getOrderId());
        boolean belongs = items.stream().anyMatch(item -> item.getDishId().equals(dto.getDishId()));
        if (!belongs) {
            throw new BizException("该菜品不在此订单中");
        }
        if (reviewMapper.countByOrderAndDish(userId, dto.getOrderId(), dto.getDishId()) > 0) {
            throw new BizException("该菜品已评价过");
        }
        Review review = new Review();
        review.setOrderId(dto.getOrderId());
        review.setUserId(userId);
        review.setDishId(dto.getDishId());
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        reviewMapper.insert(review);
        log.info("评价成功: userId={}, orderId={}, score={}", userId, dto.getOrderId(), dto.getScore());
    }

    @Override
    public List<Review> listByOrder(Long orderId) {
        return reviewMapper.selectByOrderId(orderId);
    }
}
