package com.frontdesk.pms.account.service;

import com.frontdesk.common.enums.LedgerType;
import com.frontdesk.pms.account.dto.PaymentMethodRequestDTO;
import com.frontdesk.pms.account.dto.PaymentMethodResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;
import com.frontdesk.pms.account.entity.PaymentMethod;
import com.frontdesk.pms.account.exception.BadRequestException;
import com.frontdesk.pms.account.repository.ChartOfAccountRepository;
import com.frontdesk.pms.account.repository.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceImplTest {
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private ChartOfAccountRepository chartOfAccountRepository;
    @InjectMocks
    private PaymentMethodServiceImpl service;

    @Test
    void createPaymentMethodWithValidLedgerType() {
        UUID accountId = UUID.randomUUID();
        ChartOfAccount account = new ChartOfAccount();
        account.setId(accountId);
        account.setLedgerType(LedgerType.ASSET);
        when(chartOfAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> {
            PaymentMethod entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        PaymentMethodRequestDTO request = new PaymentMethodRequestDTO();
        request.setName("UPI");
        request.setAccountId(accountId);
        request.setAllowRefund(true);
        request.setActive(true);
        PaymentMethodResponseDTO response = service.create(request);
        assertThat(response.getName()).isEqualTo("UPI");
        assertThat(response.isAllowRefund()).isTrue();
    }

    @Test
    void createPaymentMethodWithInvalidLedgerTypeThrows() {
        UUID accountId = UUID.randomUUID();
        ChartOfAccount account = new ChartOfAccount();
        account.setId(accountId);
        account.setLedgerType(LedgerType.REVENUE);
        when(chartOfAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        PaymentMethodRequestDTO request = new PaymentMethodRequestDTO();
        request.setName("UPI");
        request.setAccountId(accountId);
        request.setAllowRefund(true);
        request.setActive(true);
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Payment must map to Asset or Liability ledger");
    }

    @Test
    void updatePaymentMethodWithValidLedgerType() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        PaymentMethod entity = new PaymentMethod();
        entity.setId(id);
        entity.setName("UPI");
        entity.setAccountId(accountId);
        entity.setAllowRefund(false);
        entity.setActive(true);
        ChartOfAccount account = new ChartOfAccount();
        account.setId(accountId);
        account.setLedgerType(LedgerType.LIABILITY);
        when(paymentMethodRepository.findById(id)).thenReturn(Optional.of(entity));
        when(chartOfAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentMethodRequestDTO request = new PaymentMethodRequestDTO();
        request.setName("UPI");
        request.setAccountId(accountId);
        request.setAllowRefund(true);
        request.setActive(true);
        PaymentMethodResponseDTO response = service.update(id, request);
        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.isAllowRefund()).isTrue();
    }

    @Test
    void updatePaymentMethodWithInvalidLedgerTypeThrows() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        PaymentMethod entity = new PaymentMethod();
        entity.setId(id);
        entity.setName("UPI");
        entity.setAccountId(accountId);
        entity.setAllowRefund(false);
        entity.setActive(true);
        ChartOfAccount account = new ChartOfAccount();
        account.setId(accountId);
        account.setLedgerType(LedgerType.EXPENSE);
        when(paymentMethodRepository.findById(id)).thenReturn(Optional.of(entity));
        when(chartOfAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        PaymentMethodRequestDTO request = new PaymentMethodRequestDTO();
        request.setName("UPI");
        request.setAccountId(accountId);
        request.setAllowRefund(true);
        request.setActive(true);
        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Payment must map to Asset or Liability ledger");
    }
}
