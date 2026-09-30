package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.constant.PaymentModes;
import com.pms.reservation.constant.PaymentTypes;
import com.pms.reservation.constant.IdTypes;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.dto.HousekeepingSyncResponse;
import com.pms.reservation.dto.ReservationRoomBookingSummaryDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.integration.HousekeepingRoomStatusClient;
import com.pms.reservation.integration.FolioServiceClient;
import com.pms.reservation.integration.dto.InventoryReservationRequest;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import com.pms.reservation.service.ReservationBookingService;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationBookingServiceImpl implements ReservationBookingService {

    private static final String RESERVATION_STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_ARRIVED = "ARRIVED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    private static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String DEFAULT_CURRENCY = "INR";
    private static final int CONFIRMATION_MIN = 1000000000;
    private static final int CONFIRMATION_MAX_EXCLUSIVE = 2000000000;
    private static final int CONFIRMATION_MAX_ATTEMPTS = 50;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationPaymentTransactionRepository reservationPaymentTransactionRepository;
    private final HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;
    private final PropertyInventoryPort propertyInventoryPort;
    private final InventoryServiceClient inventoryServiceClient;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final ReservationBookingMapper reservationBookingMapper;
    private final PaymentProcessingService paymentProcessingService;
    private final HousekeepingRoomStatusClient housekeepingRoomStatusClient;
    private final HousekeepingRoomCalendarClient housekeepingRoomCalendarClient;
    private final FolioServiceClient folioServiceClient;

    @Override
    @Transactional
    public ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request) {
        normalizeCreateRequest(request);
        validatePhoneNumberFormat(request.getPhoneNumber());
        validateDates(request.getArrivalDate(), request.getDepartureDate());
        validateRequiredContactFields(request);
        validateRoomSelectionAndGuestNames(request);
        validateAndNormalizePaymentMode(request);
        validateAndNormalizePaymentType(request);

        if (StringUtils.hasText(request.getAssignedRoomNo())
                && StringUtils.hasText(request.getGuestName())
                && request.getArrivalDate() != null
                && request.getDepartureDate() != null) {
            boolean duplicate = reservationBookingRepository.existsActiveBookingForGuestRoomAndDates(
                    request.getPropertyId(),
                    request.getGuestName().trim(),
                    request.getAssignedRoomNo().trim(),
                    request.getArrivalDate(),
                    request.getDepartureDate()
            );
            if (duplicate) {
                throw new BadRequestException(
                        "An active booking already exists for this guest in room "
                                + request.getAssignedRoomNo().trim()
                                + " from " + request.getArrivalDate()
                                + " to " + request.getDepartureDate()
                                + ". Use the edit option to update the existing booking.");
            }
        }

        int roomCount = request.getNumberOfRooms();
        String confirmationNumber = generateConfirmationNumber(request.getPropertyId());

        String roomTypeId = resolveRoomTypeId(request.getPropertyId(), request.getRoomType());
        InventoryReservationRequest inventoryRequest = InventoryReservationRequest.builder()
            .confirmationNumber(confirmationNumber)
            .propertyId(request.getPropertyId())
            .bookedRoomTypeId(roomTypeId)
            .assignedRoomTypeId(roomTypeId)
            .checkInDate(request.getArrivalDate())
            .checkOutDate(request.getDepartureDate())
            .quantity(request.getNumberOfRooms())
            .build();
        inventoryServiceClient.reserve(inventoryRequest);

        BigDecimal payableAmount = calculatePayableAmount(request);
        PaymentProcessingResult paymentResult = paymentProcessingService.processPayment(request, confirmationNumber, payableAmount);
        if (!PAYMENT_STATUS_SUCCESS.equalsIgnoreCase(paymentResult.getStatus())) {
            String failureReason = paymentResult.getFailureReason() == null
                    ? "payment processing failed"
                    : paymentResult.getFailureReason();
            inventoryServiceClient.release(confirmationNumber);
            throw new BadRequestException("payment processing failed: " + failureReason);
        }

        List<String> roomGuestNames = request.getGuestNames();
        List<ReservationBookingRecord> savedBookings = new ArrayList<>();
        for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
            ReservationBookingRequestDto roomRequest = requestForRoom(
                    request, roomGuestNames.get(roomIndex), roomCount > 1);
            ReservationBookingRecord entity = reservationBookingMapper.toEntity(roomRequest);
            populateRoomFloor(entity);
            applyPropertyTaxOnBooking(entity);
            entity.setConfirmationNumber(confirmationNumber);
            entity.setReservationStatus(RESERVATION_STATUS_CONFIRMED);
            entity.setInventoryDeductedAt(LocalDateTime.now());
            entity.setInventorySyncedAt(LocalDateTime.now());
            savedBookings.add(reservationBookingRepository.save(entity));
        }

        ReservationBookingRecord saved = savedBookings.get(0);
        ReservationPaymentTransactionRecord savedPaymentTransaction = reservationPaymentTransactionRepository
            .save(buildPaymentTransaction(saved, request, payableAmount, paymentResult));
        savedBookings.forEach(this::updateStandaloneHousekeeping);
        return reservationBookingMapper.toResponse(saved, savedPaymentTransaction).toBuilder()
            .roomBookings(toRoomBookingSummaries(savedBookings))
            .build();
    }

