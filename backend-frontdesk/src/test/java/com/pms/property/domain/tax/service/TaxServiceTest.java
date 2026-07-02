package com.pms.property.domain.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.tax.dto.TaxRequest;
import com.pms.property.domain.tax.entity.TaxEntity;
import com.pms.property.domain.tax.repository.TaxRepository;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaxServiceTest {

    @Test
    void shouldCreateTaxConfiguration() {
        TaxRepository taxRepository = mock(TaxRepository.class);
        TaxRuleRepository taxRuleRepository = mock(TaxRuleRepository.class);
        TaxService service = new TaxService(taxRepository, taxRuleRepository);

        when(taxRepository.findByPropertyId("P-1")).thenReturn(Optional.empty());
        when(taxRepository.save(any(TaxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createTax("P-1", new TaxRequest("GST123", 18.0));

        assertEquals("P-1", response.propertyId());
        assertEquals("GST123", response.gstNumber());
    }

    @Test
    void shouldRejectDuplicateTaxConfiguration() {
        TaxRepository taxRepository = mock(TaxRepository.class);
        TaxRuleRepository taxRuleRepository = mock(TaxRuleRepository.class);
        TaxService service = new TaxService(taxRepository, taxRuleRepository);

        when(taxRepository.findByPropertyId("P-1")).thenReturn(Optional.of(new TaxEntity()));

        assertThrows(BadRequestException.class, () -> service.createTax("P-1", new TaxRequest("GST123", 18.0)));
    }
}

