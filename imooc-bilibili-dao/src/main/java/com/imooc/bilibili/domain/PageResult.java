package com.imooc.bilibili.domain;

import java.util.List;

/**
 * 专门用于封装分页查询的结果，总记录数、当前分页的数据
 * 相当于是个map
 *
 * @author luf
 * @date 2022/03/07 13:08
 **/
public class PageResult<T> {
    private Integer total;
    private List<T> list;

    public PageResult(Integer total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
