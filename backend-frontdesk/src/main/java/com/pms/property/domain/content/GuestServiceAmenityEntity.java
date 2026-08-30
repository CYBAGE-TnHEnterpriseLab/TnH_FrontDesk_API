package com.pms.property.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guest_service_amenity")
public class GuestServiceAmenityEntity {

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

	public void setPropertyId(String propertyId) {
		this.propertyId = propertyId;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}


