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
import com.pms.common.utils.CurrentUser;
import com.pms.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    /** Creates a new draft property for the current user. */
    @PostMapping("/createDraft")
    public ResponseEntity<ApiResponse<DraftResponse>> createPropertyDraft(@Valid @RequestBody CreateDraftRequest request) {
        UUID actor = CurrentUser.userId();
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.create(request, actor), "Draft property created"));
    }

    /** Saves updates to an existing draft property owned by the current user. */
    @PutMapping("/saveDraft/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> save(
        @PathVariable Long draftId,
        @Valid @RequestBody SaveDraftRequest request
    ) {
        UUID actor = CurrentUser.userId();
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.update(draftId, request, actor), "Draft property saved"));
    }

    /** Fetches draft property records, or both draft and published property records when status is not provided. */
    @GetMapping("/getAllProperties")
    public ResponseEntity<ApiResponse<List<DraftResponse>>> list(
        @RequestParam(name = "status", required = false) List<String> statusFilters
    ) {
        UUID actor = CurrentUser.userId();
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
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.list(statuses, actor), "Draft and published properties fetched"));
    }

    /** Fetches a single draft property by draft id. */
    @GetMapping("/getDraft/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> get(@PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.get(draftId), "Draft property fetched"));
    }

    /** Fetches published property options that can be used in the wizard flow. */
    @GetMapping("/wizard/properties")
    public ResponseEntity<ApiResponse<List<WizardPropertyOptionResponse>>> listMyWizardProperties() {
        UUID actor = CurrentUser.userId();
        return ResponseEntity.ok(
            ApiResponse.ok(draftFacade.listMyWizardProperties(actor), "Published properties for wizard fetched")
        );
    }

    /** Fetches the latest draft property version derived from a selected published property. */
    @GetMapping("/wizard/properties/{propertyId}")
    public ResponseEntity<ApiResponse<DraftResponse>> getWizardDraftByProperty(@PathVariable String propertyId) {
        UUID actor = CurrentUser.userId();
        return ResponseEntity.ok(
            ApiResponse.ok(draftFacade.getPublishedDraftByProperty(propertyId, actor), "Draft property from published property fetched")
        );
    }

    /** Publishes a draft property and returns the published property payload. */
    @PostMapping("/{draftId}/publish")
    public ResponseEntity<ApiResponse<PublishResponse>> publish(@PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.ok(publishFacade.publish(draftId), "Draft property published"));
    }

    /** Deletes a draft property that is still in editable state. */
    @DeleteMapping("/deleteDraft/{draftId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long draftId) {
        UUID actor = CurrentUser.userId();
        draftFacade.delete(draftId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null, "Draft property deleted"));
    }
}