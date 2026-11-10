package com.umhamza.ecommerce.service;

import java.math.BigDecimal;

public interface PaymentService {

    String processPayment(BigDecimal amount, String paymentReference);
}