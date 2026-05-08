package com.frontdesk.pms.account.service;

import com.frontdesk.common.enums.AccountType;
import com.frontdesk.common.enums.LedgerType;
import com.frontdesk.pms.account.dto.RevenueMappingRequestDTO;
import com.frontdesk.pms.account.dto.RevenueMappingResponseDTO;
import com.frontdesk.pms.account.dto.RevenueMappingValidationResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;
import com.frontdesk.pms.account.entity.RevenueMapping;
import com.frontdesk.pms.account.enums.ChargeType;
import com.frontdesk.pms.account.exception.BadRequestException;
import com.frontdesk.pms.account.exception.ChartOfAccountNotFoundException;
import com.frontdesk.pms.account.exception.RevenueMappingNotFoundException;
import com.frontdesk.pms.account.repository.ChartOfAccountRepository;
import com.frontdesk.pms.account.repository.RevenueMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueMappingServiceImplTest {

    @Mock
    private RevenueMappingRepository repository;

    @Mock
    private ChartOfAccountRepository chartOfAccountRepository;

    @Mock
    private PropertyLookupService propertyLookupService;

    @InjectMocks
    private RevenueMappingServiceImpl service;

    @Test
    void createSavesRevenueMapping() {
        UUID propertyId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndChargeType(propertyId, ChargeType.ROOM_CHARGES)).thenReturn(false);
        when(chartOfAccountRepository.findByIdAndPropertyId(chartOfAccountId, propertyId))
                .thenReturn(Optional.of(chartOfAccount(propertyId, chartOfAccountId)));
        when(repository.save(any(RevenueMapping.class))).thenAnswer(invocation -> {
            RevenueMapping entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        RevenueMappingResponseDTO response = service.create(propertyId, request(ChargeType.ROOM_CHARGES, chartOfAccountId));

        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        assertThat(response.getChargeType()).isEqualTo(ChargeType.ROOM_CHARGES);
    }

    @Test
    void createRejectsDuplicateChargeType() {
        UUID propertyId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndChargeType(propertyId, ChargeType.ROOM_CHARGES)).thenReturn(true);

        assertThatThrownBy(() -> service.create(propertyId, request(ChargeType.ROOM_CHARGES, chartOfAccountId)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Revenue mapping already exists for charge type");
    }

    @Test
    void createRejectsChartOfAccountFromDifferentScope() {
        UUID propertyId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndChargeType(propertyId, ChargeType.ROOM_CHARGES)).thenReturn(false);
        when(chartOfAccountRepository.findByIdAndPropertyId(chartOfAccountId, propertyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(propertyId, request(ChargeType.ROOM_CHARGES, chartOfAccountId)))
                .isInstanceOf(ChartOfAccountNotFoundException.class);
    }

    @Test
    void createRejectsTaxMappingToNonLiabilityAccount() {
        UUID propertyId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndChargeType(propertyId, ChargeType.TAXES)).thenReturn(false);
        when(chartOfAccountRepository.findByIdAndPropertyId(chartOfAccountId, propertyId))
                .thenReturn(Optional.of(chartOfAccount(propertyId, chartOfAccountId)));

        assertThatThrownBy(() -> service.create(propertyId, request(ChargeType.TAXES, chartOfAccountId)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Taxes must map to a tax liability account");
    }

    @Test
    void listByPropertyReturnsMappings() {
        UUID propertyId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByPropertyId(propertyId)).thenReturn(List.of(mapping(propertyId, chartOfAccountId, ChargeType.ROOM_CHARGES)));

        List<RevenueMappingResponseDTO> results = service.listByProperty(propertyId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChargeType()).isEqualTo(ChargeType.ROOM_CHARGES);
    }

    @Test
    void getThrowsWhenMappingMissing() {
        UUID propertyId = UUID.randomUUID();
        UUID mappingId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByIdAndPropertyId(mappingId, propertyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(propertyId, mappingId))
                .isInstanceOf(RevenueMappingNotFoundException.class);
    }

    @Test
    void deleteRemovesExistingMapping() {
        UUID propertyId = UUID.randomUUID();
        UUID mappingId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        RevenueMapping mapping = mapping(propertyId, chartOfAccountId);
        mapping.setId(mappingId);
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByIdAndPropertyId(mappingId, propertyId)).thenReturn(Optional.of(mapping));

        service.delete(propertyId, mappingId);

        verify(repository).delete(mapping);
    }

    @Test
    void validatePostingReadinessReturnsMissingChargeTypes() {
        UUID propertyId = UUID.randomUUID();
        UUID chartOfAccountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByPropertyId(propertyId)).thenReturn(List.of(
                mapping(propertyId, chartOfAccountId, ChargeType.ROOM_CHARGES),
                mapping(propertyId, chartOfAccountId, ChargeType.TAXES)
        ));

        RevenueMappingValidationResponseDTO response = service.validatePostingReadiness(propertyId);

        assertThat(response.isPostingAllowed()).isFalse();
        assertThat(response.getMissingChargeTypes()).contains(ChargeType.ADD_ONS, ChargeType.CANCELLATION_FEES, ChargeType.NO_SHOW_FEES);
    }

    private RevenueMappingRequestDTO request(ChargeType chargeType, UUID chartOfAccountId) {
        RevenueMappingRequestDTO request = new RevenueMappingRequestDTO();
        request.setChargeType(chargeType);
        request.setChartOfAccountId(chartOfAccountId);
        request.setActive(true);
        return request;
    }

    private ChartOfAccount chartOfAccount(UUID propertyId, UUID chartOfAccountId) {
        ChartOfAccount chartOfAccount = new ChartOfAccount();
        chartOfAccount.setId(chartOfAccountId);
        chartOfAccount.setPropertyId(propertyId);
        chartOfAccount.setCode("ROOM_REV");
        chartOfAccount.setName("Room Revenue");
        chartOfAccount.setAccountType(AccountType.REVENUE);
        chartOfAccount.setLedgerType(LedgerType.REVENUE);
        return chartOfAccount;
    }

    private RevenueMapping mapping(UUID propertyId, UUID chartOfAccountId) {
        return mapping(propertyId, chartOfAccountId, ChargeType.ROOM_CHARGES);
    }

    private RevenueMapping mapping(UUID propertyId, UUID chartOfAccountId, ChargeType chargeType) {
        RevenueMapping mapping = new RevenueMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setPropertyId(propertyId);
        mapping.setChargeType(chargeType);
        mapping.setChartOfAccountId(chartOfAccountId);
        mapping.setActive(true);
        return mapping;
    }
}
