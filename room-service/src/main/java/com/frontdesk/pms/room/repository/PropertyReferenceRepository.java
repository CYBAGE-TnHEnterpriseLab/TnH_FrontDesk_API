package com.frontdesk.pms.room.repository;

import com.frontdesk.pms.room.entity.PropertyReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyReferenceRepository extends JpaRepository<PropertyReference, UUID> {
}
