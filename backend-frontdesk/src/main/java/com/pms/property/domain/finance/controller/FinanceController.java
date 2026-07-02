package com.pms.property.domain.finance.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.finance.dto.ChartOfAccountRequest;
import com.pms.property.domain.finance.dto.ChartOfAccountResponse;
import com.pms.property.domain.finance.dto.FinanceSummaryResponse;
import com.pms.property.domain.finance.service.FinanceService;
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
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/properties/{propertyId}/summary")
    public ResponseEntity<ApiResponse<FinanceSummaryResponse>> getSummary(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getSummaryByPropertyId(propertyId), "Finance summary fetched"));
    }

    @GetMapping("/properties/{propertyId}/accounts")
    public ResponseEntity<ApiResponse<List<ChartOfAccountResponse>>> listAccounts(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.listAccountsByPropertyId(propertyId), "Chart of accounts fetched"));
    }

    @GetMapping("/properties/{propertyId}/accounts/{accountId}")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> getAccountById(
        @PathVariable String propertyId,
        @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getAccountById(propertyId, accountId), "Chart of account fetched"));
    }

    @PostMapping("/properties/{propertyId}/accounts")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> createAccount(
        @PathVariable String propertyId,
        @RequestBody ChartOfAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.createAccount(propertyId, request), "Chart of account created"));
    }

    @PutMapping("/properties/{propertyId}/accounts/{accountId}")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> updateAccount(
        @PathVariable String propertyId,
        @PathVariable Long accountId,
        @RequestBody ChartOfAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.updateAccount(propertyId, accountId, request), "Chart of account updated"));
    }

    @DeleteMapping("/properties/{propertyId}/accounts/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
        @PathVariable String propertyId,
        @PathVariable Long accountId
    ) {
        financeService.deleteAccount(propertyId, accountId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Chart of account deleted"));
    }
}

