package com.canteen.mapper;

import com.canteen.entity.User;
import com.canteen.vo.UserAdminVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户表 Mapper
 */
@Mapper
public interface UserMapper {

    /** 按用户名查询（登录时使用） */
    User selectByUsername(String username);

    /** 按ID查询 */
    User selectById(Long id);

    /** 新增用户（注册时使用） */
    int insert(User user);

    /** 统计用户总数（用于验证数据库连通） */
    long countAll();

    /** 管理端：用户列表（keyword 模糊搜索 账号/昵称/手机号） */
    List<UserAdminVO> selectPage(@Param("keyword") String keyword);

    /** 管理端：禁用/启用账号（骑手停用也会用到） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 扣款（下单）：余额不足影响行数=0，SQL 内联判断保证原子性 */
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 入账（充值/退款） */
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
