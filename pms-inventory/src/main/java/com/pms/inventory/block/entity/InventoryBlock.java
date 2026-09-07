package com.pms.inventory.block.entity;

import com.pms.common.entity.BaseEntity;
import com.pms.inventory.block.enums.InventoryBlockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
@Entity
@Table(name = "inventory_block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InventoryBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "room_type_id", nullable = false)
    private String roomTypeId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InventoryBlockStatus status;
}

