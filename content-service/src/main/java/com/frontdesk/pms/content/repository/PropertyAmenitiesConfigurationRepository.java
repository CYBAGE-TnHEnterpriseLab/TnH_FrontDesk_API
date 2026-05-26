package com.frontdesk.pms.content.repository;

import com.frontdesk.pms.content.entity.PropertyAmenitiesConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PropertyAmenitiesConfigurationRepository extends JpaRepository<PropertyAmenitiesConfiguration, UUID> {
    Optional<PropertyAmenitiesConfiguration> findByPropertyId(UUID propertyId);
}
