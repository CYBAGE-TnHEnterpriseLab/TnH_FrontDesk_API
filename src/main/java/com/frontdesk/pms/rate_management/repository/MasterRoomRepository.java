package com.frontdesk.pms.rate_management.repository;

import com.frontdesk.pms.rate_management.entity.MasterRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface MasterRoomRepository extends JpaRepository<MasterRoom, Long> {
    boolean existsByPropertyIdAndName(String propertyId, String name);

    @EntityGraph(attributePaths = {"pricingList"})
    List<MasterRoom> findByPropertyId(String propertyId);

    @Override
    @EntityGraph(attributePaths = {"pricingList"})
    List<MasterRoom> findAll();

    @Override
    @EntityGraph(attributePaths = {"pricingList"})
    java.util.Optional<MasterRoom> findById(Long id);
}