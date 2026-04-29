package com.frontdesk.pms.room.repository;

import com.frontdesk.pms.room.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {
	List<Floor> findByPropertyId(java.util.UUID propertyId);
}