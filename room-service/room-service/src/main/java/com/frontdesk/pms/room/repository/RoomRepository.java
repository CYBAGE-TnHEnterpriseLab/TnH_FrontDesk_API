package com.frontdesk.pms.room.repository;

import com.frontdesk.pms.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByFloorId(Long floorId);

    List<Room> findByFloorIdAndPropertyId(Long floorId, Long propertyId);

    List<Room> findByPropertyId(Long propertyId);

    List<Room> findByRoomTypeId(Long roomTypeId);
}
