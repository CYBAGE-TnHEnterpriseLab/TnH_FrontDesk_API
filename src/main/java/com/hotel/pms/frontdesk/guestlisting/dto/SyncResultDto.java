package com.hotel.pms.frontdesk.guestlisting.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SyncResultDto {
    String propertyId;
    LocalDate businessDate;
    int fetchedCount;
    int upsertedCount;
}
