package com.frontdesk.pms.rate_management.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MasterRoomMealOptionTest {

    @Test
    void fromValue_shouldAcceptAllMealCombinations() {
        assertEquals(MasterRoomMealOption.NO_MEALS, MasterRoomMealOption.fromValue("no meals"));
        assertEquals(MasterRoomMealOption.BREAKFAST, MasterRoomMealOption.fromValue("breakfast"));
        assertEquals(MasterRoomMealOption.LUNCH, MasterRoomMealOption.fromValue("lunch"));
        assertEquals(MasterRoomMealOption.DINNER, MasterRoomMealOption.fromValue("dinner"));
        assertEquals(MasterRoomMealOption.BREAKFAST_AND_LUNCH,
                MasterRoomMealOption.fromValue("breakfast + lunch"));
        assertEquals(MasterRoomMealOption.BREAKFAST_AND_DINNER,
                MasterRoomMealOption.fromValue("breakfast and dinner"));
        assertEquals(MasterRoomMealOption.LUNCH_AND_DINNER,
                MasterRoomMealOption.fromValue("lunch and dinner"));
        assertEquals(MasterRoomMealOption.ALL_MEALS, MasterRoomMealOption.fromValue("all meals"));
    }

    @Test
    void fromValue_shouldRejectUnsupportedMealValue() {
        assertThrows(IllegalArgumentException.class,
                () -> MasterRoomMealOption.fromValue("brunch"));
    }
}
