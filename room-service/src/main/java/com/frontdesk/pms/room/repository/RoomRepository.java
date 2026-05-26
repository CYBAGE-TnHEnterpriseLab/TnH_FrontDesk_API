package com.frontdesk.pms.room.repository;

import com.frontdesk.pms.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByFloorId(Long floorId);

    List<Room> findByPropertyId(UUID propertyId);

    List<Room> findByRoomTypeId(Long roomTypeId);

    List<Room> findByFloorIdAndPropertyId(Long floorId, UUID propertyId);
}
