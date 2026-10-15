package com.pms.property.draft.facade;

import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.dto.WizardPropertyOptionResponse;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.service.DraftService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DraftFacade {

    private final DraftService draftService;

    public DraftFacade(DraftService draftService) {
        this.draftService = draftService;
    }

    public DraftResponse create(CreateDraftRequest request, UUID actor) {
        return draftService.createDraft(request, actor);
    }

    public DraftResponse update(Long draftId, SaveDraftRequest request, UUID actor) {
        return draftService.saveDraft(draftId, request, actor);
    }

    public DraftResponse get(Long draftId) {
        return draftService.getDraft(draftId);
    }

    public List<DraftResponse> list(List<DraftStatus> statuses, UUID actor) {
        return draftService.getDraftsByStatus(statuses, actor);
    }

    public List<WizardPropertyOptionResponse> listMyWizardProperties(UUID actor) {
        return draftService.getMyPublishedWizardProperties(actor);
    }

    public DraftResponse getPublishedDraftByProperty(String propertyId, UUID actor) {
        return draftService.getPublishedDraftByPropertyId(propertyId, actor);
    }

    public void delete(Long draftId, UUID actor) {
        draftService.deleteDraft(draftId, actor);
    }
}

