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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public void deletePropertyData(String propertyId) {
        dayStatusRepository.deleteByPropertyId(propertyId);
        roomMasterProjectionRepository.deleteByPropertyId(propertyId);
    }

    @Override
    @Transactional
    public RoomMasterSyncResponse sync(RoomMasterSyncRequest request) {
        String propertyId = request.propertyId();
        LocalDateTime now = LocalDateTime.now();

        Map<String, RoomMasterProjection> existingByRoom = roomMasterProjectionRepository
                .findAllByPropertyId(propertyId)
                .stream()
                .collect(Collectors.toMap(
                        RoomMasterProjection::getRoomNumber,
                        projection -> projection,
                        (left, right) -> left,
                        HashMap::new));

        Set<String> incomingRooms = request.rooms().stream()
                .map(RoomMasterSyncRequest.RoomMasterUnit::roomNumber)
                .collect(Collectors.toSet());

        // One bulk read for the whole date range instead of N*D point lookups.
        Map<LocalDate, Map<String, HousekeepingRoomDayStatus>> existingDayByDateRoom =
                loadExistingDayStatus(propertyId, request.fromDate(), request.toDate());

        // Build the upsert entities for each room sequentially on the transaction
        // thread. The maps above contain JPA-managed entities from the current
        // persistence context, which is not thread-safe, so mutating them must
        // happen on the calling/transaction thread.
        List<RoomMasterProjection> projectionsToSave = new ArrayList<>();
        List<HousekeepingRoomDayStatus> dayStatusesToSave = new ArrayList<>();
        for (RoomMasterSyncRequest.RoomMasterUnit room : request.rooms()) {
            RoomSyncResult result = buildRoomSyncResult(
                    room,
                    existingByRoom,
                    existingDayByDateRoom,
                    propertyId,
                    request.fromDate(),
                    request.toDate(),
                    now);
            projectionsToSave.add(result.projection());
            dayStatusesToSave.addAll(result.dayStatuses());
        }

        // Deactivate rooms present in the DB but missing from the incoming payload.
        List<RoomMasterProjection> deactivatedToSave = deactivateMissingRooms(
                existingByRoom, incomingRooms, now);

        // Batched writes: a handful of statements instead of N + N*D round-trips.
        roomMasterProjectionRepository.saveAll(projectionsToSave);
        if (!deactivatedToSave.isEmpty()) {
            roomMasterProjectionRepository.saveAll(deactivatedToSave);
        }
        if (!dayStatusesToSave.isEmpty()) {
            dayStatusRepository.saveAll(dayStatusesToSave);
        }

        return new RoomMasterSyncResponse(projectionsToSave.size(), deactivatedToSave.size());
    }

    private RoomSyncResult buildRoomSyncResult(
            RoomMasterSyncRequest.RoomMasterUnit room,
            Map<String, RoomMasterProjection> existingByRoom,
            Map<LocalDate, Map<String, HousekeepingRoomDayStatus>> existingDayByDateRoom,
            String propertyId,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime now
    ) {
        RoomMasterProjection projection = existingByRoom.get(room.roomNumber());
        if (projection == null) {
            projection = RoomMasterProjection.builder()
                    .propertyId(propertyId)
                    .roomNumber(room.roomNumber())
                    .createdAt(now)
                    .build();
        }
        updateProjection(projection, room, now);

        List<HousekeepingRoomDayStatus> dayStatuses = new ArrayList<>();
        if (fromDate != null && toDate != null && !fromDate.isAfter(toDate)) {
            for (LocalDate businessDate = fromDate;
                 !businessDate.isAfter(toDate);
                 businessDate = businessDate.plusDays(1)) {

                Map<String, HousekeepingRoomDayStatus> byRoom = existingDayByDateRoom.get(businessDate);
                HousekeepingRoomDayStatus existing = byRoom == null ? null : byRoom.get(room.roomNumber());

                HousekeepingRoomDayStatus status;
                if (existing == null) {
                    status = HousekeepingRoomDayStatus.builder()
                            .propertyId(propertyId)
                            .businessDate(businessDate)
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
                            .build();
                } else {
                    status = existing;
                    status.setRoomTypeId(room.roomTypeId());
                    status.setRoomTypeName(room.roomTypeName());
                    status.setFloor(room.floor());
                    status.setUpdatedAt(now);
                }
                status.setSellable(computeSellable(status));
                dayStatuses.add(status);
            }
        }

        return new RoomSyncResult(projection, dayStatuses);
    }

    private Map<LocalDate, Map<String, HousekeepingRoomDayStatus>> loadExistingDayStatus(
            String propertyId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Map<LocalDate, Map<String, HousekeepingRoomDayStatus>> result = new HashMap<>();
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            return result;
        }

        List<HousekeepingRoomDayStatus> all = dayStatusRepository
                .findAllByPropertyIdAndBusinessDateBetween(propertyId, fromDate, toDate);
        for (HousekeepingRoomDayStatus status : all) {
            result.computeIfAbsent(status.getBusinessDate(), ignored -> new HashMap<>())
                    .put(status.getRoomNumber(), status);
        }
        return result;
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

    private List<RoomMasterProjection> deactivateMissingRooms(
            Map<String, RoomMasterProjection> existingByRoom,
            Set<String> incomingRooms,
            LocalDateTime now
    ) {
        List<RoomMasterProjection> deactivated = new ArrayList<>();
        for (Map.Entry<String, RoomMasterProjection> entry : existingByRoom.entrySet()) {
            if (!incomingRooms.contains(entry.getKey())) {
                RoomMasterProjection projection = entry.getValue();
                if (projection.isActive()) {
                    projection.setActive(false);
                    projection.setUpdatedAt(now);
                    deactivated.add(projection);
                }
            }
        }
        return deactivated;
    }

    private record RoomSyncResult(
            RoomMasterProjection projection,
            List<HousekeepingRoomDayStatus> dayStatuses
    ) {
    }
}
