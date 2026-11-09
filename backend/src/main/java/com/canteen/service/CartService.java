package com.canteen.service;

import com.canteen.vo.CartVO;

import java.util.List;

/**
 * 购物车业务（FR-STU-03：同一档口内加购/改量/删除，跨档口下单提示拆单）
 */
public interface CartService {

    List<CartVO> listMyCart();

    void add(com.canteen.dto.CartAddDTO dto);

    void updateQuantity(Long dishId, Integer quantity);

    void delete(Long itemId);

    void clear();
}
