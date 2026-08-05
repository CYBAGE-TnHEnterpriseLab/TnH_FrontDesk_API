package com.folio.billing.client.impl;

import com.folio.billing.client.ReservationServiceClient;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.GuestDetail;
import com.folio.billing.dto.ReservationSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "integration.reservation-service", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MockReservationServiceClient implements ReservationServiceClient {

    private final List<FolioBillingRow> folioRows = List.of(
            new FolioBillingRow(
                    "Gold",
                    "Doe",
                    "John",
                    "101",
                    "John Doe",
                    "Checked-In",
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 5),
                    4,
                    "Clean",
                    "Deluxe",
                    "CONF-1001"
            ),
            new FolioBillingRow(
                    "Silver",
                    "Brown",
                    "Emily",
                    "205",
                    "Emily Brown",
                    "Reserved",
                    LocalDate.of(2026, 8, 10),
                    LocalDate.of(2026, 8, 12),
                    2,
                    "Pending",
                    "Suite",
                    "CONF-2002"
            )
    );

    private final Map<String, ReservationSummary> summaries = Map.of(
            "CONF-1001",
            new ReservationSummary(
                    "John Doe",
                    "John Doe",
                    "Alice Doe",
                    "CONF-1001",
                    2,
                    1,
                    "Actner Corp",
                    "Direct",
                    "BAR",
                    "Confirmed",
                    "Open",
                    "101",
                    "Deluxe",
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 5),
                    4,
                    "Late checkout requested"
            ),
            "CONF-2002",
            new ReservationSummary(
                    "Emily Brown",
                    "Emily Brown",
                    "",
                    "CONF-2002",
                    1,
                    0,
                    "Blue Star Pvt Ltd",
                    "OTA",
                    "Corporate",
                    "Guaranteed",
                    "Open",
                    "205",
                    "Suite",
                    LocalDate.of(2026, 8, 10),
                    LocalDate.of(2026, 8, 12),
                    2,
                    "Airport pickup needed"
            )
    );

    private final Map<String, List<GuestDetail>> guestDetailsByConfirmation = Map.of(
            "CONF-1001",
            List.of(
                    new GuestDetail(
                            "John Doe",
                            35,
                            "+1-202-555-0101",
                            "john.doe@example.com",
                            "101 Main Street, Springfield",
                            BigDecimal.ZERO
                    ),
                    new GuestDetail(
                            "Alice Doe",
                            31,
                            "+1-202-555-0102",
                            "alice.doe@example.com",
                            "101 Main Street, Springfield",
                            BigDecimal.ZERO
                    )
            ),
            "CONF-2002",
            List.of(
                    new GuestDetail(
                            "Emily Brown",
                            29,
                            "+1-202-555-0201",
                            "emily.brown@example.com",
                            "44 River Road, Lakeside",
                            BigDecimal.ZERO
                    )
            )
    );

    @Override
    public List<FolioBillingRow> searchFolioBilling(FolioBillingFilter filter) {
        return folioRows.stream()
                .filter(row -> matches(filter.roomNumber(), row.room()))
                .filter(row -> matches(filter.guestName(), row.guest()))
                .filter(row -> matches(filter.confirmationNumber(), row.confirmationNo()))
                .filter(row -> matchesActnerCrop(filter.actnerCrop(), row.confirmationNo()))
                .filter(row -> matchesDate(filter.checkInDate(), row.checkIn()))
                .filter(row -> matchesDate(filter.checkOutDate(), row.checkOut()))
                .toList();
    }

    @Override
    public Optional<ReservationSummary> getReservationSummary(String confirmationNo, String roomNo, String guestName) {
        String normalizedConfirmation = normalize(confirmationNo);
        if (hasText(normalizedConfirmation)) {
            return Optional.ofNullable(summaries.get(normalizedConfirmation));
        }

        return folioRows.stream()
                .filter(row -> matches(roomNo, row.room()))
                .filter(row -> matches(guestName, row.guest()))
                .map(FolioBillingRow::confirmationNo)
                .map(summaries::get)
                .findFirst();
    }

    @Override
    public List<GuestDetail> getGuestDetails(String confirmationNo) {
        String normalizedConfirmation = normalize(confirmationNo);
        if (!hasText(normalizedConfirmation)) {
            return List.of();
        }
        return guestDetailsByConfirmation.getOrDefault(normalizedConfirmation, List.of());
    }

    @Override
    public Optional<String> findDefaultConfirmationNo() {
        return summaries.keySet().stream().findFirst();
    }

    private boolean matchesActnerCrop(String actnerCrop, String confirmationNo) {
        if (!hasText(actnerCrop)) {
            return true;
        }
        ReservationSummary summary = summaries.get(confirmationNo);
        if (summary == null) {
            return false;
        }
        return containsIgnoreCase(summary.company(), actnerCrop);
    }

    private boolean matchesDate(LocalDate expected, LocalDate actual) {
        return expected == null || expected.equals(actual);
    }

    private boolean matches(String filterValue, String actualValue) {
        return !hasText(filterValue) || containsIgnoreCase(actualValue, filterValue);
    }

    private boolean containsIgnoreCase(String actualValue, String filterValue) {
        if (actualValue == null || filterValue == null) {
            return false;
        }
        return actualValue.toLowerCase().contains(filterValue.trim().toLowerCase());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
