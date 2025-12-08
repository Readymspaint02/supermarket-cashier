package com.zmj.gbs_commerce_system.utils;

import lombok.Data;

import java.util.Map;

@Data
public class PageParams {
    private Integer pageNum;
    private Integer pageSize;
    private Map<String ,Object> queryParams;
}
