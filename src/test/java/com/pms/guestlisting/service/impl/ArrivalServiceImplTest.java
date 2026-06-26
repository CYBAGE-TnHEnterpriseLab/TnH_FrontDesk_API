package com.pms.guestlisting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.dto.ReservationArrivalDto;
import com.pms.guestlisting.dto.SyncResultDto;
import com.pms.guestlisting.entity.ArrivalRecord;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.guestlisting.integration.ReservationServiceClient;
import com.pms.guestlisting.mapper.ArrivalMapper;
import com.pms.guestlisting.repository.ArrivalRecordRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArrivalServiceImplTest {

        private static final String PROPERTY_ID = "PROP001";

    @Mock
    private ArrivalRecordRepository arrivalRecordRepository;

    @Mock
    private ReservationServiceClient reservationServiceClient;

    @Mock
    private ArrivalMapper arrivalMapper;

    @InjectMocks
    private ArrivalServiceImpl arrivalService;

    private LocalDate businessDate;

    @BeforeEach
    void setUp() {
        businessDate = LocalDate.of(2026, 5, 28);
    }

    @Test
        void syncArrivalsShouldThrowWhenBusinessDateIsNull() {
                assertThatThrownBy(() -> arrivalService.syncArrivals(PROPERTY_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("businessDate is required");
    }

        @Test
        void syncArrivalsShouldThrowWhenPropertyIdIsMissing() {
                assertThatThrownBy(() -> arrivalService.syncArrivals("", businessDate))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("propertyId is required");
        }

    @Test
    void syncArrivalsShouldUpsertValidRecordsAndSkipInvalidRows() {
        ReservationArrivalDto validNew = validArrival("CNF1001", "John", "Smith");
        ReservationArrivalDto validExisting = validArrival("CNF1002", "Jane", "Doe");
        ReservationArrivalDto invalid = validArrival(null, "Bad", "Row");

        ArrivalRecord newEntity = ArrivalRecord.builder().confirmationNumber("CNF1001").build();
        ArrivalRecord existingEntity = ArrivalRecord.builder().id(10L).confirmationNumber("CNF1002").build();

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate)).thenReturn(List.of(validNew, validExisting, invalid));
        when(arrivalRecordRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(PROPERTY_ID, businessDate, "CNF1001"))
                .thenReturn(Optional.empty());
        when(arrivalRecordRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(PROPERTY_ID, businessDate, "CNF1002"))
                .thenReturn(Optional.of(existingEntity));
        when(arrivalMapper.toEntity(validNew, PROPERTY_ID, businessDate)).thenReturn(newEntity);

        SyncResultDto result = arrivalService.syncArrivals(PROPERTY_ID, businessDate);

        assertThat(result.getBusinessDate()).isEqualTo(businessDate);
        assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(result.getFetchedCount()).isEqualTo(3);
        assertThat(result.getUpsertedCount()).isEqualTo(2);

        verify(arrivalMapper, times(1)).toEntity(validNew, PROPERTY_ID, businessDate);
        verify(arrivalMapper, times(1)).updateEntity(existingEntity, validExisting);
        verify(arrivalRecordRepository, times(2)).save(any(ArrivalRecord.class));
        verify(arrivalRecordRepository, never())
                .findByPropertyIdAndBusinessDateAndConfirmationNumber(eq(PROPERTY_ID), eq(businessDate), eq((String) null));
    }

    @Test
    void syncArrivalsShouldSkipRowsMissingMandatoryGuestOrDateFields() {
        ReservationArrivalDto missingFirstName = validArrival("CNF2001", null, "Last");
        ReservationArrivalDto missingLastName = validArrival("CNF2002", "First", null);
        ReservationArrivalDto missingCheckIn = validArrival("CNF2003", "John", "Smith");
        missingCheckIn.setCheckInDate(null);
        ReservationArrivalDto missingCheckOut = validArrival("CNF2004", "John", "Smith");
        missingCheckOut.setCheckOutDate(null);
        ReservationArrivalDto valid = validArrival("CNF2005", "Jane", "Doe");

        ArrivalRecord validEntity = ArrivalRecord.builder().confirmationNumber("CNF2005").build();

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate))
                .thenReturn(List.of(missingFirstName, missingLastName, missingCheckIn, missingCheckOut, valid));
        when(arrivalRecordRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(PROPERTY_ID, businessDate, "CNF2005"))
                .thenReturn(Optional.empty());
        when(arrivalMapper.toEntity(valid, PROPERTY_ID, businessDate)).thenReturn(validEntity);

        SyncResultDto result = arrivalService.syncArrivals(PROPERTY_ID, businessDate);

        assertThat(result.getFetchedCount()).isEqualTo(5);
        assertThat(result.getUpsertedCount()).isEqualTo(1);
        verify(arrivalRecordRepository, times(1)).save(any(ArrivalRecord.class));
    }

    @Test
    void searchArrivalsShouldThrowForUnsupportedSortBy() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("unknownField");

        assertThatThrownBy(() -> arrivalService.searchArrivals(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sortBy field");
    }

    @Test
        void searchArrivalsShouldAlwaysSyncBeforeSearch() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkInDate");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(arrivalRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = arrivalService.searchArrivals(request);

        assertThat(result.getContent()).isEmpty();
        verify(reservationServiceClient, times(1)).fetchArrivals(PROPERTY_ID, businessDate);
    }

    @Test
    void searchArrivalsShouldFallbackToCacheWhenSyncFails() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkInDate");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate))
                .thenThrow(new ExternalServiceException("Failed to fetch arrivals from Reservation Service"));
        when(arrivalRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(true);
        when(arrivalRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = arrivalService.searchArrivals(request);

        assertThat(result.getContent()).isEmpty();
        verify(arrivalRecordRepository, times(1)).existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate);
    }

    @Test
    void searchArrivalsShouldThrowWhenSyncFailsAndCacheMissing() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkInDate");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate))
                .thenThrow(new ExternalServiceException("Failed to fetch arrivals from Reservation Service"));
        when(arrivalRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(false);

        assertThatThrownBy(() -> arrivalService.searchArrivals(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to fetch arrivals");
    }

    @Test
    void searchArrivalsShouldReturnMappedPagedResponse() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkInDate");
        request.setSortDir("desc");
        request.setPage(0);
        request.setSize(20);

        ArrivalRecord record = ArrivalRecord.builder()
                .id(1L)
                .confirmationNumber("CNF458721")
                .firstName("John")
                .lastName("Smith")
                .build();

        ArrivalResponseDto responseDto = ArrivalResponseDto.builder()
                .id(1L)
                .confirmationNumber("CNF458721")
                .firstName("John")
                .lastName("Smith")
                .build();

        when(arrivalRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1));
        when(arrivalMapper.toResponse(record)).thenReturn(responseDto);

        var result = arrivalService.searchArrivals(request);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getConfirmationNumber()).isEqualTo("CNF458721");
                verify(reservationServiceClient, times(1)).fetchArrivals(PROPERTY_ID, businessDate);
    }

    @Test
    void searchArrivalsShouldSupportCompanySort() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("company");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(arrivalRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        arrivalService.searchArrivals(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(arrivalRecordRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("company")).isNotNull();
    }

    @Test
    void searchArrivalsShouldMapGuestNameAliasSortToLastAndFirstName() {
        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("guestName");
        request.setSortDir("desc");

        when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(arrivalRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        arrivalService.searchArrivals(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(arrivalRecordRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                pageableCaptor.capture()
        );

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("lastName")).isNotNull();
        assertThat(sort.getOrderFor("firstName")).isNotNull();
        assertThat(sort.getOrderFor("lastName").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("firstName").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

        @Test
        void searchArrivalsShouldPopulateFilterOptionsWhenRequested() {
                ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
                request.setPropertyId(PROPERTY_ID);
                request.setBusinessDate(businessDate);
                request.setSortBy("checkInDate");
                request.setSortDir("asc");
                request.setIncludeOptions(true);

                when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate)).thenReturn(List.of());
                when(arrivalRecordRepository.findAll(
                                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                                org.mockito.ArgumentMatchers.<Pageable>any()))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
                when(arrivalRecordRepository.findDistinctStatuses(PROPERTY_ID, businessDate)).thenReturn(List.of("DNM"));
                when(arrivalRecordRepository.findDistinctReservationTypes(PROPERTY_ID, businessDate)).thenReturn(List.of("Guaranteed"));
                when(arrivalRecordRepository.findDistinctCities(PROPERTY_ID, businessDate)).thenReturn(List.of("Mumbai"));
                when(arrivalRecordRepository.findDistinctRoomStatuses(PROPERTY_ID, businessDate)).thenReturn(List.of("Clean"));
                when(arrivalRecordRepository.findDistinctRoomTypes(PROPERTY_ID, businessDate)).thenReturn(List.of("Deluxe King"));
                when(arrivalRecordRepository.findDistinctFloors(PROPERTY_ID, businessDate)).thenReturn(List.of(3, 4));
                when(arrivalRecordRepository.findDistinctCompanies(PROPERTY_ID, businessDate)).thenReturn(List.of("ABC Travels"));
                when(arrivalRecordRepository.findDistinctLoyaltyMembershipStatuses(PROPERTY_ID, businessDate)).thenReturn(List.of("Gold Member"));

                var result = arrivalService.searchArrivals(request);

                assertThat(result.getFilterOptions()).isNotNull();
                assertThat(result.getFilterOptions().getStatuses()).containsExactly("DNM");
                assertThat(result.getFilterOptions().getRoomTypes()).containsExactly("Deluxe King");
                assertThat(result.getFilterOptions().getFloors()).containsExactly(3, 4);
                assertThat(result.getFilterOptions().getSortFields()).containsExactly("guestName", "roomNo", "checkInDate", "roomType", "company");
        }

        @Test
        void searchArrivalsShouldSkipSyncWhenCacheExistsInCacheMissMode() {
                ReflectionTestUtils.setField(arrivalService, "searchSyncMode", "cache-miss");

                ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
                request.setPropertyId(PROPERTY_ID);
                request.setBusinessDate(businessDate);
                request.setSortBy("checkInDate");
                request.setSortDir("asc");

                when(arrivalRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(true);
                when(arrivalRecordRepository.findAll(
                                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                                org.mockito.ArgumentMatchers.<Pageable>any()))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

                arrivalService.searchArrivals(request);

                verify(reservationServiceClient, never()).fetchArrivals(PROPERTY_ID, businessDate);
        }

        @Test
        void searchArrivalsShouldSyncWhenCacheMissingInCacheMissMode() {
                ReflectionTestUtils.setField(arrivalService, "searchSyncMode", "cache-miss");

                ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
                request.setPropertyId(PROPERTY_ID);
                request.setBusinessDate(businessDate);
                request.setSortBy("checkInDate");
                request.setSortDir("asc");

                when(arrivalRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(false);
                when(reservationServiceClient.fetchArrivals(PROPERTY_ID, businessDate)).thenReturn(List.of());
                when(arrivalRecordRepository.findAll(
                                org.mockito.ArgumentMatchers.<Specification<ArrivalRecord>>any(),
                                org.mockito.ArgumentMatchers.<Pageable>any()))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

                arrivalService.searchArrivals(request);

                verify(reservationServiceClient, times(1)).fetchArrivals(PROPERTY_ID, businessDate);
        }

    private ReservationArrivalDto validArrival(String confirmationNumber, String firstName, String lastName) {
        ReservationArrivalDto dto = new ReservationArrivalDto();
        dto.setConfirmationNumber(confirmationNumber);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setCheckInDate(businessDate);
        dto.setCheckOutDate(businessDate.plusDays(2));
        return dto;
    }
}

