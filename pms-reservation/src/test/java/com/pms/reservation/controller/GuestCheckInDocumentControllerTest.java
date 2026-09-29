package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.IdProofResponseDto;
import com.pms.reservation.service.GuestCheckInDocumentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GuestCheckInDocumentController.class,
        properties = {"security.jwt.enabled=false", "spring.data.jpa.repositories.enabled=false"})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GuestCheckInDocumentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private GuestCheckInDocumentService documentService;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void saveDigitalSignatureShouldUseExactEndpoint() throws Exception {
        when(documentService.saveDigitalSignature(any(), any(), any(), any()))
                .thenReturn(CheckInSignatureResponseDto.builder()
                .bookingId(10L).confirmationNumber("CONF-101").propertyId("PROPERTY-001")
                .contentType("image/png").payloadBase64("c2lnbmF0dXJl")
                .signedAt(LocalDateTime.of(2026, 9, 24, 10, 0)).build());

        mockMvc.perform(multipart("/api/v1/saveDigitalSignature")
                        .file(new MockMultipartFile("file", "signature.png", "image/png", "signature".getBytes()))
                        .param("bookingId", "10")
                        .param("confirmationNumber", "CONF-101")
                        .param("propertyId", "PROPERTY-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(10))
                .andExpect(jsonPath("$.data.confirmationNumber").value("CONF-101"));
    }

    @Test
    void getDigitalSignatureShouldUseExactEndpoint() throws Exception {
        when(documentService.getDigitalSignature(eq(10L), eq("CONF-101")))
                .thenReturn(CheckInSignatureResponseDto.builder().bookingId(10L).confirmationNumber("CONF-101")
                        .contentType("image/png").payloadBase64("c2lnbmF0dXJl").build());

        mockMvc.perform(get("/api/v1/getDetailsDigitalSignature")
                        .param("bookingId", "10")
                        .param("confirmationNumber", "CONF-101"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes("signature".getBytes()));
    }

    @Test
    void uploadIdProofDetailsShouldUseExactEndpoint() throws Exception {
        when(documentService.uploadIdProofDetails(any(), any(), any(), any(), any(), any()))
                .thenReturn(IdProofResponseDto.builder()
                .bookingId(10L).confirmationNumber("CONF-101").idProofType("PASSPORT").build());

        mockMvc.perform(multipart("/api/v1/uploadIdProofDetails")
                        .file(new MockMultipartFile("file", "id-proof.jpg", "image/jpeg", "id-proof".getBytes()))
                        .param("bookingId", "10")
                        .param("confirmationNumber", "CONF-101")
                        .param("propertyId", "PROPERTY-001")
                        .param("idProofType", "PASSPORT")
                        .param("idProofNumber", "P1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idProofType").value("PASSPORT"));
    }

    @Test
    void getUploadIdProofDetailsShouldUseExactEndpoint() throws Exception {
        when(documentService.getUploadIdProofDetails(eq(10L), eq("CONF-101")))
                .thenReturn(IdProofResponseDto.builder().bookingId(10L).confirmationNumber("CONF-101")
                        .contentType("image/jpeg").payloadBase64("aWQtcHJvb2Y=").build());

        mockMvc.perform(get("/api/v1/getUploadIdProofDetails")
                        .param("bookingId", "10")
                        .param("confirmationNumber", "CONF-101"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/jpeg"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes("id-proof".getBytes()));
    }
}