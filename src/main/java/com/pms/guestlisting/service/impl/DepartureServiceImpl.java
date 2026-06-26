package com.pms.guestlisting.service.impl;

import com.pms.guestlisting.dto.DepartureResponseDto;
import com.pms.guestlisting.dto.DepartureSearchRequestDto;
import com.pms.guestlisting.dto.FilterOptionsDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.dto.ReservationArrivalDto;
import com.pms.guestlisting.dto.SyncResultDto;
import com.pms.guestlisting.entity.DepartureRecord;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.guestlisting.integration.ReservationServiceClient;
import com.pms.guestlisting.mapper.DepartureMapper;
import com.pms.guestlisting.repository.DepartureRecordRepository;
import com.pms.guestlisting.service.DepartureService;
import com.pms.guestlisting.spec.DepartureSpecification;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepartureServiceImpl implements DepartureService {

    private static final String SYNC_MODE_ALWAYS = "always";
    private static final String SYNC_MODE_CACHE_MISS = "cache-miss";

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "checkInDate", "checkOutDate", "firstName", "lastName", "roomNo",
            "reservationType", "city", "roomStatus", "roomType", "confirmationNumber",
            "company", "guestName"
    );

    @Value("${departures.search.sync-mode:always}")
    private String searchSyncMode;

    private final DepartureRecordRepository departureRecordRepository;
    private final ReservationServiceClient reservationServiceClient;
    private final DepartureMapper departureMapper;

    @Override
    @Transactional
    public SyncResultDto syncDepartures(String propertyId, LocalDate businessDate) {
        if (!StringUtils.hasText(propertyId)) {
            throw new BadRequestException("propertyId is required");
        }
        if (businessDate == null) {
            throw new BadRequestException("businessDate is required");
        }

        log.info("Syncing departures for propertyId={} businessDate={}", propertyId, businessDate);
        List<ReservationArrivalDto> departures = reservationServiceClient.fetchDepartures(propertyId, businessDate);
        int upsertedCount = 0;
        int skippedCount = 0;

        for (ReservationArrivalDto reservationDeparture : departures) {
            if (!isValidForUpsert(reservationDeparture)) {
                skippedCount++;
                continue;
            }

            DepartureRecord existing = departureRecordRepository
                    .findByPropertyIdAndBusinessDateAndConfirmationNumber(
                            propertyId,
                            businessDate,
                            reservationDeparture.getConfirmationNumber()
                    )
                    .orElse(null);

            if (existing == null) {
                departureRecordRepository.save(departureMapper.toEntity(reservationDeparture, propertyId, businessDate));
            } else {
                departureMapper.updateEntity(existing, reservationDeparture);
                departureRecordRepository.save(existing);
            }
            upsertedCount++;
        }

        log.info("Departure sync completed for propertyId={} businessDate={} fetched={} upserted={} skipped={}",
                propertyId, businessDate, departures.size(), upsertedCount, skippedCount);

        return SyncResultDto.builder()
                .propertyId(propertyId)
                .businessDate(businessDate)
                .fetchedCount(departures.size())
                .upsertedCount(upsertedCount)
                .build();
    }

    @Override
    @Transactional
    public PagedResponse<DepartureResponseDto> searchDepartures(DepartureSearchRequestDto request) {
        validateSortBy(request.getSortBy());

        if (shouldSyncBeforeSearch(request.getPropertyId(), request.getBusinessDate())) {
            try {
                syncDepartures(request.getPropertyId(), request.getBusinessDate());
            } catch (ExternalServiceException ex) {
                boolean hasCachedData = departureRecordRepository.existsByPropertyIdAndBusinessDate(
                        request.getPropertyId(),
                        request.getBusinessDate()
                );
                if (!hasCachedData) {
                    throw ex;
                }
                log.warn("Reservation sync failed for propertyId={} businessDate={}, serving cached departures",
                        request.getPropertyId(), request.getBusinessDate(), ex);
            }
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                buildSort(request.getSortBy(), direction)
        );

        Page<DepartureRecord> page = departureRecordRepository.findAll(
                DepartureSpecification.byCriteria(request),
                pageable
        );

        List<DepartureResponseDto> content = page.getContent().stream()
                .map(departureMapper::toResponse)
                .toList();

        return PagedResponse.<DepartureResponseDto>builder()
                .propertyId(request.getPropertyId())
                .businessDate(request.getBusinessDate())
                .filterOptions(Boolean.TRUE.equals(request.getIncludeOptions())
                        ? buildFilterOptions(request.getPropertyId(), request.getBusinessDate())
                        : null)
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sortBy(request.getSortBy())
                .sortDir(request.getSortDir())
                .build();
    }

    private FilterOptionsDto buildFilterOptions(String propertyId, LocalDate businessDate) {
        return FilterOptionsDto.builder()
                .statuses(departureRecordRepository.findDistinctStatuses(propertyId, businessDate))
                .reservationTypes(departureRecordRepository.findDistinctReservationTypes(propertyId, businessDate))
                .cities(departureRecordRepository.findDistinctCities(propertyId, businessDate))
                .roomStatuses(departureRecordRepository.findDistinctRoomStatuses(propertyId, businessDate))
                .roomTypes(departureRecordRepository.findDistinctRoomTypes(propertyId, businessDate))
                .floors(departureRecordRepository.findDistinctFloors(propertyId, businessDate))
                .companies(departureRecordRepository.findDistinctCompanies(propertyId, businessDate))
                .loyaltyMembershipStatuses(departureRecordRepository.findDistinctLoyaltyMembershipStatuses(propertyId, businessDate))
                .sortFields(List.of("guestName", "roomNo", "checkOutDate", "roomType", "company"))
                .build();
    }

    private boolean shouldSyncBeforeSearch(String propertyId, LocalDate businessDate) {
        String configuredMode = StringUtils.hasText(searchSyncMode)
                ? searchSyncMode.trim().toLowerCase()
                : SYNC_MODE_ALWAYS;

        if (SYNC_MODE_CACHE_MISS.equals(configuredMode)) {
            return !departureRecordRepository.existsByPropertyIdAndBusinessDate(propertyId, businessDate);
        }
        return true;
    }

    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Unsupported sortBy field: " + sortBy);
        }
    }

    private Sort buildSort(String sortBy, Sort.Direction direction) {
        if ("guestName".equals(sortBy)) {
            return Sort.by(new Sort.Order(direction, "lastName"), new Sort.Order(direction, "firstName"));
        }
        return Sort.by(direction, sortBy);
    }

    private boolean isValidForUpsert(ReservationArrivalDto departure) {
        return departure != null
                && StringUtils.hasText(departure.getConfirmationNumber())
                && StringUtils.hasText(departure.getFirstName())
                && StringUtils.hasText(departure.getLastName())
                && departure.getCheckInDate() != null
                && departure.getCheckOutDate() != null;
    }
}

