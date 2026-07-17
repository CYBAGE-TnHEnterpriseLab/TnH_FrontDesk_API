package com.pms.property.domain.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.tax.dto.TaxRuleRequest;
import com.pms.property.domain.tax.entity.TaxRuleEntity;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaxServiceTest {

    @Test
    void shouldCreateTaxRule() {
        TaxRuleRepository taxRuleRepository = mock(TaxRuleRepository.class);
        TaxService service = new TaxServiceImpl(taxRuleRepository);

        when(taxRuleRepository.save(any(TaxRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createTaxRule(
            "P-1",
            new TaxRuleRequest("Service Tax", "Percentage", 6.0, "Add On", "Exclusive", "2026-05-12", true, "ACTIVE", 1)
        );

        assertEquals("P-1", response.propertyId());
        assertEquals("Service Tax", response.taxName());
    }

    @Test
    void shouldFailWhenUpdatingMissingTaxRule() {
        TaxRuleRepository taxRuleRepository = mock(TaxRuleRepository.class);
        TaxService service = new TaxServiceImpl(taxRuleRepository);

        when(taxRuleRepository.findByPropertyIdAndId("P-1", 10L)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> service.updateTaxRule(
                "P-1",
                10L,
                new TaxRuleRequest("Luxury Tax", "Percentage", 10.0, "Add On", "Inclusive", "2026-06-16", true, "ACTIVE", 2)
            )
        );
    }
}

