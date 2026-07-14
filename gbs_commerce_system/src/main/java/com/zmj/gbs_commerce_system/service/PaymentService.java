package com.zmj.gbs_commerce_system.service;

import com.zmj.gbs_commerce_system.dto.FacePayRequest;
import com.zmj.gbs_commerce_system.dto.PaymentRequest;
import com.zmj.gbs_commerce_system.entity.Orders;

public interface PaymentService {
    
    Orders createPaymentOrder(PaymentRequest request);
    
    Orders confirmPayment(Long orderId, Integer paymentMethod);
    
    Orders facePay(FacePayRequest request);
    
    Orders facePayBySearch(FacePayRequest request);
    
    Orders queryPayment(Long orderId);
}