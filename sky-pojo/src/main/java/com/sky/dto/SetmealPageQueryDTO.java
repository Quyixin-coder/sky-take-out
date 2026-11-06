package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data       //@Data 会自动生成所有字段的 get/set 方法
public class SetmealPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    private String name;

    //分类id
    private Integer categoryId;

    //状态 0表示禁用 1表示启用
    private Integer status;

}
