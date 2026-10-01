package com.frontdesk.pms.rate_management.dto;

import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class MasterRoomResponseDTO {
    private Long id;
    private String propertyId;
    private String name;
    private MasterRoomMealOption mealOption;
    private String inclusion;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<MasterRoomPricingResponseDTO> pricingList;
    private List<MasterRoomRoomTypeMappingResponseDTO> roomTypeMappings;
}