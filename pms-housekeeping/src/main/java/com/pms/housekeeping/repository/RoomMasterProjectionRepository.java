package com.pms.housekeeping.repository;

import com.pms.housekeeping.entity.RoomMasterProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMasterProjectionRepository extends JpaRepository<RoomMasterProjection, Long> {

    List<RoomMasterProjection> findAllByPropertyId(String propertyId);

    Optional<RoomMasterProjection> findByPropertyIdAndRoomNumber(String propertyId, String roomNumber);

    void deleteByPropertyId(String propertyId);
}


