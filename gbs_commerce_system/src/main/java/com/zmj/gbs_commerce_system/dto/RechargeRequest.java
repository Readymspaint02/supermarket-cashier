package com.zmj.gbs_commerce_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeRequest {
    private String memberId;
    private BigDecimal rechargeAmount;
    private BigDecimal giftAmount;
    private Integer pointsToAdd;
    private String remark;
}