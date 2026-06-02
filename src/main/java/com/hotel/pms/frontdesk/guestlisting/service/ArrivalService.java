package com.hotel.pms.frontdesk.guestlisting.service;

import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalResponseDto;
import com.hotel.pms.frontdesk.guestlisting.dto.ArrivalSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.dto.PagedResponse;
import com.hotel.pms.frontdesk.guestlisting.dto.SyncResultDto;
import java.time.LocalDate;

public interface ArrivalService {

    SyncResultDto syncArrivals(String propertyId, LocalDate businessDate);

    PagedResponse<ArrivalResponseDto> searchArrivals(ArrivalSearchRequestDto request);
}
