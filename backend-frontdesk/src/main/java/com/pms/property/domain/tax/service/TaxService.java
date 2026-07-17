package com.pms.property.domain.tax.service;

import com.pms.property.domain.tax.dto.TaxRuleRequest;
import com.pms.property.domain.tax.dto.TaxRuleResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import java.util.List;

public interface TaxService {

    TaxSummaryResponse getSummaryByPropertyId(String propertyId);

    List<TaxRuleResponse> getTaxRulesByPropertyId(String propertyId);

    TaxRuleResponse getTaxRuleById(String propertyId, Long ruleId);

    TaxRuleResponse createTaxRule(String propertyId, TaxRuleRequest request);

    TaxRuleResponse updateTaxRule(String propertyId, Long ruleId, TaxRuleRequest request);

    void deleteTaxRule(String propertyId, Long ruleId);
}