private ReservationBookingRequestDto requestForRoom(
        ReservationBookingRequestDto source,
        String guestName,
        boolean clearAssignedRoom
) {
    ReservationBookingRequestDto copy = new ReservationBookingRequestDto();
    org.springframework.beans.BeanUtils.copyProperties(source, copy);

    copy.setGuestName(guestName);
    copy.setGuestNames(List.of(guestName));
    copy.setNumberOfRooms(1);
    if (clearAssignedRoom) {
        copy.setAssignedRoomNo(null);
        copy.setFloor(null);
    }
    return copy;
}

    @Override
    public HousekeepingSyncResponse syncHousekeepingStatuses() {
        int processed = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        for (ReservationBookingRecord booking : reservationBookingRepository.findAll()) {
            boolean confirmed = RESERVATION_STATUS_CONFIRMED.equalsIgnoreCase(booking.getReservationStatus());
            boolean checkedIn = STATUS_CHECKED_IN.equalsIgnoreCase(booking.getReservationStatus());
            if ((!confirmed && !checkedIn)
                    || !StringUtils.hasText(booking.getAssignedRoomNo())) {
                skipped++;
                continue;
            }
            processed++;
            try {
                String propertyId = booking.getPropertyId();
                if (checkedIn) {
                    housekeepingRoomStatusClient.updateCheckedInStay(UUID.fromString(propertyId), booking.getArrivalDate(),
                            booking.getDepartureDate(), booking.getAssignedRoomNo(), booking.getGuestName(),
                            booking.getConfirmationNumber());
                } else {
                    housekeepingRoomStatusClient.updateReservationStay(UUID.fromString(propertyId), booking.getArrivalDate(),
                            booking.getDepartureDate(), booking.getAssignedRoomNo(), booking.getGuestName(),
                            booking.getConfirmationNumber());
                }
                updated++;
            } catch (ExternalServiceException ex) {
                failed++;
                log.warn("Housekeeping sync failed for confirmation {}", booking.getConfirmationNumber(), ex);
            }
        }
        return new HousekeepingSyncResponse(processed, updated, skipped, failed);
    }

    private void updateStandaloneHousekeeping(ReservationBookingRecord booking) {
        if (!StringUtils.hasText(booking.getAssignedRoomNo())) {
            return;
        }
        try {
            String propertyId = booking.getPropertyId();
            if (STATUS_CHECKED_IN.equalsIgnoreCase(booking.getReservationStatus())) {
                housekeepingRoomStatusClient.updateCheckedInStay(
                        UUID.fromString(propertyId), booking.getArrivalDate(), booking.getDepartureDate(),
                        booking.getAssignedRoomNo(), booking.getGuestName(), booking.getConfirmationNumber());
            } else {
                housekeepingRoomStatusClient.updateReservationStay(
                        UUID.fromString(propertyId), booking.getArrivalDate(), booking.getDepartureDate(),
                        booking.getAssignedRoomNo(), booking.getGuestName(), booking.getConfirmationNumber());
            }

        } catch (IllegalArgumentException ex) {
            throw new ExternalServiceException(
                    "Cannot synchronize the assigned room with Housekeeping because propertyId is not a UUID: "
                            + booking.getPropertyId(), ex);

        } catch (ExternalServiceException ex) {
            throw ex;
        }
    }

    private void populateRoomFloor(ReservationBookingRecord booking) {
        if (!StringUtils.hasText(booking.getAssignedRoomNo())) {
            booking.setFloor(null);
            return;
        }
        booking.setFloor(housekeepingRoomCalendarClient.findRoomFloor(
            booking.getPropertyId(),
            booking.getAssignedRoomNo(),
            booking.getArrivalDate(),
            booking.getDepartureDate()));
    }

    @Override
    @Transactional
    public ReservationViewResponseDto updateBooking(String confirmationNumber, ReservationBookingRequestDto request) {
        return updateBooking(confirmationNumber, null, request);
    }

    private ReservationBookingRecord fetchExistingBooking(String confirmationNumber, Long bookingId) {
        if (bookingId == null) {
            List<ReservationBookingRecord> matches = reservationBookingRepository
                .findByConfirmationNumber(confirmationNumber);
            if (matches.size() > 1) {
                throw new BadRequestException("bookingId is required when a confirmation has multiple rooms");
            }
            return matches.stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        }
        return reservationBookingRepository.findByIdAndConfirmationNumber(bookingId, confirmationNumber)
            .orElseThrow(() -> new BadRequestException("Reservation room booking not found"));
    }

    @Override
    @Transactional
    public ReservationViewResponseDto updateBooking(
            String confirmationNumber,
            Long bookingId,
            ReservationBookingRequestDto request
    ) {
        ReservationBookingRecord existing = fetchExistingBooking(confirmationNumber, bookingId);

        if (request.getPhoneNumber() != null) {
            validatePhoneNumberFormat(request.getPhoneNumber());
        }

        if (request.getArrivalDate() != null || request.getDepartureDate() != null) {
            LocalDate arrival = request.getArrivalDate() != null ? request.getArrivalDate() : existing.getArrivalDate();
            LocalDate departure = request.getDepartureDate() != null ? request.getDepartureDate() : existing.getDepartureDate();
            validateDates(arrival, departure);
        }

        if (request.getGuestNames() != null) {
            validateRoomSelectionAndGuestNames(request);
        }

        String previousRoomNumber = existing.getAssignedRoomNo();
        LocalDate previousArrivalDate = existing.getArrivalDate();
        LocalDate previousDepartureDate = existing.getDepartureDate();
        String previousRoomTypeId = resolveRoomTypeId(existing.getPropertyId(), existing.getRoomType());
        BigDecimal originalTotalRate = existing.getTotalRate() != null ? existing.getTotalRate() : BigDecimal.ZERO;
        BigDecimal originalGuestBalance = existing.getGuestBalance() != null ? existing.getGuestBalance() : BigDecimal.ZERO;

        ReservationBookingRecord updated = reservationBookingMapper.mergeEntity(existing, request);

        if (request.getAssignedRoomNo() != null && !Boolean.TRUE.equals(existing.getDnm())) {
            updated.setAssignedRoomNo(request.getAssignedRoomNo().trim());
            populateRoomFloor(updated);
        }

        applyPropertyTaxOnBooking(updated);

        if (STATUS_CHECKED_OUT.equalsIgnoreCase(updated.getReservationStatus())) {
            throw new BadRequestException("Checked-out reservations cannot be changed. Cancel the same-day check-out to re-check in the guest first");
        }

        validateDnmRoomLock(existing, request);

        boolean roomNumberChanged = roomNumberChanged(previousRoomNumber, updated.getAssignedRoomNo());
        if (roomNumberChanged) {
            String assignedRoomTypeId = resolveAssignedRoomTypeId(
                existing.getPropertyId(), updated.getAssignedRoomNo(), updated.getArrivalDate(), updated.getDepartureDate());
            if (assignedRoomTypeId == null) {
                throw new BadRequestException(
                        "The newly assigned room is not available in Housekeeping for the selected stay dates");
            }
            if (!assignedRoomTypeId.equals(previousRoomTypeId)) {
                inventoryServiceClient.changeAssignedRoomType(existing.getConfirmationNumber(), assignedRoomTypeId);
            }
        }

        ReservationBookingRecord saved = reservationBookingRepository.save(updated);
        boolean departureShortened = departureDateShortened(previousDepartureDate, saved.getDepartureDate());
        boolean departureExtended = previousDepartureDate != null && saved.getDepartureDate() != null
                && saved.getDepartureDate().isAfter(previousDepartureDate);

        if (roomNumberChanged) {
            clearPreviousHousekeepingStay(
                    existing.getPropertyId(),
                    existing.getConfirmationNumber(),
                    previousRoomNumber,
                    previousArrivalDate,
                    previousDepartureDate);
        }

        boolean needHousekeepingUpdate = roomNumberChanged || (!departureShortened && departureExtended);
        if (needHousekeepingUpdate && StringUtils.hasText(saved.getAssignedRoomNo())) {
            updateStandaloneHousekeeping(saved);
        }

        if (departureShortened) {
            handleShortenedDeparture(
                    existing.getPropertyId(),
                    existing.getConfirmationNumber(),
                    saved.getAssignedRoomNo(),
                    previousArrivalDate,
                    previousDepartureDate,
                    saved.getDepartureDate(),
                    saved.getRoomType(),
                    saved.getNumberOfRooms());

            BigDecimal newTotalRate = saved.getTotalRate() != null ? saved.getTotalRate() : BigDecimal.ZERO;
            BigDecimal amountPaid = originalTotalRate.subtract(originalGuestBalance);
            BigDecimal newGuestBalance = newTotalRate.subtract(amountPaid);

            saved.setGuestBalance(newGuestBalance);
            reservationBookingRepository.save(saved);
        } else if (departureExtended) {
            handleExtendedDeparture(
                    existing.getPropertyId(), existing.getConfirmationNumber(), saved,
                    previousDepartureDate);

            BigDecimal newTotalRate = saved.getTotalRate() != null ? saved.getTotalRate() : BigDecimal.ZERO;
            BigDecimal amountPaid = originalTotalRate.subtract(originalGuestBalance);
            BigDecimal newGuestBalance = newTotalRate.subtract(amountPaid);

            saved.setGuestBalance(newGuestBalance);
            reservationBookingRepository.save(saved);
        }

        if (departureShortened || departureExtended) {
            try {
                folioServiceClient.adjustReservationCharge(
                        saved.getConfirmationNumber(),
                        existing.getId(),
                        originalTotalRate,
                        saved.getTotalRate(),
                        "reservation-service"
                );
            } catch (Exception ex) {
                log.warn("Folio adjustment failed for confirmationNumber={}", saved.getConfirmationNumber(), ex);
            }
        }

        Optional<ReservationPaymentTransactionRecord> latestTransaction =
            reservationPaymentTransactionRepository.findTopByBookingIdOrderByCreatedAtDesc(existing.getId());
        return buildReservationViewResponse(saved, latestTransaction.orElse(null));
    }

    private boolean roomNumberChanged(String previousRoomNumber, String newRoomNumber) {
        String previous = previousRoomNumber == null ? "" : previousRoomNumber.trim();
        String current = newRoomNumber == null ? "" : newRoomNumber.trim();
        return !previous.equalsIgnoreCase(current);
    }

    private boolean departureDateShortened(LocalDate previousDepartureDate, LocalDate newDepartureDate) {
        if (previousDepartureDate == null || newDepartureDate == null) {
            return false;
        }
        return newDepartureDate.isBefore(previousDepartureDate);
    }

    private void handleShortenedDeparture(
            String propertyIdValue,
            String confirmationNumber,
            String roomNumber,
            LocalDate arrivalDate,
            LocalDate previousDepartureDate,
            LocalDate newDepartureDate, String roomType, Integer numberOfRooms) {
        try {
            UUID propertyId = UUID.fromString(propertyIdValue);
            if (StringUtils.hasText(roomNumber)) {
                housekeepingRoomStatusClient.clearReservationStay(
                        propertyId,
                        newDepartureDate.plusDays(1),
                        previousDepartureDate,
                        roomNumber);
                housekeepingRoomStatusClient.updateDepartureStatus(
                        propertyId,
                        newDepartureDate,
                        arrivalDate,
                        newDepartureDate,
                        roomNumber,
                        confirmationNumber);
            }
            String roomTypeId = resolveRoomTypeId(propertyIdValue, roomType);
            inventoryServiceClient.release(confirmationNumber);
            if (!newDepartureDate.isBefore(arrivalDate)) {
                int quantity = numberOfRooms != null && numberOfRooms > 0 ? numberOfRooms : 1;
                InventoryReservationRequest inventoryRequest = InventoryReservationRequest.builder()
                        .confirmationNumber(confirmationNumber)
                        .propertyId(propertyIdValue)
                        .bookedRoomTypeId(roomTypeId)
                        .assignedRoomTypeId(roomTypeId)
                        .checkInDate(arrivalDate)
                        .checkOutDate(newDepartureDate)
                        .quantity(quantity)
                        .build();
                inventoryServiceClient.reserve(inventoryRequest);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Cannot release shortened departure because propertyId is not a UUID: {}", propertyIdValue, ex);
        } catch (Exception ex) {
            log.warn("Failed to sync shortened departure for confirmationNumber={}", confirmationNumber, ex);
        }
    }

    private void handleExtendedDeparture(
            String propertyIdValue,
            String confirmationNumber,
            ReservationBookingRecord saved,
            LocalDate previousDepartureDate) {
        try {
            if (StringUtils.hasText(saved.getAssignedRoomNo())) {
                updateStandaloneHousekeeping(saved);
            }
            String roomTypeId = resolveRoomTypeId(propertyIdValue, saved.getRoomType());
            inventoryServiceClient.release(confirmationNumber);
            int quantity = saved.getNumberOfRooms() != null && saved.getNumberOfRooms() > 0 ? saved.getNumberOfRooms() : 1;
            InventoryReservationRequest inventoryRequest = InventoryReservationRequest.builder()
                    .confirmationNumber(confirmationNumber)
                    .propertyId(propertyIdValue)
                    .bookedRoomTypeId(roomTypeId)
                    .assignedRoomTypeId(roomTypeId)
                    .checkInDate(saved.getArrivalDate())
                    .checkOutDate(saved.getDepartureDate())
                    .quantity(quantity)
                    .build();
            inventoryServiceClient.reserve(inventoryRequest);
        } catch (IllegalArgumentException ex) {
            log.warn("Cannot extend departure because propertyId is not a UUID: {}", propertyIdValue, ex);
        } catch (Exception ex) {
            log.warn("Failed to sync extended departure for confirmationNumber={}", confirmationNumber, ex);
        }
    }

    private String resolveAssignedRoomTypeId(String propertyId, String roomNumber,
                                             LocalDate arrivalDate, LocalDate departureDate) {
        if (!StringUtils.hasText(roomNumber)) {
            return null;
        }
        String roomTypeName = housekeepingRoomCalendarClient.fetchRooms(propertyId, arrivalDate, departureDate).stream()
                .filter(room -> roomNumber.trim().equalsIgnoreCase(room.getRoomNumber()))
                .map(room -> room.getRoomType())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        return StringUtils.hasText(roomTypeName)
                ? resolveRoomTypeId(propertyId, roomTypeName)
                : null;
    }


    private void clearPreviousHousekeepingStay(
            String propertyIdValue,
            String confirmationNumber,
            String previousRoomNumber,
            LocalDate arrivalDate,
            LocalDate departureDate) {
        try {
            UUID propertyId = UUID.fromString(propertyIdValue);
            if (StringUtils.hasText(previousRoomNumber)) {
                housekeepingRoomStatusClient.clearReservationAssignment(
                    propertyId,
                    confirmationNumber,
                    previousRoomNumber,
                    arrivalDate,
                    departureDate);
            }
        } catch (IllegalArgumentException ex) {
            throw new ExternalServiceException(
                    "Cannot release the previous housekeeping room because propertyId is not a UUID: "
                            + propertyIdValue, ex);
        } catch (ExternalServiceException ex) {
            throw ex;
        }
    }

    /* private void clearHousekeepingAssignments(ReservationBookingRecord booking) {
        housekeepingRoomStatusClient.clearReservationAssignments(
                booking.getPropertyId(), booking.getConfirmationNumber());
    } */

        @Override
        @Transactional(readOnly = true)
        public ReservationViewResponseDto getBookingDetails(String confirmationNumber) {
        ReservationBookingRecord booking = reservationBookingRepository.findByConfirmationNumberOrderByIdAsc(confirmationNumber)
            .stream()
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Reservation booking not found"));

        return buildReservationViewForBooking(booking);
        }

        @Override
        @Transactional(readOnly = true)
        public ReservationViewResponseDto searchBooking(
            String confirmationNumber,
            Long bookingId,
            String propertyId,
            String phoneNumber,
            String email) {
        if (!StringUtils.hasText(confirmationNumber)
            && bookingId == null
            && !StringUtils.hasText(phoneNumber)
            && !StringUtils.hasText(email)) {
            throw new BadRequestException(
                "At least one of confirmationNumber, bookingId, phoneNumber, or email is required");
        }

        ReservationBookingRecord booking;
        if (StringUtils.hasText(confirmationNumber)) {
            booking = reservationBookingRepository.findByConfirmationNumberOrderByIdAsc(confirmationNumber.trim())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        } else if (bookingId != null) {
                booking = reservationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        } else if (StringUtils.hasText(phoneNumber)) {
            requirePropertyIdForContactSearch(propertyId);
            String normalizedPhoneNumber = phoneNumber.trim();
            booking = reservationBookingRepository.findFirstByPropertyIdAndPhoneNumberOrMobileNumber(
                propertyId.trim(), normalizedPhoneNumber, normalizedPhoneNumber)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        } else {
            requirePropertyIdForContactSearch(propertyId);
            String normalizedEmail = email.trim();
            booking = reservationBookingRepository.findFirstByPropertyIdAndPersonalEmailOrOfficialEmail(
                propertyId.trim(), normalizedEmail, normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        }

        return buildReservationViewForBooking(booking);
        }

        @Override
        @Transactional(readOnly = true)
        public ReservationViewResponseDto getBookingDetails(String confirmationNumber, Long bookingId) {
        ReservationBookingRecord booking = reservationBookingRepository.findById(bookingId)
            .filter(item -> confirmationNumber.equals(item.getConfirmationNumber()))
            .orElseThrow(() -> new BadRequestException("Room booking not found for confirmation number"));

        return buildReservationViewForBooking(booking);
        }

        private void requirePropertyIdForContactSearch(String propertyId) {
            if (!StringUtils.hasText(propertyId)) {
                throw new BadRequestException("propertyId is required when searching by phone number or email");
            }
        }

        private ReservationViewResponseDto buildReservationViewForBooking(ReservationBookingRecord booking) {
        Optional<ReservationPaymentTransactionRecord> latestTransaction = booking.getId() == null
            ? Optional.empty()
            : reservationPaymentTransactionRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId());
        return buildReservationViewResponse(booking, latestTransaction.orElse(null));
        }

    private ReservationViewResponseDto buildReservationViewResponse(
            ReservationBookingRecord booking,
            ReservationPaymentTransactionRecord paymentTransaction
    ) {
        ReservationBookingResponseDto baseResponse = reservationBookingMapper.toResponse(booking, paymentTransaction);
        LocalDate businessDate = resolveBusinessDate(booking);
        TaxSummary taxSummary = calculateTaxSummary(booking);
        BigDecimal folioBalance = folioServiceClient.getFolioBalance(booking.getConfirmationNumber());

        return ReservationViewResponseDto.builder()
                .reservationId(booking.getConfirmationNumber())
                .confirmationNumber(booking.getConfirmationNumber())
                .status(booking.getReservationStatus())
                .createdAt(booking.getCreatedAt() == null ? null : booking.getCreatedAt().atOffset(ZoneOffset.UTC))
                .propertyId(booking.getPropertyId())
                .businessDate(businessDate)
                .vipTag(booking.getVipTag())
                .dnm(booking.getDnm())
                .guest(buildPrimaryGuest(booking))
                .additionalGuests(buildAdditionalGuests(
                        baseResponse.getGuestNames(),
                        booking.getGuestName(),
                        booking.getPhoneNumber(),
                        preferredEmail(booking)
                ))
                .stay(buildStay(booking))
                .room(buildRoom(booking))
                .booking(buildBookingDetails(booking))
                .pricing(buildPricing(booking, taxSummary, folioBalance))
                .comments(buildComments(booking))
                .actions(buildActions(booking))
                .roomBookings(toRoomBookingSummaries(
                    reservationBookingRepository.findByConfirmationNumberOrderByIdAsc(
                        booking.getConfirmationNumber())))
                .build();
    }

            private List<ReservationRoomBookingSummaryDto> toRoomBookingSummaries(
                List<ReservationBookingRecord> bookings
            ) {
            return bookings.stream()
                .map(booking -> ReservationRoomBookingSummaryDto.builder()
                    .bookingId(booking.getId())
                    .confirmationNumber(booking.getConfirmationNumber())
                    .guestName(booking.getGuestName())
                    .assignedRoomNo(booking.getAssignedRoomNo())
                    .floor(booking.getFloor())
                    .build())
                .toList();
            }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationBookingResponseDto> getBookings() {
        List<ReservationBookingRecord> bookings = reservationBookingRepository.findAllByOrderByCreatedAtDesc();
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> bookingIds = bookings.stream()
                .map(ReservationBookingRecord::getId)
                .collect(Collectors.toList());

        Map<Long, ReservationPaymentTransactionRecord> transactionByBookingId = reservationPaymentTransactionRepository
                .findByBookingIdIn(bookingIds)
                .stream()
                .collect(Collectors.toMap(
                        ReservationPaymentTransactionRecord::getBookingId,
                        Function.identity(),
                        (existing, ignored) -> existing
                ));

        return bookings.stream()
                .map(booking -> reservationBookingMapper.toResponse(booking, transactionByBookingId.get(booking.getId())))
                .collect(Collectors.toList());
    }

    private ReservationViewResponseDto.GuestDto buildPrimaryGuest(ReservationBookingRecord booking) {
        String[] nameParts = splitGuestName(booking.getGuestName());
        return ReservationViewResponseDto.GuestDto.builder()
                .salutation(booking.getSalutation())
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .phoneNumber(booking.getPhoneNumber())
                .email(preferredEmail(booking))
                .address(booking.getAddress())
                .city(booking.getCity())
                .state(booking.getState())
                .country(booking.getCountry())
                .zipCode(booking.getZipCode())
                .dateOfBirth(booking.getDateOfBirth())
                .idType(booking.getIdType())
                .idNumber(booking.getIdNumber())
                .enrollGuest(booking.getEnrollGuest())
                .loyaltyNumber(booking.getLoyaltyNumber())
                .build();
    }

    private List<ReservationViewResponseDto.AdditionalGuestDto> buildAdditionalGuests(
            List<String> guestNames,
            String primaryGuestName,
            String fallbackPhone,
            String fallbackEmail
    ) {
        if (guestNames == null || guestNames.isEmpty()) {
            return List.of();
        }

        List<ReservationViewResponseDto.AdditionalGuestDto> additionalGuests = new ArrayList<>();
        boolean primarySkipped = false;
        String normalizedPrimary = normalizeValue(primaryGuestName);

        for (String guestName : guestNames) {
            if (!StringUtils.hasText(guestName)) {
                continue;
            }

            String trimmedName = guestName.trim();
            if (!primarySkipped
                    && StringUtils.hasText(normalizedPrimary)
                    && normalizedPrimary.equals(normalizeValue(trimmedName))) {
                primarySkipped = true;
                continue;
            }

            additionalGuests.add(ReservationViewResponseDto.AdditionalGuestDto.builder()
                    .name(trimmedName)
                    .phoneNumber(fallbackPhone)
                    .email(fallbackEmail)
                    .build());
        }

        return additionalGuests;
    }

    private ReservationViewResponseDto.StayDto buildStay(ReservationBookingRecord booking) {
        int nights = 0;
        if (booking.getArrivalDate() != null && booking.getDepartureDate() != null) {
            nights = Math.max(0, (int) ChronoUnit.DAYS.between(booking.getArrivalDate(), booking.getDepartureDate()));
        }

        return ReservationViewResponseDto.StayDto.builder()
                .checkInDate(booking.getArrivalDate())
                .checkOutDate(booking.getDepartureDate())
                .nights(nights)
                .rooms(booking.getNumberOfRooms())
                .adults(booking.getAdultCount())
                .children(booking.getChildCount())
                .childAges(List.of())
                .checkInTime(formatTime(booking.getEta()))
                .checkOutTime(formatTime(booking.getCheckOutTime()))
                .build();
    }

    private ReservationViewResponseDto.RoomDto buildRoom(ReservationBookingRecord booking) {
        return ReservationViewResponseDto.RoomDto.builder()
                .roomNo(booking.getAssignedRoomNo())
                .roomType(booking.getRoomType())
                .floor(booking.getFloor() == null ? null : String.valueOf(booking.getFloor()))
                .roomStatus(resolveRoomStatus(booking))
                .build();
    }

    private ReservationViewResponseDto.BookingDto buildBookingDetails(ReservationBookingRecord booking) {
        return ReservationViewResponseDto.BookingDto.builder()
                .groupCode(booking.getGuestGroup())
                .company(booking.getCompany())
                .blockCode(booking.getBlockCode())
                .source(booking.getSource())
                .reservationType(booking.getReservationType())
                .rateCode(booking.getRateCode())
                .build();
    }

    private ReservationViewResponseDto.PricingDto buildPricing(
            ReservationBookingRecord booking,
            TaxSummary taxSummary,
            BigDecimal folioBalance
    ) {
        BigDecimal folioOutstanding = folioBalance != null ? folioBalance : BigDecimal.ZERO;

        return ReservationViewResponseDto.PricingDto.builder()
                .currency(DEFAULT_CURRENCY)
                .roomRate(booking.getRate())
                .taxPercent(booking.getTaxPercent() == null ? taxSummary.taxPercent : booking.getTaxPercent())
                .taxAmount(taxSummary.taxAmount)
                .totalRate(booking.getTotalRate())
                .guestBalance(folioOutstanding)
                .discount(booking.getDiscount())
                .build();
    }

    @Override
    @Transactional
    public void updateGuestBalance(String confirmationNumber, Long bookingId, BigDecimal guestBalance) {
        ReservationBookingRecord booking = bookingId == null
                ? reservationBookingRepository.findByConfirmationNumberOrderByIdAsc(confirmationNumber)
                .stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"))
                : reservationBookingRepository.findById(bookingId)
                .filter(item -> confirmationNumber.equals(item.getConfirmationNumber()))
                .orElseThrow(() -> new BadRequestException("Room booking not found for confirmation number"));
        booking.setGuestBalance(guestBalance == null ? BigDecimal.ZERO : guestBalance.max(BigDecimal.ZERO));
        reservationBookingRepository.save(booking);
    }

    private ReservationViewResponseDto.CommentsDto buildComments(ReservationBookingRecord booking) {
        return ReservationViewResponseDto.CommentsDto.builder()
                .guestRequests(parseGuestRequests(booking.getSpecialRequests()))
                .billingComments(booking.getAlertsMessages())
                .build();
    }

    private ReservationViewResponseDto.ActionsDto buildActions(ReservationBookingRecord booking) {
        String status = normalizeStatus(booking.getReservationStatus());
        boolean canEdit = RESERVATION_STATUS_CONFIRMED.equals(status);
        boolean canCheckIn = RESERVATION_STATUS_CONFIRMED.equals(status);
        boolean canCheckOut = STATUS_ARRIVED.equals(status) || STATUS_CHECKED_IN.equals(status);
        boolean canCancel = false;

        if (STATUS_CHECKED_OUT.equals(status)) {
            canEdit = false;
            canCheckIn = false;
            canCheckOut = false;
        }

        return ReservationViewResponseDto.ActionsDto.builder()
                .canEdit(canEdit)
                .canCheckIn(canCheckIn)
                .canCheckOut(canCheckOut)
                .canCancel(canCancel)
                .build();
    }

    private TaxSummary calculateTaxSummary(ReservationBookingRecord booking) {
        BigDecimal baseAmount = calculateBaseTotalRate(booking);
        BigDecimal roomRate = booking.getRate() == null ? BigDecimal.ZERO : booking.getRate();
        if (!propertyWizardServiceProperties.isEnabled()
                || baseAmount.compareTo(BigDecimal.ZERO) <= 0
                || roomRate.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxSummary.zero();
        }

        List<PropertyTaxRuleResponseDto> taxRules = safeFetchTaxRules(booking.getPropertyId());
        PropertyTaxRuleResponseDto matchedRule = findMatchedTaxRule(booking.getRoomType(), roomRate, taxRules);
        if (matchedRule == null) {
            return TaxSummary.zero();
        }

        BigDecimal taxPercent = matchedRule.getTaxPercentage() == null ? BigDecimal.ZERO : matchedRule.getTaxPercentage();
        BigDecimal taxAmount = BigDecimal.ZERO;

        if (matchedRule.getTaxPercentage() != null) {
            taxAmount = baseAmount
                    .multiply(matchedRule.getTaxPercentage())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        if (matchedRule.getFixedTaxAmount() != null) {
            taxAmount = taxAmount.add(matchedRule.getFixedTaxAmount());
        }

        return new TaxSummary(taxPercent, taxAmount);
    }

    private List<PropertyTaxRuleResponseDto> safeFetchTaxRules(String propertyId) {
        try {
            List<PropertyTaxRuleResponseDto> taxRules = propertyInventoryPort.fetchTaxRules(propertyId);
            return taxRules == null ? List.of() : taxRules;
        } catch (ExternalServiceException ex) {
            return List.of();
        }
    }

    private PropertyTaxRuleResponseDto findMatchedTaxRule(
            String roomType,
            BigDecimal roomRate,
            List<PropertyTaxRuleResponseDto> taxRules
    ) {
        if (taxRules == null || taxRules.isEmpty()) {
            return null;
        }

        return taxRules.stream()
                .filter(rule -> !Boolean.FALSE.equals(rule.getActive()))
                .filter(rule -> !StringUtils.hasText(rule.getRoomType()) || isSameRoomType(rule.getRoomType(), roomType))
                .filter(rule -> rule.getMinAmount() == null || roomRate.compareTo(rule.getMinAmount()) >= 0)
                .filter(rule -> rule.getMaxAmount() == null || roomRate.compareTo(rule.getMaxAmount()) <= 0)
                .findFirst()
                .orElse(null);
    }

    private void applyPropertyTaxOnBooking(ReservationBookingRecord booking) {
        if (booking == null) {
            return;
        }

        BigDecimal baseTotalRate = calculateBaseTotalRate(booking);
        TaxSummary taxSummary = calculateTaxSummary(booking);
        booking.setTotalRate(baseTotalRate.add(taxSummary.taxAmount));
    }

    private BigDecimal calculateBaseTotalRate(ReservationBookingRecord booking) {
        if (booking == null
                || booking.getRate() == null
                || booking.getNumberOfRooms() == null
                || booking.getArrivalDate() == null
                || booking.getDepartureDate() == null) {
            return BigDecimal.ZERO;
        }

        long nights = ChronoUnit.DAYS.between(booking.getArrivalDate(), booking.getDepartureDate());
        if (nights < 0) {
            return BigDecimal.ZERO;
        }
        if (nights == 0) {
            nights = 1;
        }

        return booking.getRate()
                .multiply(BigDecimal.valueOf(booking.getNumberOfRooms().longValue()))
                .multiply(BigDecimal.valueOf(nights));
    }

    private boolean isSameRoomType(String left, String right) {
        String normalizedLeft = normalizeValue(left);
        String normalizedRight = normalizeValue(right);

        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return false;
        }

        return normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft);
    }

    private String resolveRoomStatus(ReservationBookingRecord booking) {
        if (!StringUtils.hasText(booking.getPropertyId()) || !StringUtils.hasText(booking.getConfirmationNumber())) {
            return null;
        }

        LocalDate housekeepingBusinessDate = resolveHousekeepingBusinessDate(booking);
        if (housekeepingBusinessDate == null) {
            return null;
        }

        return housekeepingRoomStatusRepository
                .findByPropertyIdAndBusinessDateAndConfirmationNumber(
                        booking.getPropertyId(),
                        housekeepingBusinessDate,
                        booking.getConfirmationNumber()
                )
                .map(HousekeepingRoomStatusRecord::getRoomStatus)
                .orElse(null);
    }

    private LocalDate resolveBusinessDate(ReservationBookingRecord booking) {
        if (booking.getCheckInBusinessDate() != null) {
            return booking.getCheckInBusinessDate();
        }
        if (booking.getCreatedAt() != null) {
            return booking.getCreatedAt().toLocalDate();
        }
        return booking.getArrivalDate();
    }

    private LocalDate resolveHousekeepingBusinessDate(ReservationBookingRecord booking) {
        if (booking.getCheckInBusinessDate() != null) {
            return booking.getCheckInBusinessDate();
        }
        return booking.getArrivalDate();
    }

    private String preferredEmail(ReservationBookingRecord booking) {
        if (StringUtils.hasText(booking.getPersonalEmail())) {
            return booking.getPersonalEmail();
        }
        return booking.getOfficialEmail();
    }

    private String[] splitGuestName(String guestName) {
        if (!StringUtils.hasText(guestName)) {
            return new String[] {null, null};
        }

        String[] parts = guestName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : null;
        return new String[] {firstName, lastName};
    }

    private String formatTime(java.time.LocalTime value) {
        if (value == null) {
            return null;
        }
        return value.format(TIME_FORMATTER);
    }

    private List<String> parseGuestRequests(String specialRequests) {
        if (!StringUtils.hasText(specialRequests)) {
            return List.of();
        }

        return Arrays.stream(specialRequests.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class TaxSummary {
        private final BigDecimal taxPercent;
        private final BigDecimal taxAmount;

        private TaxSummary(BigDecimal taxPercent, BigDecimal taxAmount) {
            this.taxPercent = taxPercent;
            this.taxAmount = taxAmount;
        }

        private static TaxSummary zero() {
            return new TaxSummary(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private void normalizeCreateRequest(ReservationBookingRequestDto request) {
        if (request == null) {
            return;
        }

        request.setPropertyId(normalizePropertyId(request.getPropertyId()));

        validateIdentityProof(request);

        request.setSalutation(defaultIfBlank(request.getSalutation(), "Mr"));
        request.setReservationType(defaultIfBlank(request.getReservationType(), "GTD"));
        request.setCity(defaultIfBlank(request.getCity(), "UNKNOWN"));
        request.setCountry(defaultIfBlank(request.getCountry(), "UNKNOWN"));
        request.setZipCode(defaultIfBlank(request.getZipCode(), "000000"));

        if (!StringUtils.hasText(request.getPersonalEmail()) && StringUtils.hasText(request.getOfficialEmail())) {
            request.setPersonalEmail(request.getOfficialEmail().trim());
        }
        if (!StringUtils.hasText(request.getOfficialEmail()) && StringUtils.hasText(request.getPersonalEmail())) {
            request.setOfficialEmail(request.getPersonalEmail().trim());
        }

        if (!StringUtils.hasText(request.getGuestName()) && request.getGuestNames() != null && !request.getGuestNames().isEmpty()) {
            String first = request.getGuestNames().get(0);
            if (StringUtils.hasText(first)) {
                request.setGuestName(first.trim());
            }
        }

        if ((request.getGuestNames() == null || request.getGuestNames().isEmpty()) && StringUtils.hasText(request.getGuestName())) {
            request.setGuestNames(List.of(request.getGuestName().trim()));
        }

        if (request.getNumberOfRooms() != null
                && request.getNumberOfRooms() > 1
                && request.getGuestNames() != null
                && request.getGuestNames().size() == 1
                && StringUtils.hasText(request.getGuestNames().get(0))) {
            request.setGuestNames(new ArrayList<>(Collections.nCopies(
                    request.getNumberOfRooms(), request.getGuestNames().get(0).trim())));
        }

        if (request.getNoPost() == null) {
            request.setNoPost(Boolean.FALSE);
        }

        if (request.getVipTag() == null) {
            request.setVipTag(Boolean.FALSE);
        }

        if (request.getDnm() == null) {
            request.setDnm(Boolean.FALSE);
        }

        if (request.getEnrollGuest() == null) {
            request.setEnrollGuest(Boolean.FALSE);
        }

        if (request.getDiscount() == null) {
            request.setDiscount(BigDecimal.ZERO);
        }

        if (request.getGuestBalance() == null) {
            request.setGuestBalance(BigDecimal.ZERO);
        }

        if (!StringUtils.hasText(request.getPaymentType())) {
            request.setPaymentType(PaymentTypes.FULL_PAYMENT);
        }
    }

    /**
     * Reservation tables currently keep propertyId as VARCHAR, but the
     * property-service contract is a UUID. Store the canonical UUID string so
     * downstream services (especially housekeeping) can safely parse it.
     */
    private String normalizePropertyId(String propertyId) {
        if (!StringUtils.hasText(propertyId)) {
            return propertyId;
        }
        try {
            return UUID.fromString(propertyId.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("propertyId must be a valid UUID");
        }
    }

    private void validateIdentityProof(ReservationBookingRequestDto request) {
        boolean hasIdType = StringUtils.hasText(request.getIdType());
        boolean hasIdNumber = StringUtils.hasText(request.getIdNumber());
        if (hasIdType && !hasIdNumber) {
            throw new BadRequestException("idNumber is required when idType is provided");
        }
        if (hasIdNumber && !hasIdType) {
            throw new BadRequestException("idType is required when idNumber is provided");
        }
        if (hasIdType) {
            String normalizedIdType = request.getIdType().trim().toUpperCase(Locale.ROOT);
            if (!IdTypes.supportedTypes().contains(normalizedIdType)) {
                throw new BadRequestException("idType must be AADHAAR, PAN, DRIVING_LICENSE, or PASSPORT");
            }
            request.setIdType(normalizedIdType);
            request.setIdNumber(request.getIdNumber().trim());
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private ReservationPaymentTransactionRecord buildPaymentTransaction(
            ReservationBookingRecord saved,
            ReservationBookingRequestDto request,
            BigDecimal amount,
            PaymentProcessingResult paymentResult
    ) {
        return ReservationPaymentTransactionRecord.builder()
                .bookingId(saved.getId())
                .confirmationNumber(saved.getConfirmationNumber())
                .propertyId(saved.getPropertyId())
                .paymentMode(request.getPayment())
                .amount(amount)
                .transactionStatus(paymentResult.getStatus())
                .transactionReference(paymentResult.getTransactionReference())
                .processorName(paymentResult.getProcessorName())
                .failureReason(paymentResult.getFailureReason())
                .processedAt(paymentResult.getProcessedAt() == null ? LocalDateTime.now() : paymentResult.getProcessedAt())
                .build();
    }

    private BigDecimal calculatePayableAmount(ReservationBookingRequestDto request) {
        if (request.getRate() == null || request.getNumberOfRooms() == null || request.getArrivalDate() == null || request.getDepartureDate() == null) {
            return BigDecimal.ZERO;
        }

        long nights = ChronoUnit.DAYS.between(request.getArrivalDate(), request.getDepartureDate());
        if (nights < 0) {
            return BigDecimal.ZERO;
        }
        if (nights == 0) {
            nights = 1;
        }

        return request.getRate()
                .multiply(BigDecimal.valueOf(request.getNumberOfRooms().longValue()))
                .multiply(BigDecimal.valueOf(nights));
    }

    private String generateConfirmationNumber(String propertyId) {
        for (int attempt = 0; attempt < CONFIRMATION_MAX_ATTEMPTS; attempt++) {
            String candidate = String.valueOf(ThreadLocalRandom.current()
                .nextInt(CONFIRMATION_MIN, CONFIRMATION_MAX_EXCLUSIVE));

            if (!reservationBookingRepository.existsByConfirmationNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate unique confirmation number");
    }

        private String resolveRoomTypeId(String propertyId, String roomType) {
        List<com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto> roomTypes =
            propertyInventoryPort.fetchRoomOutletTypes(propertyId);
        String normalizedRoomType = roomType == null ? "" : roomType.trim();
        return roomTypes.stream()
            .filter(item -> item.getId() != null)
            .filter(item -> normalizedRoomType.equalsIgnoreCase(item.getRoomName())
                || normalizedRoomType.equalsIgnoreCase(item.getRoomCode()))
            .findFirst()
            .map(item -> inventoryRoomTypeId(propertyId, item.getRoomCode(), item.getRoomName()))
            .orElseThrow(() -> new BadRequestException(
                "roomType is not configured for selected property"));
        }

        private String inventoryRoomTypeId(String propertyId, String roomCode, String roomName) {
        String roomKey = StringUtils.hasText(roomCode)
            ? roomCode.trim()
            : roomName == null ? "" : roomName.trim();
        String payload = (propertyId + ":" + (roomKey.isBlank() ? "unknown" : roomKey))
            .toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8)).toString();
        }

    private void validateDates(LocalDate arrivalDate, LocalDate departureDate) {
        if (departureDate != null && arrivalDate != null && departureDate.isBefore(arrivalDate)) {
            throw new BadRequestException("departureDate must be on or after arrivalDate");
        }
    }

    private void validateRoomSelectionAndGuestNames(ReservationBookingRequestDto request) {
        if (request.getNumberOfRooms() == null || request.getNumberOfRooms() < 1 || request.getNumberOfRooms() > 9) {
            throw new BadRequestException("numberOfRooms must be between 1 and 9");
        }

        List<String> guestNames = request.getGuestNames();
        if (guestNames == null || guestNames.size() != request.getNumberOfRooms()) {
            throw new BadRequestException("guestNames count must match numberOfRooms");
        }

        boolean hasBlankGuestName = guestNames.stream().anyMatch(name -> !StringUtils.hasText(name));
        if (hasBlankGuestName) {
            throw new BadRequestException("guestNames must not contain blank values");
        }
    }

    private void validateRequiredContactFields(ReservationBookingRequestDto request) {
        if (!StringUtils.hasText(request.getOfficialEmail())) {
            throw new BadRequestException("officialEmail is required");
        }

        if (!StringUtils.hasText(request.getPersonalEmail())) {
            throw new BadRequestException("personalEmail is required");
        }
    }

    private void validateAndNormalizePaymentMode(ReservationBookingRequestDto request) {
        if (!StringUtils.hasText(request.getPayment())) {
            throw new BadRequestException("payment is required");
        }

        String normalizedPaymentMode = PaymentModes.normalize(request.getPayment());

        if (!PaymentModes.isSupported(normalizedPaymentMode)) {
            throw new BadRequestException("payment must be one of CARD, CASH, UPI, NET_BANKING, WALLET");
        }

        request.setPayment(normalizedPaymentMode);
    }

    private void validateAndNormalizePaymentType(ReservationBookingRequestDto request) {
        if (!StringUtils.hasText(request.getPaymentType())) {
            throw new BadRequestException("paymentType is required");
        }

        String normalizedPaymentType = PaymentTypes.normalize(request.getPaymentType());
        if (!PaymentTypes.isSupported(normalizedPaymentType)) {
            throw new BadRequestException("paymentType must be one of ADVANCE, FULL_PAYMENT");
        }

        request.setPaymentType(normalizedPaymentType);
    }

    private void validatePhoneNumberFormat(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new BadRequestException("phoneNumber is required");
        }

        if (!phoneNumber.matches("\\d{10}")) {
            throw new BadRequestException("phoneNumber must be exactly 10 digits");
        }
    }

    private void preserveSystemFields(ReservationBookingRecord existing, ReservationBookingRecord updated) {
        updated.setId(existing.getId());
        updated.setConfirmationNumber(existing.getConfirmationNumber());
        updated.setReservationStatus(existing.getReservationStatus());
        updated.setAssignedRoomNo(existing.getAssignedRoomNo());
        updated.setFloor(existing.getFloor());
        updated.setInventoryDeductedAt(existing.getInventoryDeductedAt());
        updated.setInventorySyncedAt(existing.getInventorySyncedAt());
        updated.setCheckInCompletedAt(existing.getCheckInCompletedAt());
        updated.setCheckInCompletedBy(existing.getCheckInCompletedBy());
        updated.setCheckInBusinessDate(existing.getCheckInBusinessDate());
        updated.setPayment(existing.getPayment());
        updated.setPaymentType(existing.getPaymentType());
        updated.setCreatedAt(existing.getCreatedAt());
    }

    private void validateDnmRoomLock(
            ReservationBookingRecord existing,
            ReservationBookingRequestDto request
    ) {
        if (!Boolean.TRUE.equals(existing.getDnm())
                || !StringUtils.hasText(request.getAssignedRoomNo())
                || !StringUtils.hasText(existing.getAssignedRoomNo())) {
            return;
        }

        if (!existing.getAssignedRoomNo().trim().equalsIgnoreCase(request.getAssignedRoomNo().trim())) {
            throw new BadRequestException("Room cannot be changed because DNM is enabled for this reservation");
        }
    }
}


