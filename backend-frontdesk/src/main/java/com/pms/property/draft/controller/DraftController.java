package com.pms.property.draft.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.dto.WizardPropertyOptionResponse;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.facade.DraftFacade;
import com.pms.property.publish.dto.PublishResponse;
import com.pms.property.publish.facade.PublishFacade;
import com.pms.property.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property/drafts")
public class DraftController {

    private final DraftFacade draftFacade;
    private final PublishFacade publishFacade;
    private final CurrentUserProvider currentUserProvider;

    public DraftController(
        DraftFacade draftFacade,
        PublishFacade publishFacade,
        CurrentUserProvider currentUserProvider
    ) {
        this.draftFacade = draftFacade;
        this.publishFacade = publishFacade;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/addDraft")
    public ResponseEntity<ApiResponse<DraftResponse>> createPropertyDraft(@Valid @RequestBody CreateDraftRequest request) {
        String actor = currentUserProvider.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.create(request, actor), "Draft created"));
    }

    @PutMapping("/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> save(
        @PathVariable Long draftId,
        @Valid @RequestBody SaveDraftRequest request
    ) {
        String actor = currentUserProvider.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.update(draftId, request, actor), "Draft saved"));
    }

    @GetMapping("/draftList")
    public ResponseEntity<ApiResponse<List<DraftResponse>>> list(
        @RequestParam(name = "status", required = false) List<String> statusFilters
    ) {
        String actor = currentUserProvider.getCurrentUsername();
        List<DraftStatus> statuses = new ArrayList<>();
        if (statusFilters != null) {
            for (String rawStatus : statusFilters) {
                if (rawStatus != null && !rawStatus.isBlank()) {
                    try {
                        statuses.add(DraftStatus.valueOf(rawStatus.trim().toUpperCase()));
                    } catch (IllegalArgumentException ex) {
                        throw new BadRequestException("Invalid status filter: " + rawStatus);
                    }
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.list(statuses, actor), "Drafts fetched"));
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> get(@PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.get(draftId), "Draft fetched"));
    }

    @GetMapping("/wizard/properties")
    public ResponseEntity<ApiResponse<List<WizardPropertyOptionResponse>>> listMyWizardProperties() {
        String actor = currentUserProvider.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.listMyWizardProperties(actor), "Wizard properties fetched"));
    }

    @GetMapping("/wizard/properties/{propertyId}")
    public ResponseEntity<ApiResponse<DraftResponse>> getWizardDraftByProperty(@PathVariable String propertyId) {
        String actor = currentUserProvider.getCurrentUsername();
        return ResponseEntity.ok(
            ApiResponse.ok(draftFacade.getPublishedDraftByProperty(propertyId, actor), "Wizard draft fetched")
        );
    }

    @PostMapping("/{draftId}/publish")
    public ResponseEntity<ApiResponse<PublishResponse>> publish(@PathVariable Long draftId) {
        String actor = currentUserProvider.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.ok(publishFacade.publish(draftId, actor), "Draft published"));
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long draftId) {
        String actor = currentUserProvider.getCurrentUsername();
        draftFacade.delete(draftId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null, "Draft deleted"));
    }
}

