package com.umhamza.ecommerce.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreditPaymentImpl implements BankService {

    @Override
    public String processPayment(
            BigDecimal amount,
            String paymentReference) {

        // Validate payment amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Credit payment amount must be greater than zero."
            );
        }

        // Validate credit payment reference
        if (paymentReference == null ||
                !paymentReference.matches("CREDIT-\\d{4}")) {

            throw new IllegalArgumentException(
                    "Invalid credit reference. Expected format: CREDIT-1234."
            );
        }

        // Simulate successful credit payment
        return "Credit payment processed successfully for "
                + amount
                + ". Transaction reference: "
                + paymentReference;
    }
}