package com.frontdesk.pms.content.repository;

import com.frontdesk.pms.content.entity.PropertyReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyReferenceRepository extends JpaRepository<PropertyReference, UUID> {
}
