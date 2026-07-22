package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.constant.CheckInStep;
import com.pms.reservation.dto.CheckInAuditEventDto;
import com.pms.reservation.dto.CheckInAuditHistoryResponseDto;
import com.pms.reservation.dto.CheckInAuditFilterRequestDto;
import com.pms.reservation.dto.CheckInAuditPageResponseDto;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.dto.CheckInGuestUpdateRequestDto;
import com.pms.reservation.dto.CheckInPaymentValidationResponseDto;
import com.pms.reservation.dto.CheckInRoomStayUpdateRequestDto;
import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.CheckInStepProgressResponseDto;
import com.pms.reservation.dto.CheckInStepStatusDto;
import com.pms.reservation.dto.CheckInWorkflowResponseDto;
import com.pms.reservation.dto.GuestContactDetailsDto;
import com.pms.reservation.dto.RoomStayDetailsDto;
import com.pms.reservation.dto.SignatureSummaryDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import com.pms.reservation.entity.ReservationCheckInSignatureRecord;
import com.pms.reservation.entity.ReservationCheckInWorkflowRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import com.pms.reservation.repository.ReservationCheckInSignatureRepository;
import com.pms.reservation.repository.ReservationCheckInWorkflowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationCheckInWorkflowServiceImpl implements com.pms.reservation.service.ReservationCheckInWorkflowService {

    private static final String STATUS_ARRIVED = "ARRIVED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationCheckInWorkflowRepository workflowRepository;
    private final ReservationCheckInSignatureRepository signatureRepository;
    private final ReservationCheckInAuditRepository auditRepository;
    private final HousekeepingRoomStatusService housekeepingRoomStatusService;
    private final PropertyInventoryPort propertyInventoryPort;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;

    @Override
    @Transactional(readOnly = true)
    public CheckInWorkflowResponseDto getWorkflow(Long bookingId) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        ReservationCheckInSignatureRecord signature = signatureRepository.findByBookingId(bookingId).orElse(null);
        return toWorkflowResponse(booking, workflow, signature);
    }

        @Override
        @Transactional(readOnly = true)
        public CheckInStepProgressResponseDto getStepProgress(Long bookingId) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        ReservationCheckInSignatureRecord signature = signatureRepository.findByBookingId(bookingId).orElse(null);

        List<CheckInStepStatusDto> steps = buildStepStatuses(workflow);
        int completed = (int) steps.stream().filter(CheckInStepStatusDto::isCompleted).count();
        int total = steps.size();
        int progress = total == 0 ? 0 : (completed * 100) / total;

        return CheckInStepProgressResponseDto.builder()
            .bookingId(booking.getId())
            .confirmationNumber(booking.getConfirmationNumber())
            .propertyId(booking.getPropertyId())
            .currentStep(workflow.getCurrentStep())
            .completedSteps(completed)
            .totalSteps(total)
            .progressPercent(progress)
            .canCompleteCheckIn(workflow.getPaymentValidatedAt() != null && signature != null)
            .steps(steps)
            .build();
        }

        @Override
        @Transactional(readOnly = true)
        public CheckInAuditHistoryResponseDto getAuditHistory(Long bookingId) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        List<ReservationCheckInAuditRecord> records = auditRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);

        List<CheckInAuditEventDto> events = records.stream()
            .map(record -> CheckInAuditEventDto.builder()
                .eventType(record.getEventType())
                .eventMessage(record.getEventMessage())
                .changedFields(record.getChangedFields())
                .actor(record.getActor())
                .createdAt(record.getCreatedAt())
                .build())
            .toList();

        return CheckInAuditHistoryResponseDto.builder()
            .bookingId(booking.getId())
            .confirmationNumber(booking.getConfirmationNumber())
            .propertyId(booking.getPropertyId())
            .totalEvents(events.size())
            .events(events)
            .build();
        }

    @Override
    @Transactional(readOnly = true)
    public CheckInAuditPageResponseDto getAuditHistoryPage(
            Long bookingId,
            String eventType,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortDir
    ) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);

        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(1, Math.min(size, 200));
        String resolvedSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        Sort.Direction direction = "asc".equals(resolvedSortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, "createdAt"));

        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;
        if (resolvedFrom != null && resolvedTo == null) {
            resolvedTo = resolvedFrom;
        }
        if (resolvedTo != null && resolvedFrom == null) {
            resolvedFrom = resolvedTo;
        }
        if (resolvedFrom != null && resolvedTo != null && resolvedFrom.isAfter(resolvedTo)) {
            throw new BadRequestException("fromDate must be on or before toDate");
        }

        String normalizedEventType = StringUtils.hasText(eventType) ? eventType.trim() : null;
        boolean hasEventType = StringUtils.hasText(normalizedEventType);
        boolean hasDateRange = resolvedFrom != null && resolvedTo != null;

        Page<ReservationCheckInAuditRecord> resultPage;
        if (hasEventType && hasDateRange) {
            resultPage = auditRepository.findByBookingIdAndEventTypeIgnoreCaseAndCreatedAtBetween(
                    bookingId,
                    normalizedEventType,
                    resolvedFrom.atStartOfDay(),
                    resolvedTo.plusDays(1).atStartOfDay().minusNanos(1),
                    pageable
            );
        } else if (hasEventType) {
            resultPage = auditRepository.findByBookingIdAndEventTypeIgnoreCase(bookingId, normalizedEventType, pageable);
        } else if (hasDateRange) {
            resultPage = auditRepository.findByBookingIdAndCreatedAtBetween(
                    bookingId,
                    resolvedFrom.atStartOfDay(),
                    resolvedTo.plusDays(1).atStartOfDay().minusNanos(1),
                    pageable
            );
        } else {
            resultPage = auditRepository.findByBookingId(bookingId, pageable);
        }

        List<CheckInAuditEventDto> events = resultPage.getContent().stream()
                .map(record -> CheckInAuditEventDto.builder()
                        .eventType(record.getEventType())
                        .eventMessage(record.getEventMessage())
                        .changedFields(record.getChangedFields())
                        .actor(record.getActor())
                        .createdAt(record.getCreatedAt())
                        .build())
                .toList();

        return CheckInAuditPageResponseDto.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .propertyId(booking.getPropertyId())
                .filters(CheckInAuditFilterRequestDto.builder()
                        .eventType(normalizedEventType)
                        .fromDate(resolvedFrom)
                        .toDate(resolvedTo)
                        .page(resolvedPage)
                        .size(resolvedSize)
                        .sortDir(resolvedSortDir)
                        .build())
                .events(events)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public CheckInWorkflowResponseDto updateGuestDetails(Long bookingId, CheckInGuestUpdateRequestDto request, String actor) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        ensureStepAllowed(workflow, CheckInStep.GUEST_DETAILS);

        StringJoiner changed = new StringJoiner(", ");
        applyIfChanged(changed, "personalEmail", booking.getPersonalEmail(), request.getPersonalEmail());
        applyIfChanged(changed, "officialEmail", booking.getOfficialEmail(), request.getOfficialEmail());
        applyIfChanged(changed, "phoneNumber", booking.getPhoneNumber(), request.getPhoneNumber());
        applyIfChanged(changed, "mobileNumber", booking.getMobileNumber(), request.getMobileNumber());
        applyIfChanged(changed, "city", booking.getCity(), request.getCity());
        applyIfChanged(changed, "country", booking.getCountry(), request.getCountry());
        applyIfChanged(changed, "zipCode", booking.getZipCode(), request.getZipCode());

        booking.setPersonalEmail(request.getPersonalEmail().trim());
        booking.setOfficialEmail(request.getOfficialEmail().trim());
        booking.setPhoneNumber(request.getPhoneNumber().trim());
        booking.setMobileNumber(request.getMobileNumber().trim());
        booking.setCity(request.getCity().trim());
        booking.setCountry(request.getCountry().trim());
        booking.setZipCode(request.getZipCode().trim());
        reservationBookingRepository.save(booking);

        markStepCompleted(workflow, CheckInStep.GUEST_DETAILS);
        workflowRepository.save(workflow);

        appendAudit(
                booking,
                "GUEST_DETAILS_UPDATED",
                "Guest contact details updated during check-in",
                changed.length() == 0 ? "none" : changed.toString(),
                actor
        );

        ReservationCheckInSignatureRecord signature = signatureRepository.findByBookingId(bookingId).orElse(null);
        return toWorkflowResponse(booking, workflow, signature);
    }

    @Override
    @Transactional
    public CheckInWorkflowResponseDto updateRoomStay(Long bookingId, CheckInRoomStayUpdateRequestDto request, String actor) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        ensureStepAllowed(workflow, CheckInStep.ROOM_STAY);

        if (propertyWizardServiceProperties.isEnabled()) {
            PropertyInventoryValidationResponse validation = propertyInventoryPort.validateInventory(
                    booking.getPropertyId(),
                    request.getRoomType(),
                    booking.getNumberOfRooms()
            );
            if (!Boolean.TRUE.equals(validation.getPropertyExists())) {
                throw new BadRequestException("propertyId is invalid as per Property Wizard service");
            }
            if (!Boolean.TRUE.equals(validation.getRoomTypeAvailable())) {
                throw new BadRequestException("roomType is not available for selected property");
            }
            if (validation.getAvailableRooms() != null && booking.getNumberOfRooms() > validation.getAvailableRooms()) {
                throw new BadRequestException("numberOfRooms exceeds available rooms for selected property and roomType");
            }
        }

        StringJoiner changed = new StringJoiner(", ");
        applyIfChanged(changed, "roomType", booking.getRoomType(), request.getRoomType());
        applyIfChanged(changed, "roomNo", booking.getAssignedRoomNo(), request.getRoomNo());

        booking.setRoomType(request.getRoomType().trim());
        booking.setAssignedRoomNo(request.getRoomNo().trim());
        reservationBookingRepository.save(booking);

        markStepCompleted(workflow, CheckInStep.ROOM_STAY);
        workflowRepository.save(workflow);

        appendAudit(
                booking,
                "ROOM_STAY_UPDATED",
                "Room and stay details updated during check-in",
                changed.length() == 0 ? "none" : changed.toString(),
                actor
        );

        ReservationCheckInSignatureRecord signature = signatureRepository.findByBookingId(bookingId).orElse(null);
        return toWorkflowResponse(booking, workflow, signature);
    }

    @Override
    @Transactional
    public CheckInWorkflowResponseDto saveSignature(Long bookingId, CheckInSignatureRequestDto request, String actor) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        ensureStepAllowed(workflow, CheckInStep.SIGNATURE);

        ReservationCheckInSignatureRecord signature = signatureRepository.findByBookingId(bookingId)
                .orElseGet(() -> ReservationCheckInSignatureRecord.builder()
                        .bookingId(bookingId)
                        .confirmationNumber(booking.getConfirmationNumber())
                        .propertyId(booking.getPropertyId())
                        .createdAt(LocalDateTime.now())
                        .build());

        signature.setContentType(request.getContentType().trim());
        signature.setPayloadBase64(request.getPayloadBase64().trim());
        signature.setSignedAt(LocalDateTime.now());
        signature.setUpdatedAt(LocalDateTime.now());
        ReservationCheckInSignatureRecord savedSignature = signatureRepository.save(signature);

        markStepCompleted(workflow, CheckInStep.SIGNATURE);
        workflowRepository.save(workflow);

        appendAudit(
                booking,
                "SIGNATURE_CAPTURED",
                "Guest signature captured during check-in",
                "contentType",
                actor
        );

        return toWorkflowResponse(booking, workflow, savedSignature);
    }

    @Override
    @Transactional
    public CheckInPaymentValidationResponseDto validatePayment(Long bookingId, String actor) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        ensureStepAllowed(workflow, CheckInStep.PAYMENT_VALIDATION);

        BigDecimal guestBalance = booking.getGuestBalance() == null ? BigDecimal.ZERO : booking.getGuestBalance();
        if (guestBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Check-in blocked: outstanding guest balance must be settled before check-in");
        }

        markStepCompleted(workflow, CheckInStep.PAYMENT_VALIDATION);
        workflowRepository.save(workflow);

        appendAudit(
                booking,
                "PAYMENT_VALIDATED",
                "Payment validation passed for check-in",
                "guestBalance=0",
                actor
        );

        return CheckInPaymentValidationResponseDto.builder()
                .passed(true)
                .message("Payment validation passed")
                .build();
    }

    @Override
    @Transactional
    public CheckInCompletionResponseDto completeCheckIn(Long bookingId, CheckInCompleteRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInWorkflowRecord workflow = getOrCreateWorkflow(booking);
        String targetStatus = resolveTargetStatus(request.getTargetStatus());

        if (workflow.getCompletedAt() != null) {
            if (STATUS_CHECKED_IN.equals(targetStatus) && STATUS_ARRIVED.equals(booking.getReservationStatus())) {
                booking.setReservationStatus(STATUS_CHECKED_IN);
                booking.setCheckInCompletedAt(LocalDateTime.now());
                booking.setCheckInCompletedBy(request.getActor().trim());
                booking.setCheckInBusinessDate(request.getBusinessDate());
                reservationBookingRepository.save(booking);

                appendAudit(
                        booking,
                        "CHECKIN_STATUS_UPGRADED",
                        "Check-in status upgraded from ARRIVED to CHECKED_IN",
                        "status=" + STATUS_CHECKED_IN,
                        request.getActor()
                );
            } else if (!targetStatus.equals(booking.getReservationStatus())) {
                throw new BadRequestException("Check-in already completed with status " + booking.getReservationStatus());
            }

            return CheckInCompletionResponseDto.builder()
                    .bookingId(booking.getId())
                    .confirmationNumber(booking.getConfirmationNumber())
                    .reservationStatus(booking.getReservationStatus())
                    .checkInCompletedAt(booking.getCheckInCompletedAt())
                    .checkInCompletedBy(booking.getCheckInCompletedBy())
                    .build();
        }

        ensureStepAllowed(workflow, CheckInStep.COMPLETE_CHECKIN);
        assertMandatoryStepsCompleted(workflow);
        if (signatureRepository.findByBookingId(bookingId).isEmpty()) {
            throw new BadRequestException("Check-in blocked: mandatory guest signature is missing");
        }

        booking.setReservationStatus(targetStatus);
        booking.setCheckInCompletedAt(LocalDateTime.now());
        booking.setCheckInCompletedBy(request.getActor().trim());
        booking.setCheckInBusinessDate(request.getBusinessDate());
        reservationBookingRepository.save(booking);

        HousekeepingRoomStatusRequestDto housekeepingRequest = new HousekeepingRoomStatusRequestDto();
        housekeepingRequest.setPropertyId(booking.getPropertyId());
        housekeepingRequest.setBusinessDate(request.getBusinessDate());
        housekeepingRequest.setConfirmationNumber(booking.getConfirmationNumber());
        housekeepingRequest.setRoomNo(booking.getAssignedRoomNo());
        housekeepingRoomStatusService.markOccupied(housekeepingRequest);

        markStepCompleted(workflow, CheckInStep.COMPLETE_CHECKIN);
        workflowRepository.save(workflow);

        appendAudit(
                booking,
                "CHECKIN_COMPLETED",
                "Check-in completed successfully",
            "status=" + targetStatus,
                request.getActor()
        );

        return CheckInCompletionResponseDto.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .reservationStatus(booking.getReservationStatus())
                .checkInCompletedAt(booking.getCheckInCompletedAt())
                .checkInCompletedBy(booking.getCheckInCompletedBy())
                .build();
    }

    private String resolveTargetStatus(String targetStatus) {
        if (!StringUtils.hasText(targetStatus)) {
            return STATUS_CHECKED_IN;
        }

        String normalized = targetStatus.trim().toUpperCase();
        if (!STATUS_ARRIVED.equals(normalized) && !STATUS_CHECKED_IN.equals(normalized)) {
            throw new BadRequestException("targetStatus must be ARRIVED or CHECKED_IN");
        }
        return normalized;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckInSignatureResponseDto getSignature(Long bookingId) {
        ReservationBookingRecord booking = getBookingOrThrow(bookingId);
        ReservationCheckInSignatureRecord signature = signatureRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BadRequestException("Signature not found for booking"));

        return CheckInSignatureResponseDto.builder()
                .bookingId(bookingId)
                .confirmationNumber(booking.getConfirmationNumber())
                .contentType(signature.getContentType())
                .payloadBase64(signature.getPayloadBase64())
                .signedAt(signature.getSignedAt())
                .build();
    }

    private ReservationBookingRecord getBookingOrThrow(Long bookingId) {
        return reservationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
    }

    private ReservationCheckInWorkflowRecord getOrCreateWorkflow(ReservationBookingRecord booking) {
        return workflowRepository.findByBookingId(booking.getId())
                .orElseGet(() -> workflowRepository.save(ReservationCheckInWorkflowRecord.builder()
                        .bookingId(booking.getId())
                        .confirmationNumber(booking.getConfirmationNumber())
                        .propertyId(booking.getPropertyId())
                        .currentStep(CheckInStep.GUEST_DETAILS.name())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private CheckInWorkflowResponseDto toWorkflowResponse(
            ReservationBookingRecord booking,
            ReservationCheckInWorkflowRecord workflow,
            ReservationCheckInSignatureRecord signature
    ) {
        List<CheckInStepStatusDto> steps = buildStepStatuses(workflow);
        int completed = (int) steps.stream().filter(CheckInStepStatusDto::isCompleted).count();
        int total = steps.size();
        int progress = total == 0 ? 0 : (completed * 100) / total;

        return CheckInWorkflowResponseDto.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .propertyId(booking.getPropertyId())
                .reservationStatus(booking.getReservationStatus())
                .currentStep(workflow.getCurrentStep())
                .completedSteps(completed)
                .totalSteps(total)
                .progressPercent(progress)
                .canCompleteCheckIn(workflow.getPaymentValidatedAt() != null && signature != null)
                .checkInCompletedAt(booking.getCheckInCompletedAt())
                .checkInCompletedBy(booking.getCheckInCompletedBy())
                .steps(steps)
                .guestContactDetails(GuestContactDetailsDto.builder()
                        .personalEmail(booking.getPersonalEmail())
                        .officialEmail(booking.getOfficialEmail())
                        .phoneNumber(booking.getPhoneNumber())
                        .mobileNumber(booking.getMobileNumber())
                        .city(booking.getCity())
                        .country(booking.getCountry())
                        .zipCode(booking.getZipCode())
                        .build())
                .roomStayDetails(RoomStayDetailsDto.builder()
                        .arrivalDate(booking.getArrivalDate())
                        .departureDate(booking.getDepartureDate())
                        .roomType(booking.getRoomType())
                        .roomNo(booking.getAssignedRoomNo())
                        .build())
                .signature(SignatureSummaryDto.builder()
                        .present(signature != null)
                        .contentType(signature == null ? null : signature.getContentType())
                        .signedAt(signature == null ? null : signature.getSignedAt())
                        .build())
                .build();
    }

    private List<CheckInStepStatusDto> buildStepStatuses(ReservationCheckInWorkflowRecord workflow) {
        List<CheckInStepStatusDto> statuses = new ArrayList<>();
        statuses.add(stepStatus(CheckInStep.GUEST_DETAILS, workflow.getGuestDetailsCompletedAt()));
        statuses.add(stepStatus(CheckInStep.ROOM_STAY, workflow.getRoomStayCompletedAt()));
        statuses.add(stepStatus(CheckInStep.SIGNATURE, workflow.getSignatureCompletedAt()));
        statuses.add(stepStatus(CheckInStep.PAYMENT_VALIDATION, workflow.getPaymentValidatedAt()));
        statuses.add(stepStatus(CheckInStep.COMPLETE_CHECKIN, workflow.getCompletedAt()));
        return statuses;
    }

    private CheckInStepStatusDto stepStatus(CheckInStep step, LocalDateTime completedAt) {
        return CheckInStepStatusDto.builder()
                .code(step.name())
                .label(step.getLabel())
                .sequence(step.getSequence())
                .mandatory(true)
                .completed(completedAt != null)
                .completedAt(completedAt)
                .build();
    }

    private void ensureStepAllowed(ReservationCheckInWorkflowRecord workflow, CheckInStep requestedStep) {
        CheckInStep current = CheckInStep.valueOf(workflow.getCurrentStep());
        if (current != requestedStep) {
            throw new BadRequestException("Cannot execute step " + requestedStep.name() + " before completing " + current.name());
        }
    }

    private void assertMandatoryStepsCompleted(ReservationCheckInWorkflowRecord workflow) {
        if (workflow.getGuestDetailsCompletedAt() == null
                || workflow.getRoomStayCompletedAt() == null
                || workflow.getSignatureCompletedAt() == null
                || workflow.getPaymentValidatedAt() == null) {
            throw new BadRequestException("Check-in cannot be completed until all mandatory steps are completed");
        }
    }

    private void markStepCompleted(ReservationCheckInWorkflowRecord workflow, CheckInStep completedStep) {
        LocalDateTime now = LocalDateTime.now();
        switch (completedStep) {
            case GUEST_DETAILS -> {
                if (workflow.getGuestDetailsCompletedAt() == null) {
                    workflow.setGuestDetailsCompletedAt(now);
                }
                workflow.setCurrentStep(CheckInStep.ROOM_STAY.name());
            }
            case ROOM_STAY -> {
                if (workflow.getRoomStayCompletedAt() == null) {
                    workflow.setRoomStayCompletedAt(now);
                }
                workflow.setCurrentStep(CheckInStep.SIGNATURE.name());
            }
            case SIGNATURE -> {
                if (workflow.getSignatureCompletedAt() == null) {
                    workflow.setSignatureCompletedAt(now);
                }
                workflow.setCurrentStep(CheckInStep.PAYMENT_VALIDATION.name());
            }
            case PAYMENT_VALIDATION -> {
                if (workflow.getPaymentValidatedAt() == null) {
                    workflow.setPaymentValidatedAt(now);
                }
                workflow.setCurrentStep(CheckInStep.COMPLETE_CHECKIN.name());
            }
            case COMPLETE_CHECKIN -> {
                if (workflow.getCompletedAt() == null) {
                    workflow.setCompletedAt(now);
                }
                workflow.setCurrentStep(CheckInStep.COMPLETE_CHECKIN.name());
            }
            default -> throw new IllegalArgumentException("Unsupported check-in step");
        }
        workflow.setUpdatedAt(now);
    }

    private void appendAudit(
            ReservationBookingRecord booking,
            String eventType,
            String eventMessage,
            String changedFields,
            String actor
    ) {
        String resolvedActor = StringUtils.hasText(actor) ? actor.trim() : "system";
        ReservationCheckInAuditRecord record = ReservationCheckInAuditRecord.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .propertyId(booking.getPropertyId())
                .eventType(eventType)
                .eventMessage(eventMessage)
                .changedFields(changedFields)
                .actor(resolvedActor)
                .createdAt(LocalDateTime.now())
                .build();
        auditRepository.save(record);
    }

    private void applyIfChanged(StringJoiner joiner, String field, Object before, Object after) {
        if (!Objects.equals(normalize(before), normalize(after))) {
            joiner.add(field);
        }
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }
}
