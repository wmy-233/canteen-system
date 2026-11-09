package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.CartAddDTO;
import com.canteen.entity.CartItem;
import com.canteen.entity.Dish;
import com.canteen.mapper.CartItemMapper;
import com.canteen.mapper.DishMapper;
import com.canteen.service.CartService;
import com.canteen.vo.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 购物车实现（FR-STU-03）：DB 存储刷新不丢；同菜品重复加购累加数量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final DishMapper dishMapper;

    @Override
    public List<CartVO> listMyCart() {
        return cartItemMapper.selectCartList(SecurityUtil.getUserId());
    }

    @Override
    public void add(CartAddDTO dto) {
        Long userId = SecurityUtil.getUserId();
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null || dish.getStatus() == 0) {
            throw new BizException("菜品不存在或已下架");
        }
        Long current = cartItemMapper.selectQuantityByUserAndDish(userId, dto.getDishId());
        if (current == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setDishId(dto.getDishId());
            item.setQuantity(dto.getQuantity());
            cartItemMapper.insert(item);
        } else {
            long total = current + dto.getQuantity();
            if (dish.getDailyStock() > 0 && total > dish.getDailyStock()) {
                throw new BizException("超出今日限量，当前余量 " + dish.getDailyStock());
            }
            if (total > 99) {
                throw new BizException("单菜品数量过大");
            }
            cartItemMapper.updateQuantity(userId, dto.getDishId(), (int) total);
        }
        log.info("加购: userId={}, dishId={}, quantity={}", userId, dto.getDishId(), dto.getQuantity());
    }

    @Override
    public void updateQuantity(Long dishId, Integer quantity) {
        Long userId = SecurityUtil.getUserId();
        int rows = cartItemMapper.updateQuantity(userId, dishId, quantity);
        if (rows == 0) {
            throw new BizException("购物车中没有该菜品");
        }
    }

    @Override
    public void delete(Long itemId) {
        int rows = cartItemMapper.deleteByIdAndUser(itemId, SecurityUtil.getUserId());
        if (rows == 0) {
            throw new BizException("购物项不存在");
        }
    }

    @Override
    public void clear() {
        cartItemMapper.deleteByUserId(SecurityUtil.getUserId());
    }
}
