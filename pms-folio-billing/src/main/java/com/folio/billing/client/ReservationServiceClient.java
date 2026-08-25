package com.folio.billing.client;

import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.GuestDetail;
import com.folio.billing.dto.ReservationSummary;

import java.util.List;
import java.util.Optional;

public interface ReservationServiceClient {

    List<FolioBillingRow> searchFolioBilling(FolioBillingFilter filter);

    Optional<ReservationSummary> getReservationSummary(String confirmationNumber, String roomNo, String guestName);

    List<GuestDetail> getGuestDetails(String confirmationNumber);

    Optional<String> findDefaultConfirmationNumber();

    Optional<String> resolvePropertyId(String confirmationNumber);
}

