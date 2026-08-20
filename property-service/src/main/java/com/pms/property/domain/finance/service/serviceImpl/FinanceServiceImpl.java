package com.pms.property.domain.finance.service;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.finance.dto.ChartOfAccountRequest;
import com.pms.property.domain.finance.dto.ChartOfAccountResponse;
import com.pms.property.domain.finance.dto.FinanceSummaryResponse;
import com.pms.property.domain.finance.entity.ChartOfAccountEntity;
import com.pms.property.domain.finance.repository.ChartOfAccountRepository;
import com.pms.property.domain.finance.repository.RevenueMappingRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceServiceImpl implements FinanceService {

    private final ChartOfAccountRepository chartOfAccountRepository;
    private final RevenueMappingRepository revenueMappingRepository;

    public FinanceServiceImpl(
        ChartOfAccountRepository chartOfAccountRepository,
        RevenueMappingRepository revenueMappingRepository
    ) {
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.revenueMappingRepository = revenueMappingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceSummaryResponse getSummaryByPropertyId(String propertyId) {
        return new FinanceSummaryResponse(
            propertyId,
            chartOfAccountRepository.countByPropertyId(propertyId),
            revenueMappingRepository.countByPropertyId(propertyId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChartOfAccountResponse> listAccountsByPropertyId(String propertyId) {
        return chartOfAccountRepository.findAllByPropertyId(propertyId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChartOfAccountResponse getAccountById(String propertyId, Long accountId) {
        return chartOfAccountRepository.findByPropertyIdAndId(propertyId, accountId)
            .map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Chart of account not found: " + accountId));
    }

    @Override
    @Transactional
    public ChartOfAccountResponse createAccount(String propertyId, ChartOfAccountRequest request) {
        ChartOfAccountEntity entity = new ChartOfAccountEntity();
        entity.setPropertyId(propertyId);
        entity.setAccountCode(request.accountCode());
        entity.setAccountName(request.accountName());
        entity.setAccountType(request.accountType());
        entity.setLedgerType(request.ledgerType());
        entity.setActive(request.active());
        return toResponse(chartOfAccountRepository.save(entity));
    }

    @Override
    @Transactional
    public ChartOfAccountResponse updateAccount(String propertyId, Long accountId, ChartOfAccountRequest request) {
        ChartOfAccountEntity entity = chartOfAccountRepository.findByPropertyIdAndId(propertyId, accountId)
            .orElseThrow(() -> new NotFoundException("Chart of account not found: " + accountId));
        entity.setAccountCode(request.accountCode());
        entity.setAccountName(request.accountName());
        entity.setAccountType(request.accountType());
        entity.setLedgerType(request.ledgerType());
        entity.setActive(request.active());
        return toResponse(chartOfAccountRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAccount(String propertyId, Long accountId) {
        ChartOfAccountEntity entity = chartOfAccountRepository.findByPropertyIdAndId(propertyId, accountId)
            .orElseThrow(() -> new NotFoundException("Chart of account not found: " + accountId));
        chartOfAccountRepository.delete(entity);
    }

    private ChartOfAccountResponse toResponse(ChartOfAccountEntity entity) {
        return new ChartOfAccountResponse(
            entity.getId(),
            entity.getPropertyId(),
            entity.getAccountCode(),
            entity.getAccountName(),
            entity.getAccountType(),
            entity.getLedgerType(),
            entity.getActive()
        );
    }
}


