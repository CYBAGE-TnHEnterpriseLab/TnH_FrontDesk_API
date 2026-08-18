package com.pms.guestlisting.service;

import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.dto.SyncResultDto;
import java.time.LocalDate;

public interface ArrivalService {

    public SyncResultDto syncArrivals(String propertyId, LocalDate businessDate);

    public PagedResponse<ArrivalResponseDto> searchArrivals(ArrivalSearchRequestDto request);
}

