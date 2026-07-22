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

    /** Fetches the finance setup summary for a published property. */
    @GetMapping("/properties/{propertyId}/summary")
    public ResponseEntity<ApiResponse<FinanceSummaryResponse>> getSummary(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getSummaryByPropertyId(propertyId), "Published property finance summary fetched"));
    }

    /** Fetches chart of accounts configured for a published property. */
    @GetMapping("/properties/{propertyId}/accounts")
    public ResponseEntity<ApiResponse<List<ChartOfAccountResponse>>> listAccounts(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.listAccountsByPropertyId(propertyId), "Published property chart of accounts fetched"));
    }

    /** Fetches a chart of account by id for a published property. */
    @GetMapping("/properties/{propertyId}/accounts/{accountId}")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> getAccountById(
        @PathVariable String propertyId,
        @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.getAccountById(propertyId, accountId), "Published property chart of account fetched"));
    }

    /** Creates a chart of account for a published property. */
    @PostMapping("/properties/{propertyId}/accounts")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> createAccount(
        @PathVariable String propertyId,
        @RequestBody ChartOfAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.createAccount(propertyId, request), "Published property chart of account created"));
    }

    /** Updates a chart of account for a published property. */
    @PutMapping("/properties/{propertyId}/accounts/{accountId}")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> updateAccount(
        @PathVariable String propertyId,
        @PathVariable Long accountId,
        @RequestBody ChartOfAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(financeService.updateAccount(propertyId, accountId, request), "Published property chart of account updated"));
    }

    /** Deletes a chart of account from a published property. */
    @DeleteMapping("/properties/{propertyId}/accounts/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
        @PathVariable String propertyId,
        @PathVariable Long accountId
    ) {
        financeService.deleteAccount(propertyId, accountId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Published property chart of account deleted"));
    }
}

