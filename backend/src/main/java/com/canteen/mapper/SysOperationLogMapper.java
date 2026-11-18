package com.canteen.mapper;

import com.canteen.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysOperationLogMapper {

    int insert(SysOperationLog log);

    /** 管理端日志查询：模块/操作人关键字 + 成功失败过滤 */
    List<SysOperationLog> selectPage(@Param("keyword") String keyword,
                                     @Param("module") String module,
                                     @Param("success") Integer success);

    /** 清空日志（管理端运维用） */
    int deleteAll();
}
