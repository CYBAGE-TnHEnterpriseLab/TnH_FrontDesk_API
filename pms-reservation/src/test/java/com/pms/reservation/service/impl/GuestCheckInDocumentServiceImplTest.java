package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.IdProofRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInIdProofRecord;
import com.pms.reservation.entity.ReservationCheckInSignatureRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInIdProofRepository;
import com.pms.reservation.repository.ReservationCheckInSignatureRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestCheckInDocumentServiceImplTest {

        private static final String VALID_PNG_BASE64 =
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Mock private ReservationBookingRepository reservationBookingRepository;
    @Mock private ReservationCheckInSignatureRepository signatureRepository;
    @Mock private ReservationCheckInIdProofRepository idProofRepository;
    @InjectMocks private GuestCheckInDocumentServiceImpl service;

    @Test
    void saveDigitalSignatureShouldPersistDocumentAgainstBooking() {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(10L).confirmationNumber("CONF-101").propertyId("PROPERTY-001").build();
        CheckInSignatureRequestDto request = new CheckInSignatureRequestDto();
        request.setBookingId(10L);
        request.setConfirmationNumber("CONF-101");
        request.setPropertyId("PROPERTY-001");
        request.setContentType("image/png");
        request.setPayloadBase64(VALID_PNG_BASE64);
        when(reservationBookingRepository.findByIdAndConfirmationNumber(10L, "CONF-101"))
                .thenReturn(Optional.of(booking));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(signatureRepository.save(any(ReservationCheckInSignatureRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveDigitalSignature(request);

        assertThat(response.getBookingId()).isEqualTo(10L);
        assertThat(response.getPayloadBase64()).isEqualTo(VALID_PNG_BASE64);
        verify(signatureRepository).save(any(ReservationCheckInSignatureRecord.class));
    }

    @Test
    void uploadIdProofShouldNormalizeSupportedType() {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(10L).confirmationNumber("CONF-101").propertyId("PROPERTY-001").build();
        IdProofRequestDto request = new IdProofRequestDto();
        request.setBookingId(10L);
        request.setConfirmationNumber("CONF-101");
        request.setPropertyId("PROPERTY-001");
        request.setIdProofType("passport");
        request.setIdProofNumber("P1234567");
        request.setContentType("image/png");
        request.setPayloadBase64(VALID_PNG_BASE64);
        when(reservationBookingRepository.findByIdAndConfirmationNumber(10L, "CONF-101"))
                .thenReturn(Optional.of(booking));
        when(idProofRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(idProofRepository.save(any(ReservationCheckInIdProofRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.uploadIdProofDetails(request);

        assertThat(response.getIdProofType()).isEqualTo("PASSPORT");
        verify(idProofRepository).save(any(ReservationCheckInIdProofRecord.class));
    }

    @Test
    void uploadIdProofShouldRejectUnsupportedType() {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(10L).confirmationNumber("CONF-101").propertyId("PROPERTY-001").build();
        IdProofRequestDto request = new IdProofRequestDto();
        request.setBookingId(10L);
        request.setConfirmationNumber("CONF-101");
        request.setPropertyId("PROPERTY-001");
        request.setIdProofType("VOTER_ID");
        request.setIdProofNumber("V1234567");
        request.setContentType("image/png");
        request.setPayloadBase64("aWQtcHJvb2Y=");
        when(reservationBookingRepository.findByIdAndConfirmationNumber(10L, "CONF-101"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.uploadIdProofDetails(request))
                .isInstanceOf(com.pms.guestlisting.exception.BadRequestException.class)
                .hasMessageContaining("idProofType must be");
    }

    @Test
    void saveDigitalSignatureShouldRejectNonImagePayload() {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(10L).confirmationNumber("CONF-101").propertyId("PROPERTY-001").build();
        CheckInSignatureRequestDto request = new CheckInSignatureRequestDto();
        request.setBookingId(10L);
        request.setConfirmationNumber("CONF-101");
        request.setPropertyId("PROPERTY-001");
        request.setContentType("image/png");
        request.setPayloadBase64("c2lnbmF0dXJl");
        when(reservationBookingRepository.findByIdAndConfirmationNumber(10L, "CONF-101"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.saveDigitalSignature(request))
                .isInstanceOf(com.pms.guestlisting.exception.BadRequestException.class)
                .hasMessageContaining("payload format");
    }
}