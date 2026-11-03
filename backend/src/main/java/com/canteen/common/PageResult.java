package com.canteen.common;

import lombok.Data;

import java.util.List;

/**
 * 分页返回体（配合 PageHelper 使用）
 */
@Data
public class PageResult<T> {

    private Long total;      // 总条数
    private Integer pageNum; // 当前页
    private Integer pageSize;// 每页条数
    private List<T> list;    // 数据列表
}
