package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.IdProofResponseDto;
import com.pms.reservation.service.GuestCheckInDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Guest Check-In Documents", description = "Digital signature and ID proof APIs")
public class GuestCheckInDocumentController {

    private final GuestCheckInDocumentService documentService;

    @PostMapping("/saveDigitalSignature")
    @Operation(summary = "Save guest digital signature")
    public ResponseEntity<ApiResponse<CheckInSignatureResponseDto>> saveDigitalSignature(
                        @RequestParam Long bookingId,
                        @RequestParam String confirmationNumber,
                        @RequestParam String propertyId,
                        @RequestParam(defaultValue = "FRONT_DESK") String checkInChannel,
                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                                "Digital signature saved successfully",
                                documentService.saveDigitalSignature(bookingId, confirmationNumber, propertyId,
                                    checkInChannel, file)));
    }

    @GetMapping("/getDetailsDigitalSignature")
    @Operation(summary = "Get guest digital signature")
        public ResponseEntity<byte[]> getDetailsDigitalSignature(
            @RequestParam Long bookingId,
            @RequestParam String confirmationNumber) {
                CheckInSignatureResponseDto response = documentService.getDigitalSignature(bookingId, confirmationNumber);
                return imageResponse(response.getContentType(), response.getPayloadBase64(), "signature");
    }

    @PostMapping("/uploadIdProofDetails")
    @Operation(summary = "Upload guest ID proof details")
    public ResponseEntity<ApiResponse<IdProofResponseDto>> uploadIdProofDetails(
            @RequestParam Long bookingId,
            @RequestParam String confirmationNumber,
            @RequestParam String propertyId,
            @RequestParam String idProofType,
            @RequestParam String idProofNumber,
            @RequestParam(defaultValue = "FRONT_DESK") String checkInChannel,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "ID proof details uploaded successfully",
                documentService.uploadIdProofDetails(bookingId, confirmationNumber, propertyId,
                    idProofType, idProofNumber, checkInChannel, file)));
    }

    @GetMapping("/getUploadIdProofDetails")
    @Operation(summary = "Get guest ID proof details")
        public ResponseEntity<byte[]> getUploadIdProofDetails(
            @RequestParam Long bookingId,
            @RequestParam String confirmationNumber) {
                IdProofResponseDto response = documentService.getUploadIdProofDetails(bookingId, confirmationNumber);
                return imageResponse(response.getContentType(), response.getPayloadBase64(), "id-proof");
        }

        private ResponseEntity<byte[]> imageResponse(String contentType, String payloadBase64, String filename) {
                MediaType mediaType = MediaType.parseMediaType(contentType);
                byte[] image = Base64.getDecoder().decode(payloadBase64);
                return ResponseEntity.ok()
                                .contentType(mediaType)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                                .body(image);
    }
}