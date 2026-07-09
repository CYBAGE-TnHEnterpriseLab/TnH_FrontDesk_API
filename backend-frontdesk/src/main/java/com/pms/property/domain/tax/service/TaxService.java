package com.pms.property.domain.tax.service;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.tax.dto.TaxRequest;
import com.pms.property.domain.tax.dto.TaxResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import com.pms.property.domain.tax.entity.TaxEntity;
import com.pms.property.domain.tax.repository.TaxRepository;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxService {

    private final TaxRepository taxRepository;
    private final TaxRuleRepository taxRuleRepository;

    public TaxService(TaxRepository taxRepository, TaxRuleRepository taxRuleRepository) {
        this.taxRepository = taxRepository;
        this.taxRuleRepository = taxRuleRepository;
    }

    @Transactional(readOnly = true)
    public TaxSummaryResponse getSummaryByPropertyId(String propertyId) {
        return new TaxSummaryResponse(
            propertyId,
            taxRepository.findByPropertyId(propertyId).isPresent(),
            taxRuleRepository.countByPropertyId(propertyId)
        );
    }

    @Transactional(readOnly = true)
    public TaxResponse getTaxByPropertyId(String propertyId) {
        return taxRepository.findByPropertyId(propertyId)
            .map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Tax configuration not found for property: " + propertyId));
    }

    @Transactional(readOnly = true)
    public TaxResponse getTaxById(String propertyId, Long taxId) {
        return taxRepository.findByPropertyIdAndId(propertyId, taxId)
            .map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Tax configuration not found: " + taxId));
    }

    @Transactional
    public TaxResponse createTax(String propertyId, TaxRequest request) {
        if (taxRepository.findByPropertyId(propertyId).isPresent()) {
            throw new BadRequestException("Tax configuration already exists for property: " + propertyId);
        }
        TaxEntity entity = new TaxEntity();
        entity.setPropertyId(propertyId);
        entity.setGstNumber(request.gstNumber());
        entity.setTaxPercentage(request.taxPercentage());
        return toResponse(taxRepository.save(entity));
    }

    @Transactional
    public TaxResponse updateTax(String propertyId, Long taxId, TaxRequest request) {
        TaxEntity entity = taxRepository.findByPropertyIdAndId(propertyId, taxId)
            .orElseThrow(() -> new NotFoundException("Tax configuration not found: " + taxId));
        entity.setGstNumber(request.gstNumber());
        entity.setTaxPercentage(request.taxPercentage());
        return toResponse(taxRepository.save(entity));
    }

    @Transactional
    public void deleteTax(String propertyId, Long taxId) {
        TaxEntity entity = taxRepository.findByPropertyIdAndId(propertyId, taxId)
            .orElseThrow(() -> new NotFoundException("Tax configuration not found: " + taxId));
        taxRepository.delete(entity);
    }

    private TaxResponse toResponse(TaxEntity entity) {
        return new TaxResponse(entity.getId(), entity.getPropertyId(), entity.getGstNumber(), entity.getTaxPercentage());
    }
}

