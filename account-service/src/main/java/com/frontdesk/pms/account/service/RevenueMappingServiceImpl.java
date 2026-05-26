package com.frontdesk.pms.account.service;

import com.frontdesk.common.enums.AccountType;
import com.frontdesk.common.enums.LedgerType;
import com.frontdesk.pms.account.dto.RevenueMappingValidationResponseDTO;
import com.frontdesk.pms.account.enums.ChargeType;
import com.frontdesk.pms.account.dto.RevenueMappingRequestDTO;
import com.frontdesk.pms.account.dto.RevenueMappingResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;
import com.frontdesk.pms.account.entity.RevenueMapping;
import com.frontdesk.pms.account.exception.BadRequestException;
import com.frontdesk.pms.account.exception.ChartOfAccountNotFoundException;
import com.frontdesk.pms.account.exception.PropertyNotFoundException;
import com.frontdesk.pms.account.exception.RevenueMappingNotFoundException;
import com.frontdesk.pms.account.mapper.RevenueMappingMapper;
import com.frontdesk.pms.account.repository.ChartOfAccountRepository;
import com.frontdesk.pms.account.repository.RevenueMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevenueMappingServiceImpl implements RevenueMappingService {

    private final RevenueMappingRepository repository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final PropertyLookupService propertyLookupService;

    @Override
    public RevenueMappingResponseDTO create(UUID propertyId, RevenueMappingRequestDTO request) {
        assertPropertyExists(propertyId);
        // If mapping exists, update it instead of throwing error
        List<RevenueMapping> existingMappings = repository.findByPropertyId(propertyId);
        RevenueMapping existing = existingMappings.stream()
            .filter(m -> m.getChargeType() == request.getChargeType())
            .findFirst()
            .orElse(null);

        ChartOfAccount chartOfAccount = findChartOfAccount(propertyId, request.getChartOfAccountId());
        validateAccountMapping(request.getChargeType(), chartOfAccount);

        RevenueMapping entity;
        if (existing != null) {
            entity = existing;
        } else {
            entity = new RevenueMapping();
            entity.setPropertyId(propertyId);
            entity.setChargeType(request.getChargeType());
        }
        entity.setChartOfAccountId(chartOfAccount.getId());
        entity.setActive(request.isActive());
        touch(entity);

        return RevenueMappingMapper.toResponse(repository.save(entity));
    }

    @Override
    public List<RevenueMappingResponseDTO> listByProperty(UUID propertyId) {
        assertPropertyExists(propertyId);
        return repository.findByPropertyId(propertyId).stream()
                .map(RevenueMappingMapper::toResponse)
                .toList();
    }

    @Override
    public RevenueMappingResponseDTO get(UUID propertyId, UUID mappingId) {
        assertPropertyExists(propertyId);
        return RevenueMappingMapper.toResponse(findEntity(propertyId, mappingId));
    }

    @Override
    public RevenueMappingResponseDTO update(UUID propertyId, UUID mappingId, RevenueMappingRequestDTO request) {
        assertPropertyExists(propertyId);
        RevenueMapping entity = findEntity(propertyId, mappingId);
        if (entity.getChargeType() != request.getChargeType()
                && repository.existsByPropertyIdAndChargeType(propertyId, request.getChargeType())) {
            throw new BadRequestException("Revenue mapping already exists for charge type");
        }

        ChartOfAccount chartOfAccount = findChartOfAccount(propertyId, request.getChartOfAccountId());
        validateAccountMapping(request.getChargeType(), chartOfAccount);

        entity.setChargeType(request.getChargeType());
        entity.setChartOfAccountId(chartOfAccount.getId());
        entity.setActive(request.isActive());
        touch(entity);

        return RevenueMappingMapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(UUID propertyId, UUID mappingId) {
        assertPropertyExists(propertyId);
        repository.delete(findEntity(propertyId, mappingId));
    }

    @Override
    public RevenueMappingValidationResponseDTO validatePostingReadiness(UUID propertyId) {
        assertPropertyExists(propertyId);
        List<ChargeType> configured = repository.findByPropertyId(propertyId).stream()
                .filter(RevenueMapping::isActive)
                .map(RevenueMapping::getChargeType)
                .toList();

        List<ChargeType> missing = Arrays.stream(ChargeType.values())
                .filter(chargeType -> !configured.contains(chargeType))
                .toList();

        return RevenueMappingValidationResponseDTO.builder()
                .propertyId(propertyId)
                .postingAllowed(missing.isEmpty())
                .missingChargeTypes(missing)
                .build();
    }

    private RevenueMapping findEntity(UUID propertyId, UUID mappingId) {
        return repository.findByIdAndPropertyId(mappingId, propertyId)
                .orElseThrow(() -> new RevenueMappingNotFoundException(mappingId));
    }

    private ChartOfAccount findChartOfAccount(UUID propertyId, UUID chartOfAccountId) {
        return chartOfAccountRepository.findByIdAndPropertyId(chartOfAccountId, propertyId)
                .orElseThrow(() -> new ChartOfAccountNotFoundException(chartOfAccountId));
    }

    private void validateAccountMapping(ChargeType chargeType, ChartOfAccount chartOfAccount) {
        if (chargeType == ChargeType.TAXES) {
            if (chartOfAccount.getLedgerType() != LedgerType.LIABILITY || chartOfAccount.getAccountType() != AccountType.TAX) {
                throw new BadRequestException("Taxes must map to a tax liability account");
            }
            return;
        }

        if (chartOfAccount.getLedgerType() != LedgerType.REVENUE || chartOfAccount.getAccountType() != AccountType.REVENUE) {
            throw new BadRequestException("Charge type must map to a revenue ledger account");
        }
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

    private void touch(RevenueMapping entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

}
