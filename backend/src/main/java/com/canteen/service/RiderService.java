package com.canteen.service;

import com.canteen.common.PageResult;
import com.canteen.dto.RiderAssignDTO;
import com.canteen.dto.RiderSaveDTO;
import com.canteen.dto.RiderUpdateDTO;
import com.canteen.vo.OrderVO;
import com.canteen.vo.RiderVO;

/**
 * 骑手管理业务（标注：管理端负责骑手的功能，接口全部挂 /api/admin/rider/**）
 */
public interface RiderService {

    PageResult<RiderVO> page(String keyword, Integer pageNum, Integer pageSize);

    /** 新增骑手：建登录账号（RIDER 角色）+ 骑手档案 */
    void create(RiderSaveDTO dto);

    /** 编辑姓名/手机号/停用启用（停用即禁登录并不可再接单） */
    void update(Long riderId, RiderUpdateDTO dto);

    /** 待分配配送单列表 */
    PageResult<OrderVO> pendingOrders(Integer pageNum, Integer pageSize);

    /** 指派骑手（校验骑手在岗） */
    void assign(RiderAssignDTO dto);

    /** 标记送达（1配送中 -> 2已送达，订单同步已完成） */
    void deliver(Long orderId);
}
