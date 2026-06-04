package com.hotel.pms.frontdesk.guestlisting.service;

import com.hotel.pms.frontdesk.guestlisting.dto.DepartureResponseDto;
import com.hotel.pms.frontdesk.guestlisting.dto.DepartureSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.dto.PagedResponse;
import com.hotel.pms.frontdesk.guestlisting.dto.SyncResultDto;
import java.time.LocalDate;

public interface DepartureService {

    SyncResultDto syncDepartures(String propertyId, LocalDate businessDate);

    PagedResponse<DepartureResponseDto> searchDepartures(DepartureSearchRequestDto request);
}
