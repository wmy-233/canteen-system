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

    /**
     * 由 PageHelper 分页后的 List 构建（Page 是其子类，含总数）
     */
    public static <T> PageResult<T> build(List<T> list, Integer pageNum, Integer pageSize) {
        PageResult<T> page = new PageResult<>();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        if (list instanceof com.github.pagehelper.Page) {
            page.setTotal(((com.github.pagehelper.Page<?>) list).getTotal());
        } else {
            page.setTotal((long) list.size());
        }
        page.setList(list);
        return page;
    }
}
