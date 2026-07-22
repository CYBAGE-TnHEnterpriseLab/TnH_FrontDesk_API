package com.pms.property.integration.inventory.repository;

import com.pms.property.integration.inventory.entity.InventorySyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventorySyncStateRepository extends JpaRepository<InventorySyncStateEntity, String> {
}

