package com.pms.property.draft.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateDraftRequest(
    @NotNull Integer schemaVersion,
    @NotNull JsonNode wizardData,
    String currentStep,
    List<String> completedSteps
) {
}


