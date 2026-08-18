package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.ReservationRoomCalendarResponseDto;
import com.pms.reservation.service.ReservationRoomCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reservation Room Calendar", description = "APIs for room-wise calendar during reservation assignment")
public class ReservationRoomCalendarController {

    private final ReservationRoomCalendarService reservationRoomCalendarService;

    @GetMapping("/rooms/calendar")
    @Operation(summary = "Get room assignment calendar",
            description = "Returns room-wise date calendar with booking and housekeeping statuses")
    public ResponseEntity<ApiResponse<ReservationRoomCalendarResponseDto>> getRoomCalendar(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
                        @RequestParam(required = false) List<String> roomTypes,
                        @RequestParam(required = false) String roomType
    ) {
                List<String> effectiveRoomTypes = mergeRoomTypes(roomTypes, roomType);
        ReservationRoomCalendarResponseDto response = reservationRoomCalendarService.getRoomCalendar(
                propertyId,
                arrivalDate,
                departureDate,
                                effectiveRoomTypes
        );
        return ResponseEntity.ok(ApiResponse.success("Room calendar fetched successfully", response));
    }

        private List<String> mergeRoomTypes(List<String> roomTypes, String roomType) {
                LinkedHashSet<String> merged = new LinkedHashSet<>();

                if (roomTypes != null) {
                        for (String item : roomTypes) {
                                if (!StringUtils.hasText(item)) {
                                        continue;
                                }
                                merged.add(item.trim());
                        }
                }

                if (StringUtils.hasText(roomType)) {
                        merged.add(roomType.trim());
                }

                return merged.isEmpty() ? List.of() : new ArrayList<>(merged);
        }
}
