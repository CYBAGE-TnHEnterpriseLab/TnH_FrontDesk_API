package com.pms.property.draft.service;

import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.dto.WizardPropertyOptionResponse;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import java.util.Collection;
import java.util.List;

public interface DraftService {

    DraftResponse createDraft(CreateDraftRequest request, String actor);

    DraftResponse saveDraft(Long draftId, SaveDraftRequest request, String actor);

    DraftResponse getDraft(Long draftId);

    DraftResponse getPublishedDraftByPropertyId(String propertyId, String actor);

    List<WizardPropertyOptionResponse> getMyPublishedWizardProperties(String actor);

    List<DraftResponse> getDraftsByStatus(Collection<DraftStatus> requestedStatuses, String actor);

    PropertyDraftEntity getById(Long draftId);

    void deleteDraft(Long draftId, String actor);

    void deleteImagesFromWizardData(String wizardDataJson);

    void markPublished(PropertyDraftEntity draft, String propertyId, String actor);
}
