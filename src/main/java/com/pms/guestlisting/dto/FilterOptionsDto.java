package com.pms.guestlisting.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FilterOptionsDto {
    List<String> reservationStatuses;
    List<String> roomStatuses;
    List<String> stayTypes;
    List<String> roomTypes;
    List<Integer> floors;
    List<String> loyalties;
    List<String> vips;
}

