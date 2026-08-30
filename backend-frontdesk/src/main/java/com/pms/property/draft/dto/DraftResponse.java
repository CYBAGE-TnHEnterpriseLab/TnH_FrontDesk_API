package com.pms.property.draft.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record DraftResponse(
    Long id,
    Integer schemaVersion,
    String status,
    String lifecycleState,
    String currentStep,
    List<String> completedSteps,
    JsonNode wizardData,
    Long version,
    String publishedPropertyId,
    String createdBy,
    String updatedBy,
    String publishedBy
) {
}



