package com.umhamza.ecommerce.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DebitPaymentImpl implements BankService {

    @Override
    public String processPayment(
            BigDecimal amount,
            String paymentReference) {

        // Validate payment amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Debit payment amount must be greater than zero."
            );
        }

        // Validate debit payment reference
        if (paymentReference == null ||
                !paymentReference.matches("DEBIT-\\d{6}")) {

            throw new IllegalArgumentException(
                    "Invalid debit reference. Expected format: DEBIT-123456."
            );
        }

        // Simulate successful debit payment
        return "Debit payment processed successfully for "
                + amount
                + ". Transaction reference: "
                + paymentReference;
    }
}