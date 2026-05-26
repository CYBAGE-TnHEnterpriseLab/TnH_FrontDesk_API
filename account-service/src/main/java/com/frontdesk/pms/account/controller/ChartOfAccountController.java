   
package com.frontdesk.pms.account.controller;

import com.frontdesk.pms.account.dto.ChartOfAccountRequestDTO;
import com.frontdesk.pms.account.dto.ChartOfAccountResponseDTO;
import com.frontdesk.pms.account.service.ChartOfAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/properties/{propertyId}/chart-of-accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChartOfAccountController {

    private final ChartOfAccountService service;

    @PostMapping
    public ChartOfAccountResponseDTO create(
            @PathVariable UUID propertyId,
            @RequestBody @Valid ChartOfAccountRequestDTO request
    ) {
        return service.create(propertyId, request);
    }

    @GetMapping
    public List<ChartOfAccountResponseDTO> list(@PathVariable UUID propertyId) {
        return service.listByProperty(propertyId);
    }

    @GetMapping("/{accountId}")
    public ChartOfAccountResponseDTO get(
            @PathVariable UUID propertyId,
            @PathVariable UUID accountId
    ) {
        return service.get(propertyId, accountId);
    }

     // Endpoint to list all accounts for mapping dropdown
    @GetMapping("/dropdown")
    public List<ChartOfAccountResponseDTO> dropdown(@PathVariable UUID propertyId) {
        return service.listByProperty(propertyId);
    }

    // Endpoint to fetch only ASSET and LIABILITY ledger type accounts
    @GetMapping("/asset-liability")
    public List<ChartOfAccountResponseDTO> assetAndLiabilityAccounts(@PathVariable UUID propertyId) {
        return service.listByProperty(propertyId).stream()
                .filter(a -> a.getLedgerType() == com.frontdesk.common.enums.LedgerType.ASSET || a.getLedgerType() == com.frontdesk.common.enums.LedgerType.LIABILITY)
                .toList();
    }

    @PutMapping("/{accountId}")
    public ChartOfAccountResponseDTO update(
            @PathVariable UUID propertyId,
            @PathVariable UUID accountId,
            @RequestBody @Valid ChartOfAccountRequestDTO request
    ) {
        return service.update(propertyId, accountId, request);
    }

    @DeleteMapping("/{accountId}")
    public void delete(
            @PathVariable UUID propertyId,
            @PathVariable UUID accountId
    ) {
        service.delete(propertyId, accountId);
    }
}
