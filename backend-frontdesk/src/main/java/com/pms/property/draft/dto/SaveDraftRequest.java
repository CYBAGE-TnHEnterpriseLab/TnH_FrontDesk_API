package com.pms.property.draft.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveDraftRequest(
    @NotNull Integer schemaVersion,
    @NotNull JsonNode wizardData,
    Long expectedVersion,
    String currentStep,
    List<String> completedSteps
) {
}


