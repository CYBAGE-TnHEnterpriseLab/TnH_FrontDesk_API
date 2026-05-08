package com.frontdesk.pms.account.entity;

import com.frontdesk.common.entity.BaseEntity;
import com.frontdesk.pms.account.enums.ChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "revenue_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_revenue_mappings_property_id_charge_type",
                columnNames = {"property_id", "charge_type"}
        )
)
@Getter
@Setter
public class RevenueMapping extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 50)
    private ChargeType chargeType;

    @Column(name = "chart_of_account_id", nullable = false)
    private UUID chartOfAccountId;

    @Column(nullable = false)
    private boolean active = true;
}
