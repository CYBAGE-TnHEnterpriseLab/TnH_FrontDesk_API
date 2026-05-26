package com.frontdesk.pms.account.service;

import com.frontdesk.pms.account.dto.ChartOfAccountRequestDTO;
import com.frontdesk.pms.account.dto.ChartOfAccountResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ChartOfAccountService {
    ChartOfAccountResponseDTO create(UUID propertyId, ChartOfAccountRequestDTO request);
    List<ChartOfAccountResponseDTO> listByProperty(UUID propertyId);
    ChartOfAccountResponseDTO get(UUID propertyId, UUID accountId);
    ChartOfAccountResponseDTO update(UUID propertyId, UUID accountId, ChartOfAccountRequestDTO request);
    void delete(UUID propertyId, UUID accountId);
    boolean isRevenueLedgerAvailable(UUID propertyId, UUID accountId);
}
