package com.hotel.pms.frontdesk.guestlisting.spec;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.entity.ArrivalRecord;
import com.hotel.pms.frontdesk.guestlisting.repository.ArrivalRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ArrivalSpecificationTest {

    @Autowired
    private ArrivalRecordRepository arrivalRecordRepository;

    @Test
    void byCriteriaShouldReturnOnlyRecordsMatchingBusinessDateAsCheckInDate() {
        LocalDate businessDate = LocalDate.of(2026, 6, 2);

        ArrivalRecord matchingArrival = ArrivalRecord.builder()
                .propertyId("PROP001")
                .businessDate(businessDate)
                .checkInDate(businessDate)
                .checkOutDate(businessDate.plusDays(2))
                .status("DNM")
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

        ArrivalRecord nonMatchingArrival = ArrivalRecord.builder()
                .propertyId("PROP001")
                .businessDate(businessDate)
                .checkInDate(businessDate.plusDays(1))
                .checkOutDate(businessDate.plusDays(3))
                .status("DNM")
                .salutation("Ms.")
                .firstName("Jane")
                .lastName("Doe")
                .roomNo("402")
                .reservationType("Guaranteed")
                .city("Mumbai")
                .rateCode("BAR")
                .roomNights(2)
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

        arrivalRecordRepository.saveAll(List.of(matchingArrival, nonMatchingArrival));

        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId("PROP001");
        request.setBusinessDate(businessDate);
        request.setPage(0);
        request.setSize(20);

        var result = arrivalRecordRepository.findAll(
                ArrivalSpecification.byCriteria(request),
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(ArrivalRecord::getConfirmationNumber)
                .containsExactly("CNF-1001");
    }
}
