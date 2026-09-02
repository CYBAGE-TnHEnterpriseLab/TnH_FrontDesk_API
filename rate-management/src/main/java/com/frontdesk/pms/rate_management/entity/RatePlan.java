package com.frontdesk.pms.rate_management.entity;

import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.enums.RatePlanType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@Entity
@Table(
        name = "rate_plan",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_rate_plan_property_name", columnNames = {"property_id", "name"}),
            @UniqueConstraint(name = "uk_rate_plan_property_code", columnNames = {"property_id", "code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RatePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String occupancyType;

    @Enumerated(EnumType.STRING)
    @Column
    private MasterRoomMealOption mealOption;

    @Column
    private String inclusion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatePlanType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatePlanCalculationMethod calculationMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatePlanStatus status = RatePlanStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column
    private Double adjustmentValue;

    @Column
    private Double manualAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rate_plan_manual_occupancy_price", joinColumns = @JoinColumn(name = "rate_plan_id"))
    @MapKeyColumn(name = "occupancy_type")
    @Column(name = "manual_amount")
    private Map<String, Double> manualPricingByOccupancy = new HashMap<>();

        @Column(name = "parent_rate_plan_id")
        private Long parentRatePlanId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rate_plan_room_type", joinColumns = @JoinColumn(name = "rate_plan_id"))
    @Column(name = "room_type_id", nullable = false)
    private Set<Long> applicableRoomTypeIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rate_plan_policy", joinColumns = @JoinColumn(name = "rate_plan_id"))
    @OrderColumn(name = "policy_index")
    @Column(name = "policy_id")
    private List<String> policyId = new ArrayList<>(); // New field for policy IDs
}
