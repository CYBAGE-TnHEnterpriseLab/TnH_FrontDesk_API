package com.frontdesk.pms.repository;

import com.frontdesk.pms.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // Check if property name already exists 
    Optional<Property> findByPropertyName(String propertyName);
}