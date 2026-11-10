package com.canteen.mapper;

import com.canteen.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 站内消息 Mapper
 */
@Mapper
public interface SysMessageMapper {

    /** 发给一个人 */
    int insert(SysMessage message);

    /** 发给某角色下所有启用账号（用于"管理员全体收到新入驻申请"） */
    int insertForRole(@Param("roleCode") String roleCode,
                      @Param("title") String title,
                      @Param("content") String content,
                      @Param("type") Integer type);

    /** 未读数 */
    long countUnread(@Param("userId") Long userId);

    /** 消息分页（isRead null=全部） */
    List<SysMessage> selectPage(@Param("userId") Long userId,
                                @Param("isRead") Integer isRead,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    long countPage(@Param("userId") Long userId, @Param("isRead") Integer isRead);

    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);
}
