package com.pms.property.domain.tax.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.tax.dto.TaxRequest;
import com.pms.property.domain.tax.dto.TaxResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import com.pms.property.domain.tax.service.TaxService;
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

    @GetMapping("/properties/{propertyId}/summary")
    public ResponseEntity<ApiResponse<TaxSummaryResponse>> getSummary(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.getSummaryByPropertyId(propertyId), "Tax summary fetched"));
    }

    @GetMapping("/properties/{propertyId}/configurations/current")
    public ResponseEntity<ApiResponse<TaxResponse>> getCurrentByProperty(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.getTaxByPropertyId(propertyId), "Tax configuration fetched"));
    }

    @GetMapping("/properties/{propertyId}/configurations/{taxId}")
    public ResponseEntity<ApiResponse<TaxResponse>> getById(@PathVariable String propertyId, @PathVariable Long taxId) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.getTaxById(propertyId, taxId), "Tax configuration fetched"));
    }

    @PostMapping("/properties/{propertyId}/configurations")
    public ResponseEntity<ApiResponse<TaxResponse>> create(@PathVariable String propertyId, @RequestBody TaxRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.createTax(propertyId, request), "Tax configuration created"));
    }

    @PutMapping("/properties/{propertyId}/configurations/{taxId}")
    public ResponseEntity<ApiResponse<TaxResponse>> update(
        @PathVariable String propertyId,
        @PathVariable Long taxId,
        @RequestBody TaxRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(taxService.updateTax(propertyId, taxId, request), "Tax configuration updated"));
    }

    @DeleteMapping("/properties/{propertyId}/configurations/{taxId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String propertyId, @PathVariable Long taxId) {
        taxService.deleteTax(propertyId, taxId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tax configuration deleted"));
    }
}

