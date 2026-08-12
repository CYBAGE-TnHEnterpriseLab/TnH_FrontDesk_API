package com.pms.housekeeping.dto.request;

import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.ReservationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HousekeepingRoomFilterRequest(

        @NotNull
        UUID propertyId,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate businessDate,

//        LocalDate fromDate,
//        LocalDate toDate,

        String search,

        List<CleaningStatus> cleaningStatus,

        List<FrontOfficeStatus> frontOfficeStatus,

        List<ReservationStatus> reservationStatus,

        UUID roomTypeId,

        String floor,

        String attendant,

        HousekeepingPriority priority,

        @Min(0)
        Integer page,

        @Min(1)
        @Max(200)
        Integer size,

        String sortBy,

        String sortDir

) {}
