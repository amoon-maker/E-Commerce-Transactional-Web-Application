package com.umhamza.ecommerce.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BankPaymentServiceImpl implements PaymentService {

    @Override
    public String processPayment(BigDecimal amount, String paymentReference) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Bank payment amount must be greater than zero."
            );
        }

        if (paymentReference == null ||
                !paymentReference.matches("BANK-\\d{6}")) {
            throw new IllegalArgumentException(
                    "Invalid bank transfer reference. Expected format: BANK-123456."
            );
        }

        return "Bank transfer payment submitted successfully for " + amount;
    }
}