package com.pms.property.domain.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.finance.dto.ChartOfAccountRequest;
import com.pms.property.domain.finance.entity.ChartOfAccountEntity;
import com.pms.property.domain.finance.repository.ChartOfAccountRepository;
import com.pms.property.domain.finance.repository.RevenueMappingRepository;
import com.pms.property.domain.finance.service.serviceImpl.FinanceServiceImpl;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class FinanceServiceTest {

    @Test
    void shouldCreateAccount() {
        ChartOfAccountRepository accountRepository = mock(ChartOfAccountRepository.class);
        RevenueMappingRepository revenueRepository = mock(RevenueMappingRepository.class);
        FinanceService service = new FinanceServiceImpl(accountRepository, revenueRepository);

        when(accountRepository.save(any(ChartOfAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createAccount("P-1", new ChartOfAccountRequest("REV", "Revenue", "REVENUE", "LIABILITY", true));

        assertEquals("P-1", response.propertyId());
        assertEquals("REV", response.accountCode());
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        ChartOfAccountRepository accountRepository = mock(ChartOfAccountRepository.class);
        RevenueMappingRepository revenueRepository = mock(RevenueMappingRepository.class);
        FinanceService service = new FinanceServiceImpl(accountRepository, revenueRepository);

        when(accountRepository.findByPropertyIdAndId("P-1", 11L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getAccountById("P-1", 11L));
    }
}

