package com.pms.housekeeping.dto;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.entity.StatusChangeSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class HousekeepingValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator VALIDATOR;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void housekeepingRoomFilterRequest_shouldValidateRequiredAndRangeConstraints() {
        HousekeepingRoomFilterRequest request = new HousekeepingRoomFilterRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1,
                0,
                null,
                null
        );

        Set<String> paths = VALIDATOR.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(paths).contains("propertyId", "businessDate", "page", "size");
    }

    @Test
    void updateHousekeepingStatusRequest_shouldValidateRequiredFields() {
        UpdateHousekeepingStatusRequest request = new UpdateHousekeepingStatusRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<String> paths = VALIDATOR.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(paths).contains("propertyId", "businessDate", "sourceModule");
    }

    @Test
    void roomMasterSyncRequest_shouldValidateNestedRooms() {
        RoomMasterSyncRequest request = new RoomMasterSyncRequest(
                null,
                null,
                null,
                List.of(new RoomMasterSyncRequest.RoomMasterUnit(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        true
                ))
        );

        Set<String> paths = VALIDATOR.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(paths).contains("propertyId", "fromDate", "toDate", "rooms[0].roomTypeId", "rooms[0].roomTypeName", "rooms[0].roomNumber");
    }

    @Test
    void validRequests_shouldPassValidation() {
        HousekeepingRoomFilterRequest filterRequest = new HousekeepingRoomFilterRequest(
                UUID.randomUUID().toString(),
                LocalDate.of(2026, 8, 18),
                "suite",
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                0,
                25,
                null,
                null
        );

        UpdateHousekeepingStatusRequest updateRequest = new UpdateHousekeepingStatusRequest(
                UUID.randomUUID().toString(),
                LocalDate.of(2026, 8, 18),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StatusChangeSource.SYSTEM,
                null
        );

        RoomMasterSyncRequest syncRequest = new RoomMasterSyncRequest(
                UUID.randomUUID().toString(),
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 8, 20),
                List.of(new RoomMasterSyncRequest.RoomMasterUnit(
                        "13",
                        "Deluxe",
                        "101",
                        null,
                        null,
                        null,
                        null,
                        false,
                        true
                ))
        );

        assertThat(VALIDATOR.validate(filterRequest)).isEmpty();
        assertThat(VALIDATOR.validate(updateRequest)).isEmpty();
        assertThat(VALIDATOR.validate(syncRequest)).isEmpty();
    }
}



