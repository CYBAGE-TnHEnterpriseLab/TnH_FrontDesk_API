package com.hotel.pms.frontdesk.guestlisting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalResponseDto;
import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.dto.PagedResponse;
import com.hotel.pms.frontdesk.guestlisting.exception.GlobalExceptionHandler;
import com.hotel.pms.frontdesk.guestlisting.service.ArrivalService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ArrivalController.class)
@Import(GlobalExceptionHandler.class)
class ArrivalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArrivalService arrivalService;

    @Test
    void getArrivalsShouldReturnPagedResponse() throws Exception {
        ArrivalResponseDto dto = ArrivalResponseDto.builder()
                .id(1L)
                .confirmationNumber("CNF458721")
                .firstName("John")
                .lastName("Smith")
                .build();

        PagedResponse<ArrivalResponseDto> response = PagedResponse.<ArrivalResponseDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(100)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .sortBy("checkInDate")
                .sortDir("asc")
                .build();

        when(arrivalService.searchArrivals(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/arrivals/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01")
                        .param("search", "smith")
                        .param("status", "DNM")
                        .param("reservationType", "Guaranteed")
                        .param("city", "Mumbai")
                        .param("roomStatus", "Clean")
                        .param("corporateCode", "CORP001")
                        .param("roomType", "Deluxe King")
                        .param("company", "ABC Travels")
                        .param("sharingStatus", "Y")
                        .param("loyaltyMembershipStatus", "Gold Member")
                        .param("page", "1")
                        .param("size", "25")
                        .param("sortBy", "lastName")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].confirmationNumber").value("CNF458721"));

        ArgumentCaptor<ArrivalSearchRequestDto> requestCaptor = ArgumentCaptor.forClass(ArrivalSearchRequestDto.class);
        verify(arrivalService).searchArrivals(requestCaptor.capture());

        ArrivalSearchRequestDto captured = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("PROP001", captured.getPropertyId());
        org.junit.jupiter.api.Assertions.assertEquals("smith", captured.getSearch());
        org.junit.jupiter.api.Assertions.assertEquals("DNM", captured.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Guaranteed", captured.getReservationType());
        org.junit.jupiter.api.Assertions.assertEquals("Mumbai", captured.getCity());
        org.junit.jupiter.api.Assertions.assertEquals("Clean", captured.getRoomStatus());
        org.junit.jupiter.api.Assertions.assertEquals("CORP001", captured.getCorporateCode());
        org.junit.jupiter.api.Assertions.assertEquals("Deluxe King", captured.getRoomType());
        org.junit.jupiter.api.Assertions.assertEquals("ABC Travels", captured.getCompany());
        org.junit.jupiter.api.Assertions.assertEquals("Y", captured.getSharingStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Gold Member", captured.getLoyaltyMembershipStatus());
        org.junit.jupiter.api.Assertions.assertEquals(1, captured.getPage());
        org.junit.jupiter.api.Assertions.assertEquals(25, captured.getSize());
        org.junit.jupiter.api.Assertions.assertEquals("lastName", captured.getSortBy());
        org.junit.jupiter.api.Assertions.assertEquals("desc", captured.getSortDir());
    }

        @Test
        void getArrivalsShouldReturnBadRequestWhenPropertyIdIsMissing() throws Exception {
                mockMvc.perform(get("/api/v1/arrivals/list")
                                                .param("businessDate", "2026-06-01"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void getArrivalsShouldReturnBadRequestWhenBusinessDateFormatIsInvalid() throws Exception {
                mockMvc.perform(get("/api/v1/arrivals/list")
                                                .param("propertyId", "PROP001")
                                                .param("businessDate", "06-01-2026"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

}
