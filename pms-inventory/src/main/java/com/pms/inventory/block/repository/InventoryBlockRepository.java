package com.pms.inventory.block.repository;

import com.pms.inventory.block.entity.InventoryBlock;
import com.pms.inventory.block.enums.InventoryBlockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryBlockRepository extends JpaRepository<InventoryBlock, Long> {

    Optional<InventoryBlock> findByIdAndStatus(Long id, InventoryBlockStatus status);
}

