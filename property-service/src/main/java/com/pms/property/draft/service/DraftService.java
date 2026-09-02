package com.pms.property.draft.service;

import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.dto.WizardPropertyOptionResponse;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DraftService {

    DraftResponse createDraft(CreateDraftRequest request, UUID actor);

    DraftResponse saveDraft(Long draftId, SaveDraftRequest request, UUID actor);

    DraftResponse getDraft(Long draftId);

    DraftResponse getPublishedDraftByPropertyId(String propertyId, UUID actor);

    List<WizardPropertyOptionResponse> getMyPublishedWizardProperties(UUID actor);

    List<DraftResponse> getDraftsByStatus(Collection<DraftStatus> requestedStatuses, UUID actor);

    PropertyDraftEntity getById(Long draftId);

    void deleteDraft(Long draftId, UUID actor);

    void deleteImagesFromWizardData(String wizardDataJson);

    void markPublished(PropertyDraftEntity draft, String propertyId, UUID actor);
}
