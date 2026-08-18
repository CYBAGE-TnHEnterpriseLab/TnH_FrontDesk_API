package com.pms.guestlisting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.dto.DepartureSearchRequestDto;
import com.pms.guestlisting.dto.ReservationArrivalDto;
import com.pms.guestlisting.dto.SyncResultDto;
import com.pms.guestlisting.entity.DepartureRecord;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.guestlisting.integration.ReservationServiceClient;
import com.pms.guestlisting.mapper.DepartureMapper;
import com.pms.guestlisting.repository.DepartureRecordRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DepartureServiceImplTest {

    private static final String PROPERTY_ID = "PROP001";

    @Mock
    private DepartureRecordRepository departureRecordRepository;

    @Mock
    private ReservationServiceClient reservationServiceClient;

    @Mock
    private DepartureMapper departureMapper;

    @InjectMocks
    private DepartureServiceImpl departureService;

    private LocalDate businessDate;

    @BeforeEach
    void setUp() {
        businessDate = LocalDate.of(2026, 5, 28);
    }

    @Test
    void syncDeparturesShouldThrowWhenBusinessDateIsNull() {
        assertThatThrownBy(() -> departureService.syncDepartures(PROPERTY_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("businessDate is required");
    }

    @Test
    void syncDeparturesShouldThrowWhenPropertyIdIsMissing() {
        assertThatThrownBy(() -> departureService.syncDepartures("", businessDate))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("propertyId is required");
    }

    @Test
    void syncDeparturesShouldUpsertValidRecordsAndSkipInvalidRows() {
        ReservationArrivalDto validNew = validDeparture("CNF1001", "John", "Smith");
        ReservationArrivalDto validExisting = validDeparture("CNF1002", "Jane", "Doe");
        ReservationArrivalDto invalid = validDeparture(null, "Bad", "Row");

        DepartureRecord newEntity = DepartureRecord.builder().confirmationNumber("CNF1001").build();
        DepartureRecord existingEntity = DepartureRecord.builder().id(10L).confirmationNumber("CNF1002").build();

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate)).thenReturn(List.of(validNew, validExisting, invalid));
        when(departureRecordRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(PROPERTY_ID, businessDate, "CNF1001"))
                .thenReturn(Optional.empty());
        when(departureRecordRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(PROPERTY_ID, businessDate, "CNF1002"))
                .thenReturn(Optional.of(existingEntity));
        when(departureMapper.toEntity(validNew, PROPERTY_ID, businessDate)).thenReturn(newEntity);

        SyncResultDto result = departureService.syncDepartures(PROPERTY_ID, businessDate);

        assertThat(result.getBusinessDate()).isEqualTo(businessDate);
        assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(result.getFetchedCount()).isEqualTo(3);
        assertThat(result.getUpsertedCount()).isEqualTo(2);

        verify(departureMapper, times(1)).toEntity(validNew, PROPERTY_ID, businessDate);
        verify(departureMapper, times(1)).updateEntity(existingEntity, validExisting);
        verify(departureRecordRepository, times(2)).save(any(DepartureRecord.class));
    }

    @Test
    void syncDeparturesShouldSkipRowsMissingMandatoryGuestOrDateFields() {
        ReservationArrivalDto missingFirstName = validDeparture("CNF2001", null, "Last");
        ReservationArrivalDto missingLastName = validDeparture("CNF2002", "First", null);
        ReservationArrivalDto missingCheckIn = validDeparture("CNF2003", "John", "Smith");
        missingCheckIn.setCheckInDate(null);
        ReservationArrivalDto missingCheckOut = validDeparture("CNF2004", "John", "Smith");
        missingCheckOut.setCheckOutDate(null);
        ReservationArrivalDto valid = validDeparture("CNF2005", "Jane", "Doe");

        DepartureRecord validEntity = DepartureRecord.builder().confirmationNumber("CNF2005").build();

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate))
                .thenReturn(List.of(missingFirstName, missingLastName, missingCheckIn, missingCheckOut, valid));
        when(departureRecordRepository.findByPropertyIdAndBusinessDateAndConfirmationNumber(PROPERTY_ID, businessDate, "CNF2005"))
                .thenReturn(Optional.empty());
        when(departureMapper.toEntity(valid, PROPERTY_ID, businessDate)).thenReturn(validEntity);

        SyncResultDto result = departureService.syncDepartures(PROPERTY_ID, businessDate);

        assertThat(result.getFetchedCount()).isEqualTo(5);
        assertThat(result.getUpsertedCount()).isEqualTo(1);
        verify(departureRecordRepository, times(1)).save(any(DepartureRecord.class));
    }

    @Test
    void searchDeparturesShouldThrowForUnsupportedSortBy() {
        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("unknownField");

        assertThatThrownBy(() -> departureService.searchDepartures(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sortBy field");
    }

    @Test
    void searchDeparturesShouldAlwaysSyncBeforeSearch() {
        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkOutDate");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(departureRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = departureService.searchDepartures(request);

        assertThat(result.getContent()).isEmpty();
        verify(reservationServiceClient, times(1)).fetchDepartures(PROPERTY_ID, businessDate);
    }

        @Test
        void searchDeparturesShouldFallbackToCacheWhenSyncFails() {
        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkOutDate");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate))
            .thenThrow(new ExternalServiceException("Failed to fetch departures from Reservation Service"));
        when(departureRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(true);
        when(departureRecordRepository.findAll(
            org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
            org.mockito.ArgumentMatchers.<Pageable>any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = departureService.searchDepartures(request);

        assertThat(result.getContent()).isEmpty();
        verify(departureRecordRepository, times(1)).existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate);
        }

        @Test
        void searchDeparturesShouldThrowWhenSyncFailsAndCacheMissing() {
        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkOutDate");
        request.setSortDir("asc");

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate))
            .thenThrow(new ExternalServiceException("Failed to fetch departures from Reservation Service"));
        when(departureRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(false);

        assertThatThrownBy(() -> departureService.searchDepartures(request))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("Failed to fetch departures");
        }

    @Test
    void searchDeparturesShouldSkipSyncWhenCacheExistsInCacheMissMode() {
        ReflectionTestUtils.setField(departureService, "searchSyncMode", "cache-miss");

        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkOutDate");
        request.setSortDir("asc");

        when(departureRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(true);
        when(departureRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        departureService.searchDepartures(request);

        verify(reservationServiceClient, never()).fetchDepartures(PROPERTY_ID, businessDate);
    }

    @Test
    void searchDeparturesShouldSyncWhenCacheMissingInCacheMissMode() {
        ReflectionTestUtils.setField(departureService, "searchSyncMode", "cache-miss");

        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkOutDate");
        request.setSortDir("asc");

        when(departureRecordRepository.existsByPropertyIdAndBusinessDate(PROPERTY_ID, businessDate)).thenReturn(false);
        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(departureRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        departureService.searchDepartures(request);

        verify(reservationServiceClient, times(1)).fetchDepartures(PROPERTY_ID, businessDate);
    }

    @Test
    void searchDeparturesShouldMapGuestNameAliasSortToLastAndFirstName() {
        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("guestName");
        request.setSortDir("desc");

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(departureRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        departureService.searchDepartures(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(departureRecordRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
                pageableCaptor.capture()
        );

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("lastName")).isNotNull();
        assertThat(sort.getOrderFor("firstName")).isNotNull();
        assertThat(sort.getOrderFor("lastName").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("firstName").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void searchDeparturesShouldPopulateFilterOptionsWhenRequested() {
        DepartureSearchRequestDto request = new DepartureSearchRequestDto();
        request.setPropertyId(PROPERTY_ID);
        request.setBusinessDate(businessDate);
        request.setSortBy("checkOutDate");
        request.setSortDir("asc");
        request.setIncludeOptions(true);

        when(reservationServiceClient.fetchDepartures(PROPERTY_ID, businessDate)).thenReturn(List.of());
        when(departureRecordRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DepartureRecord>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(departureRecordRepository.findDistinctStatuses(PROPERTY_ID, businessDate)).thenReturn(List.of("DNM"));
        when(departureRecordRepository.findDistinctReservationTypes(PROPERTY_ID, businessDate)).thenReturn(List.of("Guaranteed"));
        when(departureRecordRepository.findDistinctRoomStatuses(PROPERTY_ID, businessDate)).thenReturn(List.of("Clean"));
        when(departureRecordRepository.findDistinctRoomTypes(PROPERTY_ID, businessDate)).thenReturn(List.of("Deluxe King"));
        when(departureRecordRepository.findDistinctFloors(PROPERTY_ID, businessDate)).thenReturn(List.of(3, 4));
        when(departureRecordRepository.findDistinctLoyaltyMembershipStatuses(PROPERTY_ID, businessDate)).thenReturn(List.of("Gold Member"));

        var result = departureService.searchDepartures(request);

        assertThat(result.getFilterOptions()).isNotNull();
        assertThat(result.getFilterOptions().getReservationStatuses()).containsExactly("DNM");
        assertThat(result.getFilterOptions().getRoomStatuses()).containsExactly("Clean");
        assertThat(result.getFilterOptions().getStayTypes()).containsExactly("Guaranteed");
        assertThat(result.getFilterOptions().getRoomTypes()).containsExactly("Deluxe King");
        assertThat(result.getFilterOptions().getFloors()).containsExactly(3, 4);
        assertThat(result.getFilterOptions().getLoyalties()).containsExactly("Gold Member");
        assertThat(result.getFilterOptions().getVips()).containsExactly("Y", "N");
    }

    private ReservationArrivalDto validDeparture(String confirmationNumber, String firstName, String lastName) {
        ReservationArrivalDto dto = new ReservationArrivalDto();
        dto.setConfirmationNumber(confirmationNumber);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setCheckInDate(businessDate.minusDays(2));
        dto.setCheckOutDate(businessDate);
        return dto;
    }
}

