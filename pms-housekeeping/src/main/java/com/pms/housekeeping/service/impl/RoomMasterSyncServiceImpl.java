package com.pms.housekeeping.service.impl;

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
import com.pms.housekeeping.service.RoomMasterSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomMasterSyncServiceImpl implements RoomMasterSyncService {

    private final RoomMasterProjectionRepository roomMasterProjectionRepository;
    private final HousekeepingRoomDayStatusRepository dayStatusRepository;

    public RoomMasterSyncServiceImpl(
            RoomMasterProjectionRepository roomMasterProjectionRepository,
            HousekeepingRoomDayStatusRepository dayStatusRepository
    ) {
        this.roomMasterProjectionRepository = roomMasterProjectionRepository;
        this.dayStatusRepository = dayStatusRepository;
    }

    @Override
    @Transactional
    public RoomMasterSyncResponse sync(RoomMasterSyncRequest request) {
        String propertyId = request.propertyId();
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

            updateProjection(projection, room, now);
            roomMasterProjectionRepository.save(projection);

            ensureDayStatusExistsForRange(propertyId, room, request.fromDate(), request.toDate(), now);
            synced++;
        }

        int deactivated = deactivateMissingRooms(existingByRoom, incomingRooms, now);
        return new RoomMasterSyncResponse(synced, deactivated);
    }

    private void updateProjection(
            RoomMasterProjection projection,
            RoomMasterSyncRequest.RoomMasterUnit room,
            LocalDateTime now
    ) {
        projection.setRoomTypeId(room.roomTypeId());
        projection.setRoomTypeName(room.roomTypeName());
        projection.setFloor(room.floor());
        projection.setZone(room.zone());
        projection.setRoomClass(room.roomClass());
        projection.setFeaturesCsv(room.featuresCsv());
        projection.setVipCapable(room.vipCapable());
        projection.setActive(room.active());
        projection.setUpdatedAt(now);
    }

    private void ensureDayStatusExistsForRange(
            String propertyId,
            RoomMasterSyncRequest.RoomMasterUnit room,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime now
    ) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            return;
        }

        for (LocalDate businessDate = fromDate;
             !businessDate.isAfter(toDate);
             businessDate = businessDate.plusDays(1)) {

            LocalDate finalBusinessDate = businessDate;
            HousekeepingRoomDayStatus status =
                    dayStatusRepository
                            .findByPropertyIdAndBusinessDateAndRoomNumber(
                                    propertyId,
                                    businessDate,
                                    room.roomNumber())
                            .orElseGet(() -> HousekeepingRoomDayStatus.builder()
                                    .propertyId(propertyId)
                                    .businessDate(finalBusinessDate)
                                    .roomNumber(room.roomNumber())
                                    .roomTypeId(room.roomTypeId())
                                    .roomTypeName(room.roomTypeName())
                                    .floor(room.floor())
                                    .cleaningStatus(CleaningStatus.DIRTY)
                                    .frontOfficeStatus(FrontOfficeStatus.VACANT)
                                    .reservationStatus(ReservationStatus.NOT_RESERVED)
                                    .priority(HousekeepingPriority.NORMAL)
                                    .sellable(false)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build());

            status.setRoomTypeId(room.roomTypeId());
            status.setRoomTypeName(room.roomTypeName());
            status.setFloor(room.floor());
            status.setUpdatedAt(now);
            status.setSellable(computeSellable(status));

            dayStatusRepository.save(status);
        }
    }

    private boolean computeSellable(HousekeepingRoomDayStatus status) {
        if (status.getCleaningStatus() == null || status.getFrontOfficeStatus() == null) {
            return false;
        }

        boolean clean = status.getCleaningStatus() == CleaningStatus.CLEAN
                || status.getCleaningStatus() == CleaningStatus.INSPECTED;
        boolean vacant = status.getFrontOfficeStatus() == FrontOfficeStatus.VACANT;
        boolean unassigned = status.getConfirmationId() == null;
        boolean available = status.getCleaningStatus() != CleaningStatus.OUT_OF_ORDER
                && status.getCleaningStatus() != CleaningStatus.OUT_OF_SERVICE;

        return clean && vacant && unassigned && available;
    }

    private int deactivateMissingRooms(
            Map<String, RoomMasterProjection> existingByRoom,
            Set<String> incomingRooms,
            LocalDateTime now
    ) {
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


