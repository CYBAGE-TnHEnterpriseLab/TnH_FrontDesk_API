package com.pms.property.draft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "property_draft")
public class PropertyDraftEntity {

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
    private Long publishedPropertyId;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public String getWizardData() {
        return wizardData;
    }

    public void setWizardData(String wizardData) {
        this.wizardData = wizardData;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
    }

    public Long getPublishedPropertyId() {
        return publishedPropertyId;
    }

    public void setPublishedPropertyId(Long publishedPropertyId) {
        this.publishedPropertyId = publishedPropertyId;
    }

    public DraftLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(DraftLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(String completedSteps) {
        this.completedSteps = completedSteps;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}


