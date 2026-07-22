package com.pms.housekeeping.service;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.dto.response.RoomMasterSyncResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusRepository;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomMasterSyncService {

    private final RoomMasterProjectionRepository roomMasterProjectionRepository;
    private final HousekeepingRoomDayStatusRepository dayStatusRepository;

    public RoomMasterSyncService(
            RoomMasterProjectionRepository roomMasterProjectionRepository,
            HousekeepingRoomDayStatusRepository dayStatusRepository
    ) {
        this.roomMasterProjectionRepository = roomMasterProjectionRepository;
        this.dayStatusRepository = dayStatusRepository;
    }

    @Transactional
    public RoomMasterSyncResponse sync(RoomMasterSyncRequest request) {
        UUID propertyId = request.propertyId();
        LocalDateTime now = LocalDateTime.now();

        Map<String, RoomMasterProjection> existingByRoom = roomMasterProjectionRepository.findAllByPropertyId(propertyId)
                .stream()
                .collect(Collectors.toMap(RoomMasterProjection::getRoomNumber, projection -> projection, (left, right) -> left, HashMap::new));

        Set<String> incomingRooms = request.rooms().stream()
                .map(RoomMasterSyncRequest.RoomMasterUnit::roomNumber)
                .collect(Collectors.toSet());

        int synced = 0;
        for (RoomMasterSyncRequest.RoomMasterUnit room : request.rooms()) {
            RoomMasterProjection projection = existingByRoom.get(room.roomNumber());
            if (projection == null) {
                projection = RoomMasterProjection.builder()
                        .propertyId(propertyId)
                        .roomNumber(room.roomNumber())
                        .createdAt(now)
                        .build();
            }

            projection.setRoomTypeId(room.roomTypeId());
            projection.setRoomTypeName(room.roomTypeName());
            projection.setFloor(room.floor());
            projection.setZone(room.zone());
            projection.setRoomClass(room.roomClass());
            projection.setFeaturesCsv(room.featuresCsv());
            projection.setVipCapable(room.vipCapable());
            projection.setActive(room.active());
            projection.setUpdatedAt(now);
            roomMasterProjectionRepository.save(projection);

            ensureDayStatusExistsForRange(propertyId, room, request.fromDate(), request.toDate(), now);
            synced++;
        }

        int deactivated = deactivateMissingRooms(existingByRoom, incomingRooms, now);
        return new RoomMasterSyncResponse(synced, deactivated);
    }

    private void ensureDayStatusExistsForRange(
            UUID propertyId,
            RoomMasterSyncRequest.RoomMasterUnit room,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime now
    ) {
        if (!toDate.isAfter(fromDate)) {
            return;
        }

        for (LocalDate businessDate = fromDate; businessDate.isBefore(toDate); businessDate = businessDate.plusDays(1)) {
            LocalDate targetDate = businessDate;
            HousekeepingRoomDayStatus status = dayStatusRepository
                    .findByPropertyIdAndBusinessDateAndRoomNumber(propertyId, targetDate, room.roomNumber())
                    .orElseGet(() -> HousekeepingRoomDayStatus.builder()
                            .propertyId(propertyId)
                            .businessDate(targetDate)
                            .roomNumber(room.roomNumber())
                            .createdAt(now)
                            .build());

            status.setRoomTypeId(room.roomTypeId());
            if (status.getCleaningStatus() == null) {
                status.setCleaningStatus(CleaningStatus.DIRTY);
                status.setStatusChangedAt(now);
            }
            if (status.getFrontOfficeStatus() == null) {
                status.setFrontOfficeStatus(FrontOfficeStatus.VACANT);
                status.setFoStatusChangedAt(now);
            }
            if (status.getReservationStatus() == null) {
                status.setReservationStatus(ReservationStatus.NOT_RESERVED);
                status.setReservationStatusChangedAt(now);
            }
            if (status.getPriority() == null) {
                status.setPriority(HousekeepingPriority.NORMAL);
            }
            status.setSellable(computeSellable(status));
            status.setUpdatedAt(now);
            dayStatusRepository.save(status);
        }
    }

    private boolean computeSellable(HousekeepingRoomDayStatus status) {
        boolean cleanEnough = status.getCleaningStatus() == CleaningStatus.CLEAN
                || status.getCleaningStatus() == CleaningStatus.INSPECTED;
        boolean vacant = status.getFrontOfficeStatus() == FrontOfficeStatus.VACANT;
        boolean noAssignment = status.getAssignedReservationId() == null;
        boolean notOut = status.getCleaningStatus() != CleaningStatus.OUT_OF_ORDER
                && status.getCleaningStatus() != CleaningStatus.OUT_OF_SERVICE;
        return cleanEnough && vacant && noAssignment && notOut;
    }

    private int deactivateMissingRooms(Map<String, RoomMasterProjection> existingByRoom, Set<String> incomingRooms, LocalDateTime now) {
        int deactivated = 0;
        for (Map.Entry<String, RoomMasterProjection> entry : existingByRoom.entrySet()) {
            if (!incomingRooms.contains(entry.getKey())) {
                RoomMasterProjection projection = entry.getValue();
                if (projection.isActive()) {
                    projection.setActive(false);
                    projection.setUpdatedAt(now);
                    roomMasterProjectionRepository.save(projection);
                    deactivated++;
                }
            }
        }
        return deactivated;
    }
}


