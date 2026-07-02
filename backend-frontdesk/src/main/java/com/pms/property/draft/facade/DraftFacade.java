package com.pms.property.draft.facade;

import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.dto.WizardPropertyOptionResponse;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.service.DraftService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DraftFacade {

    private final DraftService draftService;

    public DraftFacade(DraftService draftService) {
        this.draftService = draftService;
    }

    public DraftResponse create(CreateDraftRequest request, String actor) {
        return draftService.createDraft(request, actor);
    }

    public DraftResponse update(Long draftId, SaveDraftRequest request, String actor) {
        return draftService.saveDraft(draftId, request, actor);
    }

    public DraftResponse get(Long draftId) {
        return draftService.getDraft(draftId);
    }

    public List<DraftResponse> list(List<DraftStatus> statuses) {
        return draftService.getDraftsByStatus(statuses);
    }

    public List<WizardPropertyOptionResponse> listMyWizardProperties(String actor) {
        return draftService.getMyPublishedWizardProperties(actor);
    }

    public DraftResponse getPublishedDraftByProperty(String propertyId, String actor) {
        return draftService.getPublishedDraftByPropertyId(propertyId, actor);
    }

    public void delete(Long draftId, String actor) {
        draftService.deleteDraft(draftId, actor);
    }
}

