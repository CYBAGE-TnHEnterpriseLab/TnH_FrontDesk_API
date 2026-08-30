package com.pms.property.draft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "property_draft")
public class PropertyDraftEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wizard_data", nullable = false)
    private String wizardData;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false)
    private DraftLifecycleState lifecycleState;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(name = "completed_steps", nullable = false)
    private String completedSteps;

    @Column(name = "published_property_id")
    private String publishedPropertyId;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Version
    private Long version;
}



