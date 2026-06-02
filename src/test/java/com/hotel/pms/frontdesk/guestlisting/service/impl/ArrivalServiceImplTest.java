package com.hotel.pms.frontdesk.guestlisting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalResponseDto;
import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.dto.ReservationArrivalDto;
import com.hotel.pms.frontdesk.guestlisting.dto.SyncResultDto;
import com.hotel.pms.frontdesk.guestlisting.entity.ArrivalRecord;
import com.hotel.pms.frontdesk.guestlisting.exception.BadRequestException;
import com.hotel.pms.frontdesk.guestlisting.integration.ReservationServiceClient;
import com.hotel.pms.frontdesk.guestlisting.mapper.ArrivalMapper;
import com.hotel.pms.frontdesk.guestlisting.repository.ArrivalRecordRepository;
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
