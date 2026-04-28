package com.frontdesk.pms.content.repository;

import com.frontdesk.pms.content.entity.PropertySpecialRequestsConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PropertySpecialRequestsConfigurationRepository extends JpaRepository<PropertySpecialRequestsConfiguration, UUID> {
    Optional<PropertySpecialRequestsConfiguration> findByPropertyId(UUID propertyId);
}
