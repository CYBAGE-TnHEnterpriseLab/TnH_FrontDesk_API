package com.frontdesk.pms.rate_management.repository;

import com.frontdesk.pms.rate_management.entity.RatePlan;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {
    boolean existsByPropertyIdAndCodeIgnoreCase(String propertyId, String code);

    java.util.Optional<RatePlan> findByIdAndPropertyId(Long id, String propertyId);

        List<RatePlan> findByPropertyId(String propertyId);

        List<RatePlan> findByPropertyIdOrderByIdDesc(String propertyId);

    @Query("select distinct rp from RatePlan rp join rp.applicableRoomTypeIds roomTypeId " +
            "where rp.propertyId = :propertyId and roomTypeId = :roomTypeId and rp.mealOption = :mealOption " +
            "and rp.status = :status and :stayDate between rp.startDate and rp.endDate")
    List<RatePlan> findAvailableByRoomTypeMealAndDate(@Param("propertyId") String propertyId,
                                                      @Param("roomTypeId") Long roomTypeId,
                                                      @Param("mealOption") MasterRoomMealOption mealOption,
                                                      @Param("stayDate") LocalDate stayDate,
                                                      @Param("status") RatePlanStatus status);

    @Query("select count(distinct rp) from RatePlan rp join rp.applicableRoomTypeIds roomTypeId " +
            "where rp.propertyId = :propertyId and rp.status = :status and rp.occupancyType = :occupancyType and rp.mealOption = :mealOption " +
            "and roomTypeId in :roomTypeIds and rp.startDate <= :endDate and rp.endDate >= :startDate " +
            "and (:excludeRatePlanId is null or rp.id <> :excludeRatePlanId)")
    long countOverlappingActivePlans(@Param("propertyId") String propertyId,
                                     @Param("roomTypeIds") Set<Long> roomTypeIds,
                                     @Param("occupancyType") String occupancyType,
                                     @Param("mealOption") MasterRoomMealOption mealOption,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("status") RatePlanStatus status,
                                     @Param("excludeRatePlanId") Long excludeRatePlanId);
}
