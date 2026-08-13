package com.folio.billing.client;

import com.folio.billing.dto.PropertyTaxRule;
import java.util.List;

public interface PropertyTaxRuleClient {
    List<PropertyTaxRule> getTaxRules(String propertyId);
}
