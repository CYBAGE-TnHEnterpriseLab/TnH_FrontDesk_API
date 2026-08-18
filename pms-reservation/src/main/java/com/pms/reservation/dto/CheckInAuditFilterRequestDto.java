package com.pms.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInAuditFilterRequestDto {
    String eventType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate toDate;

    int page;
    int size;
    String sortDir;
}
