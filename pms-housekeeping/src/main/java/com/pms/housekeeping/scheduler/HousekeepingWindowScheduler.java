package com.pms.housekeeping.scheduler;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import com.pms.housekeeping.service.RoomMasterSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HousekeepingWindowScheduler {

    private final RoomMasterProjectionRepository roomMasterProjectionRepository;
    private final RoomMasterSyncService roomMasterSyncService;
    private final int horizonDays;

    public HousekeepingWindowScheduler(
            RoomMasterProjectionRepository roomMasterProjectionRepository,
            RoomMasterSyncService roomMasterSyncService,
            @Value("${housekeeping.window.horizon-days:90}") int horizonDays
    ) {
        this.roomMasterProjectionRepository = roomMasterProjectionRepository;
        this.roomMasterSyncService = roomMasterSyncService;
        this.horizonDays = Math.max(horizonDays, 1);
    }

    // Runs every day at 00:10 and adds one new tail day to keep a rolling window.
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void addTailDayForAllProperties() {
        LocalDate nextTailDay = LocalDate.now().plusDays(horizonDays);
        LocalDate toDateExclusive = nextTailDay.plusDays(1);

        Map<String, List<RoomMasterProjection>> byProperty = new HashMap<>();
        for (RoomMasterProjection projection : roomMasterProjectionRepository.findAll()) {
            if (!projection.isActive()) {
                continue;
            }
            byProperty.computeIfAbsent(projection.getPropertyId(), ignored -> new ArrayList<>()).add(projection);
        }

        for (Map.Entry<String, List<RoomMasterProjection>> entry : byProperty.entrySet()) {
            List<RoomMasterSyncRequest.RoomMasterUnit> rooms = entry.getValue().stream()
                    .map(this::toRoomUnit)
                    .toList();

            if (rooms.isEmpty()) {
                continue;
            }

            RoomMasterSyncRequest request = new RoomMasterSyncRequest(
                    entry.getKey(),
                    nextTailDay,
                    toDateExclusive,
                    rooms
            );
            roomMasterSyncService.sync(request);
        }
    }

    private RoomMasterSyncRequest.RoomMasterUnit toRoomUnit(RoomMasterProjection projection) {
        return new RoomMasterSyncRequest.RoomMasterUnit(
                projection.getRoomTypeId(),
                projection.getRoomTypeName(),
                projection.getRoomNumber(),
                projection.getFloor(),
                projection.getZone(),
                projection.getRoomClass(),
                projection.getFeaturesCsv(),
                projection.isVipCapable(),
                projection.isActive()
        );
    }
}

