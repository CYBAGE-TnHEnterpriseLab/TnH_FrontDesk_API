package com.pms.property.domain.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.payment.dto.PaymentMethodRequest;
import com.pms.property.domain.payment.entity.PaymentMethodEntity;
import com.pms.property.domain.payment.repository.PaymentMethodRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    @Test
    void shouldCreatePaymentMethod() {
        PaymentMethodRepository repository = mock(PaymentMethodRepository.class);
        PaymentService service = new PaymentServiceImpl(repository);

        when(repository.save(any(PaymentMethodEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createMethod("P-1", new PaymentMethodRequest("CASH", "CASH_LEDGER", true, true));

        assertEquals("P-1", response.propertyId());
        assertEquals("CASH", response.paymentMethod());
    }

    @Test
    void shouldThrowWhenMethodNotFound() {
        PaymentMethodRepository repository = mock(PaymentMethodRepository.class);
        PaymentService service = new PaymentServiceImpl(repository);

        when(repository.findByPropertyIdAndId("P-1", 22L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getMethodById("P-1", 22L));
    }
}

