package com.pms.property.domain.tax.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.tax.dto.TaxRuleRequest;
import com.pms.property.domain.tax.dto.TaxRuleResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import com.pms.property.domain.tax.service.TaxService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/taxes")
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    /** Fetches the tax-rule summary for a published property. */
    @GetMapping("/properties/{propertyId}/summary")
    public ResponseEntity<ApiResponse<TaxSummaryResponse>> getSummary(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.getSummaryByPropertyId(propertyId), "Published property tax summary fetched"));
    }

    /** Fetches tax rules configured for a published property. */
    @GetMapping("/properties/{propertyId}/rules")
    public ResponseEntity<ApiResponse<List<TaxRuleResponse>>> getRules(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.getTaxRulesByPropertyId(propertyId), "Published property tax rules fetched"));
    }

    /** Fetches a tax rule by id for a published property. */
    @GetMapping("/properties/{propertyId}/rules/{ruleId}")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> getRuleById(@PathVariable String propertyId, @PathVariable Long ruleId) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.getTaxRuleById(propertyId, ruleId), "Published property tax rule fetched"));
    }

    /** Creates a tax rule for a published property. */
    @PostMapping("/properties/{propertyId}/rules")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> createRule(
        @PathVariable String propertyId,
        @RequestBody TaxRuleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.createTaxRule(propertyId, request), "Published property tax rule created"));
    }

    /** Updates a tax rule for a published property. */
    @PutMapping("/properties/{propertyId}/rules/{ruleId}")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> updateRule(
        @PathVariable String propertyId,
        @PathVariable Long ruleId,
        @RequestBody TaxRuleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.updateTaxRule(propertyId, ruleId, request), "Published property tax rule updated"));
    }

    /** Deletes a tax rule from a published property. */
    @DeleteMapping("/properties/{propertyId}/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable String propertyId, @PathVariable Long ruleId) {
        taxService.deleteTaxRule(propertyId, ruleId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Published property tax rule deleted"));
    }
}
