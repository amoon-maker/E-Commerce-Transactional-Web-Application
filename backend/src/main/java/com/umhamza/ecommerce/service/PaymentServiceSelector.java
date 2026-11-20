package com.umhamza.ecommerce.service;

import org.springframework.stereotype.Component;

@Component
public class PaymentServiceSelector {

    private final DebitPaymentImpl debitPayment;
    private final CreditPaymentImpl creditPayment;

    public PaymentServiceSelector(
            DebitPaymentImpl debitPayment,
            CreditPaymentImpl creditPayment) {

        this.debitPayment = debitPayment;
        this.creditPayment = creditPayment;
    }

    public BankService getPaymentService(String paymentMethod) {

        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment method cannot be null or empty."
            );
        }

        return switch (paymentMethod.trim().toUpperCase()) {
            case "DEBIT" -> debitPayment;
            case "CREDIT" -> creditPayment;

            default -> throw new IllegalArgumentException(
                    "Unsupported payment method: " + paymentMethod
            );
        };
    }
}