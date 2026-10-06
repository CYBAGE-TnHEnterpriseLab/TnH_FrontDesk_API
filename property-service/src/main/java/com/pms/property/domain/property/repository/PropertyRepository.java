package com.pms.property.domain.property.repository;

import com.pms.property.domain.property.entity.PropertyEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<PropertyEntity, String> {

    List<PropertyEntity> findByCreatedBy(UUID createdBy);

    boolean existsByPropertyCodeAndIdNot(String propertyCode, String id);
}


