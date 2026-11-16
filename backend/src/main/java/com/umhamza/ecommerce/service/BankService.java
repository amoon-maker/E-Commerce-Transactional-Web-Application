package com.umhamza.ecommerce.service;

import java.math.BigDecimal;

public interface BankService {

    String processPayment(BigDecimal amount, String paymentReference);
}