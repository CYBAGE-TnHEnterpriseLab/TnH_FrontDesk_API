package com.pms.guestlisting.spec;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.entity.ArrivalRecord;
import com.pms.guestlisting.repository.ArrivalRecordRepository;
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
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
        request.setPropertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
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

    @Test
    void byCriteriaShouldApplyAllOptionalFiltersAndSearch() {
        LocalDate businessDate = LocalDate.of(2026, 6, 2);

        ArrivalRecord matching = ArrivalRecord.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
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
                .confirmationNumber("CNF-ABC-123")
                .company("ABC Travels")
                .sharingStatus("Y")
                .floor(3)
                .loyaltyMembershipStatus("Gold Member")
                .sourceLastSyncedAt(LocalDateTime.now())
                .build();

        ArrivalRecord nonMatching = ArrivalRecord.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .businessDate(businessDate)
                .checkInDate(businessDate)
                .checkOutDate(businessDate.plusDays(1))
                .status("ARR")
                .salutation("Ms.")
                .firstName("Jane")
                .lastName("Doe")
                .roomNo("999")
                .reservationType("Non-Guaranteed")
                .city("Pune")
                .rateCode("WKND")
                .roomNights(1)
                .roomStatus("Dirty")
                .corporateCode("CORP999")
                .roomType("Standard")
                .confirmationNumber("CNF-XYZ-999")
                .company("Other Co")
                .sharingStatus("N")
                .floor(9)
                .loyaltyMembershipStatus("Blue")
                .sourceLastSyncedAt(LocalDateTime.now())
                .build();

        arrivalRecordRepository.saveAll(List.of(matching, nonMatching));

        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        request.setBusinessDate(businessDate);
        request.setStatus("DNM");
        request.setReservationType("Guaranteed");
        request.setCity("mum");
        request.setRoomStatus("Clean");
        request.setCorporateCode("CORP001");
        request.setRoomType("deluxe");
        request.setFloor(3);
        request.setCompany("abc");
        request.setSharingStatus("Y");
        request.setLoyaltyMembershipStatus("gold");
        request.setSearch("abc-123");
        request.setPage(0);
        request.setSize(20);

        var result = arrivalRecordRepository.findAll(
                ArrivalSpecification.byCriteria(request),
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(ArrivalRecord::getConfirmationNumber)
                .containsExactly("CNF-ABC-123");
    }
}

