package com.pms.property.draft.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.facade.DraftFacade;
import com.pms.property.publish.dto.PublishResponse;
import com.pms.property.publish.facade.PublishFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property/drafts")
public class DraftController {

    private final DraftFacade draftFacade;
    private final PublishFacade publishFacade;

    public DraftController(DraftFacade draftFacade, PublishFacade publishFacade) {
        this.draftFacade = draftFacade;
        this.publishFacade = publishFacade;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DraftResponse>> create(@Valid @RequestBody CreateDraftRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.create(request), "Draft created"));
    }

    @PutMapping("/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> save(
        @PathVariable Long draftId,
        @Valid @RequestBody SaveDraftRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.update(draftId, request), "Draft saved"));
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> get(@PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.ok(draftFacade.get(draftId), "Draft fetched"));
    }

    @PostMapping("/{draftId}/publish")
    public ResponseEntity<ApiResponse<PublishResponse>> publish(@PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.ok(publishFacade.publish(draftId), "Draft published"));
    }
}

