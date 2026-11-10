package com.umhamza.ecommerce.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreditCardPaymentServiceImpl implements PaymentService {

    @Override
    public String processPayment(BigDecimal amount, String paymentReference) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Credit card payment amount must be greater than zero."
            );
        }

        if (paymentReference == null ||
                !paymentReference.matches("CARD-\\d{4}")) {
            throw new IllegalArgumentException(
                    "Invalid credit card reference. Expected format: CARD-1234."
            );
        }

        return "Credit card payment processed successfully for " + amount;
    }
}