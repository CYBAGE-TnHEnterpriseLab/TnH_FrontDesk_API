package com.pms.guestlisting.service;

import com.pms.guestlisting.dto.DepartureResponseDto;
import com.pms.guestlisting.dto.DepartureSearchRequestDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.dto.SyncResultDto;
import java.time.LocalDate;

public interface DepartureService {

    SyncResultDto syncDepartures(String propertyId, LocalDate businessDate);

    PagedResponse<DepartureResponseDto> searchDepartures(DepartureSearchRequestDto request);
}

