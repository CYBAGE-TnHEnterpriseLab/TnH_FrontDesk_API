package com.frontdesk.pms.account.service;

import com.frontdesk.pms.account.dto.ChartOfAccountRequestDTO;
import com.frontdesk.pms.account.dto.ChartOfAccountResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;
import com.frontdesk.pms.account.exception.BadRequestException;
import com.frontdesk.pms.account.exception.ChartOfAccountNotFoundException;
import com.frontdesk.pms.account.exception.PropertyNotFoundException;
import com.frontdesk.pms.account.mapper.ChartOfAccountMapper;
import com.frontdesk.pms.account.repository.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChartOfAccountServiceImpl implements ChartOfAccountService {

    private final ChartOfAccountRepository repository;
    private final PropertyLookupService propertyLookupService;
    private final TransactionReferenceService transactionReferenceService;

    @Override
    public ChartOfAccountResponseDTO create(UUID propertyId, ChartOfAccountRequestDTO request) {
        assertPropertyExists(propertyId);
        String code = normalizeRequired(request.getCode(), "code");
        if (repository.existsByPropertyIdAndCodeIgnoreCase(propertyId, code)) {
            throw new BadRequestException("Chart of account code already exists for property");
        }
        String name = normalizeRequired(request.getName(), "name");
        if (repository.existsByPropertyIdAndNameIgnoreCase(propertyId, name)) {
            throw new BadRequestException("Chart of account name already exists for property");
        }

        // Validate allowed LedgerType values
        if (request.getLedgerType() == null ||
            !(request.getLedgerType().name().equals("REVENUE") ||
              request.getLedgerType().name().equals("LIABILITY") ||
              request.getLedgerType().name().equals("ASSET") ||
              request.getLedgerType().name().equals("EXPENSE"))) {
            throw new BadRequestException("Ledger type must be one of: REVENUE, LIABILITY, ASSET, EXPENSE");
        }

        // Validate AccountType/LedgerType combinations as per FRD
        switch (request.getAccountType()) {
            case REVENUE:
                if (request.getLedgerType() != com.frontdesk.common.enums.LedgerType.REVENUE) {
                    throw new BadRequestException("Account type REVENUE must have ledger type REVENUE");
                }
                break;
            case TAX:
                if (request.getLedgerType() != com.frontdesk.common.enums.LedgerType.LIABILITY) {
                    throw new BadRequestException("Account type TAX must have ledger type LIABILITY");
                }
                break;
            case PAYMENT:
            case DEPOSIT:
                if (request.getLedgerType() != com.frontdesk.common.enums.LedgerType.ASSET && request.getLedgerType() != com.frontdesk.common.enums.LedgerType.LIABILITY) {
                    throw new BadRequestException("Account type PAYMENT or DEPOSIT must have ledger type ASSET or LIABILITY");
                }
                break;
            case EXPENSE:
                if (request.getLedgerType() != com.frontdesk.common.enums.LedgerType.EXPENSE) {
                    throw new BadRequestException("Account type EXPENSE must have ledger type EXPENSE");
                }
                break;
            default:
                throw new BadRequestException("Invalid account type");
        }

        ChartOfAccount entity = new ChartOfAccount();
        entity.setPropertyId(propertyId);
        entity.setCode(code);
        entity.setName(name);
        entity.setAccountType(request.getAccountType());
        entity.setLedgerType(request.getLedgerType());
        entity.setDescription(normalizeOptional(request.getDescription()));
        entity.setActive(request.isActive());
        touch(entity);

        return ChartOfAccountMapper.toResponse(repository.save(entity));
    }

    @Override
    public List<ChartOfAccountResponseDTO> listByProperty(UUID propertyId) {
        assertPropertyExists(propertyId);
        return repository.findByPropertyId(propertyId).stream()
                .map(ChartOfAccountMapper::toResponse)
                .toList();
    }

    @Override
    public ChartOfAccountResponseDTO get(UUID propertyId, UUID accountId) {
        assertPropertyExists(propertyId);
        return ChartOfAccountMapper.toResponse(findEntity(propertyId, accountId));
    }

    @Override
    public ChartOfAccountResponseDTO update(UUID propertyId, UUID accountId, ChartOfAccountRequestDTO request) {
        assertPropertyExists(propertyId);
        ChartOfAccount entity = findEntity(propertyId, accountId);
        String code = normalizeRequired(request.getCode(), "code");
        String name = normalizeRequired(request.getName(), "name");
        if (!entity.getCode().equalsIgnoreCase(code)
                && repository.existsByPropertyIdAndCodeIgnoreCase(propertyId, code)) {
            throw new BadRequestException("Chart of account code already exists for property");
        }
        if (!entity.getName().equalsIgnoreCase(name)
                && repository.existsByPropertyIdAndNameIgnoreCase(propertyId, name)) {
            throw new BadRequestException("Chart of account name already exists for property");
        }

        entity.setCode(code);
        entity.setName(name);
        entity.setAccountType(request.getAccountType());
        entity.setLedgerType(request.getLedgerType());
        entity.setDescription(normalizeOptional(request.getDescription()));
        entity.setActive(request.isActive());
        touch(entity);

        return ChartOfAccountMapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(UUID propertyId, UUID accountId) {
        assertPropertyExists(propertyId);
        ChartOfAccount entity = findEntity(propertyId, accountId);
        if (transactionReferenceService.hasTransactionsForAccount(propertyId, accountId)) {
            throw new BadRequestException("Cannot delete account because transactions exist");
        }
        repository.delete(entity);
    }

    @Override
    public boolean isRevenueLedgerAvailable(UUID propertyId, UUID accountId) {
        ChartOfAccount entity = findEntity(propertyId, accountId);
        return entity.getLedgerType() == com.frontdesk.common.enums.LedgerType.REVENUE;
    }

    private ChartOfAccount findEntity(UUID propertyId, UUID accountId) {
        return repository.findByIdAndPropertyId(accountId, propertyId)
                .orElseThrow(() -> new ChartOfAccountNotFoundException(accountId));
    }

    private void assertPropertyExists(UUID propertyId) {
        try {
            if (!propertyLookupService.exists(propertyId)) {
                throw new PropertyNotFoundException(propertyId);
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to validate property because property-service is unavailable",
                    ex
            );
        }
    }

    private void touch(ChartOfAccount entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(field + " must not be blank");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
