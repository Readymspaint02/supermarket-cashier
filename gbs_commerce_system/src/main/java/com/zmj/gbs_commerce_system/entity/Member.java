package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("members")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String memberId;
    private String name;
    private Integer gender;
    private Integer age;
    private String phone;
    private String email;
    private String level;
    private Integer points;
    private BigDecimal balance;
    private String address;
    private Integer status;
    private Integer faceRegistered;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
