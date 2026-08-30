package com.pms.property.draft.facade;

import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.service.DraftService;
import org.springframework.stereotype.Component;

@Component
public class DraftFacade {

    private final DraftService draftService;

    public DraftFacade(DraftService draftService) {
        this.draftService = draftService;
    }

    public DraftResponse create(CreateDraftRequest request) {
        return draftService.createDraft(request);
    }

    public DraftResponse update(Long draftId, SaveDraftRequest request) {
        return draftService.saveDraft(draftId, request);
    }

    public DraftResponse get(Long draftId) {
        return draftService.getDraft(draftId);
    }
}

