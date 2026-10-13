package com.frontdesk.pms.rate_management.mapper;

import com.frontdesk.pms.rate_management.dto.MasterRoomRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomResponseDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoom;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasterRoomMapperTest {

    private final MasterRoomMapper mapper = new MasterRoomMapper();

    @Test
    void toEntity_shouldPreserveEveryMealCombination() {
        for (MasterRoomMealOption mealOption : MasterRoomMealOption.values()) {
            MasterRoomRequestDTO request = new MasterRoomRequestDTO();
            request.setName("Template");
            request.setMealOption(mealOption);

            assertEquals(mealOption, mapper.toEntity(request).getMealOption());
        }
    }

    @Test
    void toEntity_shouldDefaultMissingMealOptionToNoMeals() {
        MasterRoomRequestDTO request = new MasterRoomRequestDTO();
        request.setName("Template");

        assertEquals(MasterRoomMealOption.NO_MEALS, mapper.toEntity(request).getMealOption());
    }

    @Test
    void toResponseDTO_shouldDefaultLegacyMissingMealOptionToNoMeals() {
        MasterRoom entity = new MasterRoom();
        entity.setMealOption(null);

        MasterRoomResponseDTO response = mapper.toResponseDTO(entity);

        assertEquals(MasterRoomMealOption.NO_MEALS, response.getMealOption());
    }
}
