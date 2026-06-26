package com.pms.guestlisting.service;

import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.dto.SyncResultDto;
import java.time.LocalDate;

public interface ArrivalService {

    SyncResultDto syncArrivals(String propertyId, LocalDate businessDate);

    PagedResponse<ArrivalResponseDto> searchArrivals(ArrivalSearchRequestDto request);
}

