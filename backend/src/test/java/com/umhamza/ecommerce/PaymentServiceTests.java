package com.umhamza.ecommerce;

import com.umhamza.ecommerce.service.BankPaymentServiceImpl;
import com.umhamza.ecommerce.service.CreditCardPaymentServiceImpl;
import com.umhamza.ecommerce.service.PaymentService;
import com.umhamza.ecommerce.service.PaymentServiceSelector;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTests {

    private final CreditCardPaymentServiceImpl creditCardService =
            new CreditCardPaymentServiceImpl();

    private final BankPaymentServiceImpl bankService =
            new BankPaymentServiceImpl();

    private final PaymentServiceSelector selector =
            new PaymentServiceSelector(creditCardService, bankService);

    @Test
    void creditCardPaymentShouldProcessValidReference() {
        String result = creditCardService.processPayment(
                new BigDecimal("100.00"), "CARD-1234"
        );

        assertTrue(result.contains("Credit card payment processed"));
    }

    @Test
    void creditCardPaymentShouldRejectInvalidReference() {
        assertThrows(IllegalArgumentException.class, () ->
                creditCardService.processPayment(
                        new BigDecimal("100.00"), "BANK-123456"
                )
        );
    }

    @Test
    void bankPaymentShouldProcessValidReference() {
        String result = bankService.processPayment(
                new BigDecimal("200.00"), "BANK-123456"
        );

        assertTrue(result.contains("Bank transfer payment submitted"));
    }

    @Test
    void bankPaymentShouldRejectInvalidReference() {
        assertThrows(IllegalArgumentException.class, () ->
                bankService.processPayment(
                        new BigDecimal("200.00"), "CARD-1234"
                )
        );
    }

    @Test
    void selectorShouldReturnCreditCardImplementation() {
        PaymentService service =
                selector.getPaymentService("CREDIT_CARD");

        assertInstanceOf(CreditCardPaymentServiceImpl.class, service);
    }

    @Test
    void selectorShouldReturnBankImplementation() {
        PaymentService service =
                selector.getPaymentService("BANK_TRANSFER");

        assertInstanceOf(BankPaymentServiceImpl.class, service);
    }

    @Test
    void selectorShouldRejectUnsupportedPaymentMethod() {
        assertThrows(IllegalArgumentException.class, () ->
                selector.getPaymentService("CASH")
        );
    }
}