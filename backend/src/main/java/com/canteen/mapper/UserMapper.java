package com.canteen.mapper;

import com.canteen.entity.User;
import org.apache.ibatis.annotations.Mapper;

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
}
