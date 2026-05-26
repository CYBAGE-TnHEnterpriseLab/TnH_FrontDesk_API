package com.frontdesk.pms.room.repository;

import com.frontdesk.pms.room.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    Optional<RoomType> findByName(String name);

    Optional<RoomType> findByNameAndPropertyId(String name, UUID propertyId);

    List<RoomType> findByPropertyId(UUID propertyId);
}
