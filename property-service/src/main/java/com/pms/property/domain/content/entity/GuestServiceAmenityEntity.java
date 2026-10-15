package com.pms.property.domain.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.pms.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "guest_service_amenity")
public class GuestServiceAmenityEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "property_id", nullable = false)
	private String propertyId;

	@Column(nullable = false, length = 40)
	private String section;

	@Column(nullable = false, length = 120)
	private String code;

	@Column(nullable = false)
	private Boolean enabled;

}



