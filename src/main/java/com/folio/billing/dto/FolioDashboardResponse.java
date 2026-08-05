package com.folio.billing.dto;

import java.util.List;

public record FolioDashboardResponse(
        List<FolioTransactionRow> folioADashboard,
        List<GuestDetail> guestDetails
) {
}
