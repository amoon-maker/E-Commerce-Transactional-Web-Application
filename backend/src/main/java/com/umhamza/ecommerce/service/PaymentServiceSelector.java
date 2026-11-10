package com.umhamza.ecommerce.service;

import org.springframework.stereotype.Component;

@Component
public class PaymentServiceSelector {

    private final CreditCardPaymentServiceImpl creditCardPaymentService;
    private final BankPaymentServiceImpl bankPaymentService;

    public PaymentServiceSelector(
            CreditCardPaymentServiceImpl creditCardPaymentService,
            BankPaymentServiceImpl bankPaymentService) {

        this.creditCardPaymentService = creditCardPaymentService;
        this.bankPaymentService = bankPaymentService;
    }

    public PaymentService getPaymentService(String paymentMethod) {

        if (paymentMethod == null) {
            throw new IllegalArgumentException(
                    "Payment method cannot be null."
            );
        }

        return switch (paymentMethod.trim().toUpperCase()) {
            case "CREDIT_CARD" -> creditCardPaymentService;
            case "BANK_TRANSFER" -> bankPaymentService;
            default -> throw new IllegalArgumentException(
                    "Unsupported payment method: " + paymentMethod
            );
        };
    }
}