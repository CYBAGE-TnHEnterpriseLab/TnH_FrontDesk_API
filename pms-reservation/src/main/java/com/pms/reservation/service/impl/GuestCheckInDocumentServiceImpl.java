package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.constant.IdTypes;
import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.IdProofRequestDto;
import com.pms.reservation.dto.IdProofResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInIdProofRecord;
import com.pms.reservation.entity.ReservationCheckInSignatureRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInIdProofRepository;
import com.pms.reservation.repository.ReservationCheckInSignatureRepository;
import com.pms.reservation.service.GuestCheckInDocumentService;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GuestCheckInDocumentServiceImpl implements GuestCheckInDocumentService {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationCheckInSignatureRepository signatureRepository;
    private final ReservationCheckInIdProofRepository idProofRepository;

    @Override
    @Transactional
    public CheckInSignatureResponseDto saveDigitalSignature(CheckInSignatureRequestDto request) {
        ReservationBookingRecord booking = resolveBooking(
                request.getBookingId(), request.getConfirmationNumber(), request.getPropertyId());
        String payload = validateImage(request.getContentType(), request.getPayloadBase64(), "signature");
        ReservationCheckInSignatureRecord record = signatureRepository.findByBookingId(booking.getId())
                .orElseGet(ReservationCheckInSignatureRecord::new);
        record.setBookingId(booking.getId());
        record.setConfirmationNumber(booking.getConfirmationNumber());
        record.setPropertyId(booking.getPropertyId());
        record.setCheckInChannel(normalizeCheckInChannel(request.getCheckInChannel()));
        record.setContentType(request.getContentType().trim());
        record.setPayloadBase64(payload);
        record.setSignedAt(LocalDateTime.now());
        return toSignatureResponse(signatureRepository.save(record));
    }

    @Override
    @Transactional
    public CheckInSignatureResponseDto saveDigitalSignature(Long bookingId, String confirmationNumber,
                                                              String propertyId, MultipartFile file) {
        return saveDigitalSignature(bookingId, confirmationNumber, propertyId, IdTypes.FRONT_DESK, file);
    }

    @Override
    @Transactional
    public CheckInSignatureResponseDto saveDigitalSignature(Long bookingId, String confirmationNumber,
                                                              String propertyId, String checkInChannel,
                                                              MultipartFile file) {
        ReservationBookingRecord booking = resolveBooking(bookingId, confirmationNumber, propertyId);
        String contentType = file == null ? null : file.getContentType();
        String payload = encodeFile(file, contentType, "signature");
        ReservationCheckInSignatureRecord record = signatureRepository.findByBookingId(booking.getId())
                .orElseGet(ReservationCheckInSignatureRecord::new);
        record.setBookingId(booking.getId());
        record.setConfirmationNumber(booking.getConfirmationNumber());
        record.setPropertyId(booking.getPropertyId());
        record.setCheckInChannel(normalizeCheckInChannel(checkInChannel));
        record.setContentType(contentType.trim());
        record.setPayloadBase64(payload);
        record.setSignedAt(LocalDateTime.now());
        return toSignatureResponse(signatureRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public CheckInSignatureResponseDto getDigitalSignature(Long bookingId, String confirmationNumber) {
        ReservationBookingRecord booking = resolveBooking(bookingId, confirmationNumber, null);
        return signatureRepository.findByBookingId(booking.getId())
                .map(this::toSignatureResponse)
                .orElseThrow(() -> new BadRequestException("Digital signature not found"));
    }

    @Override
    @Transactional
    public IdProofResponseDto uploadIdProofDetails(IdProofRequestDto request) {
        ReservationBookingRecord booking = resolveBooking(
                request.getBookingId(), request.getConfirmationNumber(), request.getPropertyId());
        String idProofType = normalizeIdProofType(request.getIdProofType(), request.getIdProofNumber());
        String payload = validateImage(request.getContentType(), request.getPayloadBase64(), "ID proof");
        ReservationCheckInIdProofRecord record = idProofRepository.findByBookingId(booking.getId())
                .orElseGet(ReservationCheckInIdProofRecord::new);
        record.setBookingId(booking.getId());
        record.setConfirmationNumber(booking.getConfirmationNumber());
        record.setPropertyId(booking.getPropertyId());
        record.setCheckInChannel(normalizeCheckInChannel(request.getCheckInChannel()));
        record.setIdProofType(idProofType);
        record.setIdProofNumber(request.getIdProofNumber().trim());
        record.setContentType(request.getContentType().trim());
        record.setPayloadBase64(payload);
        record.setUploadedAt(LocalDateTime.now());
        return toIdProofResponse(idProofRepository.save(record));
    }

    @Override
    @Transactional
    public IdProofResponseDto uploadIdProofDetails(Long bookingId, String confirmationNumber, String propertyId,
                                                    String idProofType, String idProofNumber, MultipartFile file) {
        return uploadIdProofDetails(bookingId, confirmationNumber, propertyId, idProofType, idProofNumber,
            IdTypes.FRONT_DESK, file);
        }

        @Override
        @Transactional
        public IdProofResponseDto uploadIdProofDetails(Long bookingId, String confirmationNumber, String propertyId,
                                String idProofType, String idProofNumber, String checkInChannel,
                                MultipartFile file) {
        ReservationBookingRecord booking = resolveBooking(bookingId, confirmationNumber, propertyId);
        String contentType = file == null ? null : file.getContentType();
        String payload = encodeFile(file, contentType, "ID proof");
        if (!StringUtils.hasText(idProofType) || !StringUtils.hasText(idProofNumber)) {
            throw new BadRequestException("idProofType and idProofNumber are required");
        }
        idProofType = normalizeIdProofType(idProofType, idProofNumber);
        ReservationCheckInIdProofRecord record = idProofRepository.findByBookingId(booking.getId())
                .orElseGet(ReservationCheckInIdProofRecord::new);
        record.setBookingId(booking.getId());
        record.setConfirmationNumber(booking.getConfirmationNumber());
        record.setPropertyId(booking.getPropertyId());
        record.setCheckInChannel(normalizeCheckInChannel(checkInChannel));
        record.setIdProofType(idProofType);
        record.setIdProofNumber(idProofNumber.trim());
        record.setContentType(contentType.trim());
        record.setPayloadBase64(payload);
        record.setUploadedAt(LocalDateTime.now());
        return toIdProofResponse(idProofRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public IdProofResponseDto getUploadIdProofDetails(Long bookingId, String confirmationNumber) {
        ReservationBookingRecord booking = resolveBooking(bookingId, confirmationNumber, null);
        return idProofRepository.findByBookingId(booking.getId())
                .map(this::toIdProofResponse)
                .orElseThrow(() -> new BadRequestException("ID proof details not found"));
    }

    private ReservationBookingRecord resolveBooking(Long bookingId, String confirmationNumber, String propertyId) {
        if (bookingId == null || !StringUtils.hasText(confirmationNumber)) {
            throw new BadRequestException("bookingId and confirmationNumber are required");
        }
        ReservationBookingRecord booking = reservationBookingRepository
                .findByIdAndConfirmationNumber(bookingId, confirmationNumber.trim())
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        if (StringUtils.hasText(propertyId) && !propertyId.trim().equals(booking.getPropertyId())) {
            throw new BadRequestException("Booking does not belong to the supplied propertyId");
        }
        return booking;
    }

    private String normalizeIdProofType(String idProofType, String idProofNumber) {
        if (!StringUtils.hasText(idProofType) || !StringUtils.hasText(idProofNumber)) {
            throw new BadRequestException("idProofType and idProofNumber are required");
        }
        String normalizedType = idProofType.trim().toUpperCase(Locale.ROOT);
        if (!IdTypes.isSupported(normalizedType)) {
            throw new BadRequestException("idProofType must be AADHAAR, PAN, DRIVING_LICENSE, or PASSPORT");
        }
        return normalizedType;
    }

    private String normalizeCheckInChannel(String channel) {
        String normalized = StringUtils.hasText(channel) ? channel.trim().toUpperCase(Locale.ROOT) : IdTypes.FRONT_DESK;
        if (!IdTypes.isSupportedCheckInChannel(normalized)) {
            throw new BadRequestException("checkInChannel must be FRONT_DESK, KIOSK, or ONLINE");
        }
        return normalized;
    }

    private String validateImage(String contentType, String payloadBase64, String documentName) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!normalizedContentType.equals("image/png")
                && !normalizedContentType.equals("image/jpeg")
                && !normalizedContentType.equals("image/jpg")) {
            throw new BadRequestException(documentName + " contentType must be image/png or image/jpeg");
        }
        String payload = payloadBase64 == null ? "" : payloadBase64.trim();
        if (payload.startsWith("data:")) {
            int commaIndex = payload.indexOf(',');
            if (commaIndex < 0) {
                throw new BadRequestException(documentName + " payload is not valid Base64");
            }
            payload = payload.substring(commaIndex + 1);
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(payload);
            if (decoded.length == 0 || decoded.length > MAX_IMAGE_BYTES) {
                throw new BadRequestException(documentName + " image size must be between 1 byte and 10 MB");
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(documentName + " payload is not valid Base64");
        }
        return payload;
    }

    private String encodeFile(MultipartFile file, String contentType, String documentName) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(documentName + " image file is required");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_IMAGE_BYTES) {
                throw new BadRequestException(documentName + " image size must not exceed 10 MB");
            }
            validateImage(contentType, Base64.getEncoder().encodeToString(bytes), documentName);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException ex) {
            throw new BadRequestException("Unable to read " + documentName + " image");
        }
    }

    private CheckInSignatureResponseDto toSignatureResponse(ReservationCheckInSignatureRecord record) {
        return CheckInSignatureResponseDto.builder()
                .bookingId(record.getBookingId())
                .confirmationNumber(record.getConfirmationNumber())
                .propertyId(record.getPropertyId())
                .contentType(record.getContentType())
                .payloadBase64(record.getPayloadBase64())
                .checkInChannel(record.getCheckInChannel())
                .signedAt(record.getSignedAt())
                .build();
    }

    private IdProofResponseDto toIdProofResponse(ReservationCheckInIdProofRecord record) {
        return IdProofResponseDto.builder()
                .bookingId(record.getBookingId())
                .confirmationNumber(record.getConfirmationNumber())
                .propertyId(record.getPropertyId())
                .idProofType(record.getIdProofType())
                .idProofNumber(record.getIdProofNumber())
                .contentType(record.getContentType())
                .checkInChannel(record.getCheckInChannel())
                .payloadBase64(record.getPayloadBase64())
                .uploadedAt(record.getUploadedAt())
                .build();
    }
}