package com.pms.inventory.inventory.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
		name = "room_type_inventory_daily",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_inventory_daily_property_room_type_date",
						columnNames = {"property_id", "room_type_id", "business_date"}
				)
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomTypeInventoryDaily extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "property_id", nullable = false)
	private UUID propertyId;

	@Column(name = "room_type_id", nullable = false)
	private UUID roomTypeId;

	@Column(name = "business_date", nullable = false)
	private LocalDate businessDate;

	@Column(name = "total_inventory", nullable = false)
	private Integer totalInventory;

	@Column(name = "reserved_count", nullable = false)
	private Integer reservedCount;

	@Column(name = "blocked_count", nullable = false)
	private Integer blockedCount;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	public int availableCount() {
		return totalInventory - reservedCount - blockedCount;
	}
}

