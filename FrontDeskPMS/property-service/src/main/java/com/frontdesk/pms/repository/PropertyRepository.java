package com.frontdesk.pms.repository;

import com.frontdesk.pms.entity.Property;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    boolean existsByEmail(String email);
    List<Property> findByNameIgnoreCase(String name);

}
