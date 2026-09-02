package com.pms.reservation.integration;

import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import java.time.LocalDate;
import java.util.List;

public interface PropertyInventoryPort {

    List<PropertyRoomOutletTypeDto> fetchRoomOutletTypes(String propertyId);

    List<PropertyTaxRuleResponseDto> fetchTaxRules(String propertyId);
}
