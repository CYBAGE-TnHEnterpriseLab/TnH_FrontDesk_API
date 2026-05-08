package com.frontdesk.pms.account.service;

import com.frontdesk.common.enums.AccountType;
import com.frontdesk.common.enums.LedgerType;
import com.frontdesk.pms.account.dto.ChartOfAccountRequestDTO;
import com.frontdesk.pms.account.dto.ChartOfAccountResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;
import com.frontdesk.pms.account.exception.BadRequestException;
import com.frontdesk.pms.account.exception.ChartOfAccountNotFoundException;
import com.frontdesk.pms.account.exception.PropertyNotFoundException;
import com.frontdesk.pms.account.repository.ChartOfAccountRepository;
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
class ChartOfAccountServiceImplTest {

    @Mock
    private ChartOfAccountRepository repository;

    @Mock
    private PropertyLookupService propertyLookupService;

    @Mock
    private TransactionReferenceService transactionReferenceService;

    @InjectMocks
    private ChartOfAccountServiceImpl service;

    @Test
    void createSavesChartOfAccountForProperty() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndCodeIgnoreCase(propertyId, "ROOM_REV")).thenReturn(false);
        when(repository.existsByPropertyIdAndNameIgnoreCase(propertyId, "Room Revenue")).thenReturn(false);
        when(repository.save(any(ChartOfAccount.class))).thenAnswer(invocation -> {
            ChartOfAccount entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ChartOfAccountResponseDTO response = service.create(propertyId, request("ROOM_REV", "Room Revenue"));

        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        assertThat(response.getCode()).isEqualTo("ROOM_REV");
        assertThat(response.getName()).isEqualTo("Room Revenue");
    }

    @Test
    void createRejectsDuplicateCode() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndCodeIgnoreCase(propertyId, "ROOM_REV")).thenReturn(true);

        assertThatThrownBy(() -> service.create(propertyId, request("ROOM_REV", "Room Revenue")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chart of account code already exists for property");
    }

    @Test
    void createRejectsDuplicateName() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.existsByPropertyIdAndCodeIgnoreCase(propertyId, "ROOM_REV")).thenReturn(false);
        when(repository.existsByPropertyIdAndNameIgnoreCase(propertyId, "Room Revenue")).thenReturn(true);

        assertThatThrownBy(() -> service.create(propertyId, request("ROOM_REV", "Room Revenue")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chart of account name already exists for property");
    }

    @Test
    void getThrowsWhenScopedAccountMissing() {
        UUID propertyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByIdAndPropertyId(accountId, propertyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(propertyId, accountId))
                .isInstanceOf(ChartOfAccountNotFoundException.class);
    }

    @Test
    void listByPropertyReturnsMappedResults() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByPropertyId(propertyId)).thenReturn(List.of(entity(propertyId, "ROOM_REV", "Room Revenue")));

        List<ChartOfAccountResponseDTO> results = service.listByProperty(propertyId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCode()).isEqualTo("ROOM_REV");
    }

    @Test
    void deleteValidatesPropertyBeforeDelete() {
        UUID propertyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ChartOfAccount entity = entity(propertyId, "ROOM_REV", "Room Revenue");
        entity.setId(accountId);
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByIdAndPropertyId(accountId, propertyId)).thenReturn(Optional.of(entity));
        when(transactionReferenceService.hasTransactionsForAccount(propertyId, accountId)).thenReturn(false);

        service.delete(propertyId, accountId);

        verify(repository).delete(entity);
    }

    @Test
    void deleteRejectsWhenTransactionsExist() {
        UUID propertyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ChartOfAccount entity = entity(propertyId, "ROOM_REV", "Room Revenue");
        entity.setId(accountId);
        when(propertyLookupService.exists(propertyId)).thenReturn(true);
        when(repository.findByIdAndPropertyId(accountId, propertyId)).thenReturn(Optional.of(entity));
        when(transactionReferenceService.hasTransactionsForAccount(propertyId, accountId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(propertyId, accountId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete account because transactions exist");
    }

    @Test
    void createThrowsWhenPropertyMissing() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(propertyId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(propertyId, request("ROOM_REV", "Room Revenue")))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    private ChartOfAccountRequestDTO request(String code, String name) {
        ChartOfAccountRequestDTO request = new ChartOfAccountRequestDTO();
        request.setCode(code);
        request.setName(name);
        request.setAccountType(AccountType.REVENUE);
        request.setLedgerType(LedgerType.REVENUE);
        request.setDescription("Default revenue ledger");
        request.setActive(true);
        return request;
    }

    private ChartOfAccount entity(UUID propertyId, String code, String name) {
        ChartOfAccount entity = new ChartOfAccount();
        entity.setId(UUID.randomUUID());
        entity.setPropertyId(propertyId);
        entity.setCode(code);
        entity.setName(name);
        entity.setAccountType(AccountType.REVENUE);
        entity.setLedgerType(LedgerType.REVENUE);
        entity.setActive(true);
        return entity;
    }
}
