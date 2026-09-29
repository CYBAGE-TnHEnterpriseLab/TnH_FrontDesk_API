package com.pms.reservation.service;

import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.IdProofRequestDto;
import com.pms.reservation.dto.IdProofResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface GuestCheckInDocumentService {

    CheckInSignatureResponseDto saveDigitalSignature(CheckInSignatureRequestDto request);

    CheckInSignatureResponseDto saveDigitalSignature(Long bookingId, String confirmationNumber,
                                                       String propertyId, MultipartFile file);

    CheckInSignatureResponseDto getDigitalSignature(Long bookingId, String confirmationNumber);

    IdProofResponseDto uploadIdProofDetails(IdProofRequestDto request);

    IdProofResponseDto uploadIdProofDetails(Long bookingId, String confirmationNumber, String propertyId,
                                            String idProofType, String idProofNumber, MultipartFile file);

    IdProofResponseDto getUploadIdProofDetails(Long bookingId, String confirmationNumber);
}