package com.pms.property.domain.room.repository;

import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomOutletTypeRepository extends JpaRepository<RoomOutletTypeEntity, Long> {

	long countByPropertyId(String propertyId);

	List<RoomOutletTypeEntity> findAllByPropertyId(String propertyId);

	long deleteByPropertyId(String propertyId);
}


