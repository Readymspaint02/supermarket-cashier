package com.zmj.gbs_commerce_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private String memberId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private Long cashierId;
    private String cashierName;
    private Integer paymentMethod;

    public static OrderPaidMessage of(Long orderId, String orderNo, String memberId,
                                       BigDecimal totalAmount, BigDecimal paidAmount,
                                       Long cashierId, String cashierName) {
        return new OrderPaidMessage(orderId, orderNo, memberId, totalAmount, paidAmount, cashierId, cashierName, null);
    }

    public static OrderPaidMessage of(Long orderId, String orderNo, String memberId,
                                       BigDecimal totalAmount, BigDecimal paidAmount,
                                       Long cashierId, String cashierName, Integer paymentMethod) {
        return new OrderPaidMessage(orderId, orderNo, memberId, totalAmount, paidAmount, cashierId, cashierName, paymentMethod);
    }
}