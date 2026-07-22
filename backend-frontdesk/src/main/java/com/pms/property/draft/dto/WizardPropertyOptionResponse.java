package com.pms.property.draft.dto;

public record WizardPropertyOptionResponse(
    String propertyId,
    String propertyName,
    Long latestPublishedDraftId
) {
}

