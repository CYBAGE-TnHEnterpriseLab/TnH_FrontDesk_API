package com.pms.guestlisting.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.dto.DepartureResponseDto;
import com.pms.guestlisting.dto.DepartureSearchRequestDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.guestlisting.service.ArrivalService;
import com.pms.guestlisting.service.DepartureService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GuestListingController.class)
@Import(GlobalExceptionHandler.class)
class GuestListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArrivalService arrivalService;

    @MockBean
    private DepartureService departureService;

    @Test
    void getGuestListingShouldReturnArrivalsByDefault() throws Exception {
        ArrivalResponseDto dto = ArrivalResponseDto.builder()
                .id(1L)
                .confirmationNumber("CNF458721")
                .firstName("John")
                .lastName("Smith")
                .build();

        PagedResponse<ArrivalResponseDto> response = PagedResponse.<ArrivalResponseDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .sortBy("checkInDate")
                .sortDir("asc")
                .build();

        when(arrivalService.searchArrivals(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/guest-listing/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Guest listing fetched successfully"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].listingType").value("ARRIVAL"))
                .andExpect(jsonPath("$.data.content[0].confirmationNumber").value("CNF458721"));

        ArgumentCaptor<ArrivalSearchRequestDto> requestCaptor = ArgumentCaptor.forClass(ArrivalSearchRequestDto.class);
        verify(arrivalService).searchArrivals(requestCaptor.capture());
        verify(departureService, never()).searchDepartures(any());

        ArrivalSearchRequestDto captured = requestCaptor.getValue();
        assertEquals("PROP001", captured.getPropertyId());
        assertEquals("checkInDate", captured.getSortBy());
    }

    @Test
    void getGuestListingShouldReturnDeparturesWhenViewIsDepartures() throws Exception {
        DepartureResponseDto dto = DepartureResponseDto.builder()
                .id(2L)
                .confirmationNumber("CNF999001")
                .firstName("Emma")
                .lastName("Stone")
                .build();

        PagedResponse<DepartureResponseDto> response = PagedResponse.<DepartureResponseDto>builder()
                .content(List.of(dto))
                .page(1)
                .size(25)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .sortBy("checkOutDate")
                .sortDir("desc")
                .build();

        when(departureService.searchDepartures(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/guest-listing/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01")
                        .param("view", "departures")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].listingType").value("DEPARTURE"))
                .andExpect(jsonPath("$.data.content[0].confirmationNumber").value("CNF999001"));

        ArgumentCaptor<DepartureSearchRequestDto> requestCaptor = ArgumentCaptor.forClass(DepartureSearchRequestDto.class);
        verify(departureService).searchDepartures(requestCaptor.capture());
        verify(arrivalService, never()).searchArrivals(any());

        DepartureSearchRequestDto captured = requestCaptor.getValue();
        assertEquals("PROP001", captured.getPropertyId());
        assertEquals("checkOutDate", captured.getSortBy());
        assertEquals("desc", captured.getSortDir());
    }

    @Test
    void getGuestListingShouldReturnBadRequestWhenViewIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/guest-listing/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01")
                        .param("view", "both"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
