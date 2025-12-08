package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String salt;
    private String email;
    private String nickname;
    private String avatar;
    private Integer status;
    private String loginIp;
    private Date loginDate;
    private Date createTime;
    private Date updateTime;
    private String remark;

    @TableField(exist = false)
    private List<Role> roles;
}