package com.pms.property.draft.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.entity.PropertyDraftEntity;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DraftMapper {

    private DraftMapper() {
    }

    public static DraftResponse toResponse(PropertyDraftEntity entity, ObjectMapper objectMapper) {
        JsonNode data = objectMapper.valueToTree(entity.getWizardData());
        try {
            data = objectMapper.readTree(entity.getWizardData());
        } catch (Exception ignored) {
            // Returns raw text node when malformed legacy data exists.
        }
        return new DraftResponse(
            entity.getId(),
            entity.getSchemaVersion(),
            entity.getStatus().name(),
            entity.getLifecycleState().name(),
            entity.getCurrentStep(),
            splitSteps(entity.getCompletedSteps()),
            data,
            entity.getVersion(),
            entity.getPublishedPropertyId()
        );
    }

    private static List<String> splitSteps(String completedSteps) {
        if (completedSteps == null || completedSteps.isBlank()) {
            return List.of();
        }
        return Arrays.stream(completedSteps.split(","))
            .map(String::trim)
            .filter(step -> !step.isBlank())
            .collect(Collectors.toList());
    }
}


