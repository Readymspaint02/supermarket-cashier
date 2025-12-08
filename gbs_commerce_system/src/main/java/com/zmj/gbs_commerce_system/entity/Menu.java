package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private Integer isFrame;
    private String menuType;
    private Integer visible;
    private Integer status;
    private String perms;
    private String icon;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;

    @TableField(exist = false)
    private List<Menu> children;
}