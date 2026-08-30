package com.pms.property.draft.repository;

import com.pms.property.draft.entity.PropertyDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDraftRepository extends JpaRepository<PropertyDraftEntity, Long> {
}

