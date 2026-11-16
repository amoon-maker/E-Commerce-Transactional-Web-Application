
package com.umhamza.ecommerce;

import com.umhamza.ecommerce.service.DebitPaymentImpl;
import com.umhamza.ecommerce.service.CreditPaymentImpl;
import com.umhamza.ecommerce.service.BankService;
import com.umhamza.ecommerce.service.PaymentServiceSelector;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTests {

    private final DebitPaymentImpl debitService =
            new DebitPaymentImpl();

    private final CreditPaymentImpl creditService =
            new CreditPaymentImpl();

    private final PaymentServiceSelector selector =
            new PaymentServiceSelector(debitService, creditService);

    @Test
    void creditPaymentShouldProcessValidReference() {
        String result = creditService.processPayment(
                new BigDecimal("100.00"), "CREDIT-1234"
        );

        assertTrue(result.contains("Credit payment processed"));
    }

    @Test
    void creditPaymentShouldRejectInvalidReference() {
        assertThrows(IllegalArgumentException.class, () ->
                creditService.processPayment(
                        new BigDecimal("100.00"), "DEBIT-123456"
                )
        );
    }

    @Test
    void debitPaymentShouldProcessValidReference() {
        String result = debitService.processPayment(
                new BigDecimal("200.00"), "DEBIT-123456"
        );

        assertTrue(result.contains("Debit payment processed"));
    }

    @Test
    void debitPaymentShouldRejectInvalidReference() {
        assertThrows(IllegalArgumentException.class, () ->
                debitService.processPayment(
                        new BigDecimal("200.00"), "CREDIT-1234"
                )
        );
    }

    @Test
    void selectorShouldReturnCreditImplementation() {
        BankService service =
                selector.getPaymentService("CREDIT");

        assertInstanceOf(CreditPaymentImpl.class, service);
    }

    @Test
    void selectorShouldReturnDebitImplementation() {
        BankService service =
                selector.getPaymentService("DEBIT");

        assertInstanceOf(DebitPaymentImpl.class, service);
    }

    @Test
    void selectorShouldRejectUnsupportedPaymentMethod() {
        assertThrows(IllegalArgumentException.class, () ->
                selector.getPaymentService("CASH")
        );
    }
}
