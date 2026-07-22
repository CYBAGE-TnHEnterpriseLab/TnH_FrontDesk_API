package com.pms.property.integration.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_sync_state")
public class InventorySyncStateEntity {

    @Id
    @Column(name = "property_id", length = 36)
    private String propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InventorySyncStatus status;

    @Column(name = "last_request_id", length = 64)
    private String lastRequestId;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        if (retryCount == null) {
            retryCount = 0;
        }
        updatedAt = Instant.now();
    }
}

