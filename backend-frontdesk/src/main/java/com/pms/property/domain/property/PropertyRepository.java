package com.pms.property.domain.property;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<PropertyEntity, String> {

	List<PropertyEntity> findByCreatedBy(String createdBy);
}

