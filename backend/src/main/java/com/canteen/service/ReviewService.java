package com.canteen.service;

import com.canteen.dto.ReviewCreateDTO;
import com.canteen.entity.Review;

import java.util.List;

/**
 * 评价业务（FR-STU-09）
 */
public interface ReviewService {

    void create(ReviewCreateDTO dto);

    List<Review> listByOrder(Long orderId);
}
