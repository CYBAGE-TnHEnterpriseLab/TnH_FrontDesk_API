package com.pms.property.domain.tax.service.serviceImpl;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.tax.dto.TaxRuleRequest;
import com.pms.property.domain.tax.dto.TaxRuleResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import com.pms.property.domain.tax.entity.TaxRuleEntity;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import com.pms.property.domain.tax.service.TaxService;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxServiceImpl implements TaxService {

    private final TaxRuleRepository taxRuleRepository;

    public TaxServiceImpl(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxSummaryResponse getSummaryByPropertyId(String propertyId) {
        long count = taxRuleRepository.countByPropertyId(propertyId);
        return new TaxSummaryResponse(propertyId, count > 0, count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxRuleResponse> getTaxRulesByPropertyId(String propertyId) {
        return taxRuleRepository.findAllByPropertyIdOrderByPriorityAsc(propertyId)
            .stream()
            .map(this::toRuleResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaxRuleResponse getTaxRuleById(String propertyId, Long ruleId) {
        return taxRuleRepository.findByPropertyIdAndId(propertyId, ruleId)
            .map(this::toRuleResponse)
            .orElseThrow(() -> new NotFoundException("Tax rule not found: " + ruleId));
    }

    @Override
    @Transactional
    public TaxRuleResponse createTaxRule(String propertyId, TaxRuleRequest request) {
        TaxRuleEntity entity = new TaxRuleEntity();
        entity.setPropertyId(propertyId);
        applyRuleRequest(entity, request);
        return toRuleResponse(taxRuleRepository.save(entity));
    }

    @Override
    @Transactional
    public TaxRuleResponse updateTaxRule(String propertyId, Long ruleId, TaxRuleRequest request) {
        TaxRuleEntity entity = taxRuleRepository.findByPropertyIdAndId(propertyId, ruleId)
            .orElseThrow(() -> new NotFoundException("Tax rule not found: " + ruleId));
        applyRuleRequest(entity, request);
        return toRuleResponse(taxRuleRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteTaxRule(String propertyId, Long ruleId) {
        TaxRuleEntity entity = taxRuleRepository.findByPropertyIdAndId(propertyId, ruleId)
            .orElseThrow(() -> new NotFoundException("Tax rule not found: " + ruleId));
        taxRuleRepository.delete(entity);
    }

    private void applyRuleRequest(TaxRuleEntity entity, TaxRuleRequest request) {
        entity.setTaxName(request.taxName());
        entity.setType(request.type());
        entity.setRate(request.rate());
        entity.setApplicableOn(request.applicableOn());
        entity.setInclExcl(request.inclExcl());
        entity.setEffectiveDate(request.effectiveDate());
        entity.setActive(request.active());
        entity.setStatus(request.status());
        entity.setPriority(request.priority());
    }

    private TaxRuleResponse toRuleResponse(TaxRuleEntity entity) {
        return new TaxRuleResponse(
            entity.getId(),
            entity.getPropertyId(),
            entity.getTaxName(),
            entity.getType(),
            entity.getRate(),
            entity.getApplicableOn(),
            entity.getInclExcl(),
            entity.getEffectiveDate(),
            entity.getActive(),
            entity.getStatus(),
            entity.getPriority()
        );
    }
}


