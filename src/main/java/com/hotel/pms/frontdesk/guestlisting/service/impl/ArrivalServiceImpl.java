package com.hotel.pms.frontdesk.guestlisting.service.impl;

import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalResponseDto;
import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.dto.PagedResponse;
import com.hotel.pms.frontdesk.guestlisting.dto.ReservationArrivalDto;
import com.hotel.pms.frontdesk.guestlisting.dto.SyncResultDto;
import com.hotel.pms.frontdesk.guestlisting.entity.ArrivalRecord;
import com.hotel.pms.frontdesk.guestlisting.exception.BadRequestException;
import com.hotel.pms.frontdesk.guestlisting.integration.ReservationServiceClient;
import com.hotel.pms.frontdesk.guestlisting.mapper.ArrivalMapper;
import com.hotel.pms.frontdesk.guestlisting.repository.ArrivalRecordRepository;
import com.hotel.pms.frontdesk.guestlisting.service.ArrivalService;
import com.hotel.pms.frontdesk.guestlisting.spec.ArrivalSpecification;
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
public class ArrivalServiceImpl implements ArrivalService {

    private static final String SYNC_MODE_ALWAYS = "always";
    private static final String SYNC_MODE_CACHE_MISS = "cache-miss";

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "checkInDate", "checkOutDate", "firstName", "lastName", "roomNo",
            "reservationType", "city", "roomStatus", "roomType", "confirmationNumber",
            "company", "guestName"
    );

    @Value("${arrivals.search.sync-mode:always}")
    private String searchSyncMode;

    private final ArrivalRecordRepository arrivalRecordRepository;
    private final ReservationServiceClient reservationServiceClient;
    private final ArrivalMapper arrivalMapper;

    @Override
    @Transactional
    public SyncResultDto syncArrivals(String propertyId, LocalDate businessDate) {
        if (!StringUtils.hasText(propertyId)) {
            throw new BadRequestException("propertyId is required");
        }
        if (businessDate == null) {
            throw new BadRequestException("businessDate is required");
        }

        log.info("Syncing arrivals for propertyId={} businessDate={}", propertyId, businessDate);
        List<ReservationArrivalDto> arrivals = reservationServiceClient.fetchArrivals(propertyId, businessDate);
        int upsertedCount = 0;
        int skippedCount = 0;

        for (ReservationArrivalDto reservationArrival : arrivals) {
            if (!isValidForUpsert(reservationArrival)) {
                skippedCount++;
                continue;
            }

            ArrivalRecord existing = arrivalRecordRepository
                    .findByPropertyIdAndBusinessDateAndConfirmationNumber(
                            propertyId,
                            businessDate,
                            reservationArrival.getConfirmationNumber()
                    )
                    .orElse(null);

            if (existing == null) {
                arrivalRecordRepository.save(arrivalMapper.toEntity(reservationArrival, propertyId, businessDate));
            } else {
                arrivalMapper.updateEntity(existing, reservationArrival);
                arrivalRecordRepository.save(existing);
            }
            upsertedCount++;
        }

        log.info("Arrival sync completed for propertyId={} businessDate={} fetched={} upserted={} skipped={}",
            propertyId, businessDate, arrivals.size(), upsertedCount, skippedCount);

        return SyncResultDto.builder()
            .propertyId(propertyId)
                .businessDate(businessDate)
                .fetchedCount(arrivals.size())
                .upsertedCount(upsertedCount)
                .build();
    }

    @Override
    @Transactional
    public PagedResponse<ArrivalResponseDto> searchArrivals(ArrivalSearchRequestDto request) {
        validateSortBy(request.getSortBy());

        if (shouldSyncBeforeSearch(request.getPropertyId(), request.getBusinessDate())) {
            // Refresh local cache before serving search for the given business date and property.
            syncArrivals(request.getPropertyId(), request.getBusinessDate());
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
            buildSort(request.getSortBy(), direction)
        );

        Page<ArrivalRecord> page = arrivalRecordRepository.findAll(
                ArrivalSpecification.byCriteria(request),
                pageable
        );

        List<ArrivalResponseDto> content = page.getContent().stream()
                .map(arrivalMapper::toResponse)
                .toList();

        return PagedResponse.<ArrivalResponseDto>builder()
            .propertyId(request.getPropertyId())
            .businessDate(request.getBusinessDate())
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

    private boolean shouldSyncBeforeSearch(String propertyId, LocalDate businessDate) {
        String configuredMode = StringUtils.hasText(searchSyncMode)
                ? searchSyncMode.trim().toLowerCase()
                : SYNC_MODE_ALWAYS;

        if (SYNC_MODE_CACHE_MISS.equals(configuredMode)) {
            return !arrivalRecordRepository.existsByPropertyIdAndBusinessDate(propertyId, businessDate);
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
            // Keep guest name sorting stable across duplicate first/last names.
            return Sort.by(new Sort.Order(direction, "lastName"), new Sort.Order(direction, "firstName"));
        }
        return Sort.by(direction, sortBy);
    }

    private boolean isValidForUpsert(ReservationArrivalDto arrival) {
        return arrival != null
                && StringUtils.hasText(arrival.getConfirmationNumber())
                && StringUtils.hasText(arrival.getFirstName())
                && StringUtils.hasText(arrival.getLastName())
                && arrival.getCheckInDate() != null
                && arrival.getCheckOutDate() != null;
    }
}
