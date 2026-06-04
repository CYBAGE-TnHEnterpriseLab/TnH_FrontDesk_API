package com.hotel.pms.frontdesk.guestlisting.spec;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.pms.frontdesk.guestlisting.dto.DepartureSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.entity.DepartureRecord;
import com.hotel.pms.frontdesk.guestlisting.repository.DepartureRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class DepartureSpecificationTest {

    @Autowired
    private DepartureRecordRepository departureRecordRepository;

    @Test
    void byCriteriaShouldReturnOnlyRecordsMatchingBusinessDateAsCheckOutDate() {
        LocalDate businessDate = LocalDate.of(2026, 6, 2);

        DepartureRecord matchingDeparture = DepartureRecord.builder()
                .propertyId("PROP001")
                .businessDate(businessDate)
                .checkInDate(businessDate.minusDays(2))
                .checkOutDate(businessDate)
                .status("DUE")
                .salutation("Mr.")
                .firstName("John")
                .lastName("Smith")
                .roomNo("305")
                .reservationType("Guaranteed")
                .city("Mumbai")
                .rateCode("BAR")
                .roomNights(2)
                .roomStatus("Clean")
                .corporateCode("CORP001")
                .roomType("Deluxe King")
                .confirmationNumber("CNF-1001")
                .company("ABC Travels")
                .sharingStatus("Y")
                .floor(3)
                .loyaltyMembershipStatus("Gold Member")
                .sourceLastSyncedAt(LocalDateTime.now())
                .build();

        DepartureRecord nonMatchingDeparture = DepartureRecord.builder()
                .propertyId("PROP001")
                .businessDate(businessDate)
                .checkInDate(businessDate.minusDays(3))
                .checkOutDate(businessDate.plusDays(1))
                .status("DUE")
                .salutation("Ms.")
                .firstName("Jane")
                .lastName("Doe")
                .roomNo("402")
                .reservationType("Guaranteed")
                .city("Mumbai")
                .rateCode("BAR")
                .roomNights(3)
                .roomStatus("Clean")
                .corporateCode("CORP001")
                .roomType("Executive Twin")
                .confirmationNumber("CNF-1002")
                .company("ABC Travels")
                .sharingStatus("N")
                .floor(4)
                .loyaltyMembershipStatus("Silver Member")
                .sourceLastSyncedAt(LocalDateTime.now())
                .build();

        departureRecordRepository.saveAll(List.of(matchingDeparture, nonMatchingDeparture));

        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId("PROP001");
        request.setBusinessDate(businessDate);
        request.setPage(0);
        request.setSize(20);

        var result = departureRecordRepository.findAll(
                DepartureSpecification.byCriteria(request),
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(DepartureRecord::getConfirmationNumber)
                .containsExactly("CNF-1001");
    }
}
