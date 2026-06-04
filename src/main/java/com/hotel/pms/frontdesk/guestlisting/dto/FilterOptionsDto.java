package com.hotel.pms.frontdesk.guestlisting.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FilterOptionsDto {
    List<String> statuses;
    List<String> reservationTypes;
    List<String> cities;
    List<String> roomStatuses;
    List<String> roomTypes;
    List<String> companies;
    List<String> loyaltyMembershipStatuses;
    List<String> sortFields;
}
