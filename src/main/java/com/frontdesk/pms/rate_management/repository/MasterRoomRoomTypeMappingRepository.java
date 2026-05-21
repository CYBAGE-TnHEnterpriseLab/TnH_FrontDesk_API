package com.frontdesk.pms.rate_management.repository;

import com.frontdesk.pms.rate_management.entity.MasterRoomRoomTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MasterRoomRoomTypeMappingRepository extends JpaRepository<MasterRoomRoomTypeMapping, Long> {
	List<MasterRoomRoomTypeMapping> findByMasterRoomId(Long masterRoomId);
}