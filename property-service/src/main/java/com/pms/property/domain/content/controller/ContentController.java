package com.pms.property.domain.content.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.content.dto.ContentOverviewRequest;
import com.pms.property.domain.content.dto.ContentOverviewResponse;
import com.pms.property.domain.content.dto.ContentSummaryResponse;
import com.pms.property.domain.content.service.ContentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /** Fetches the content setup summary for a published property. */
    @GetMapping("/properties/{propertyId}/summary")
    public ResponseEntity<ApiResponse<ContentSummaryResponse>> getSummary(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(contentService.getSummaryByPropertyId(propertyId), "Published property content summary fetched"));
    }

    /** Fetches content overviews configured for a published property. */
    @GetMapping("/properties/{propertyId}/overviews")
    public ResponseEntity<ApiResponse<List<ContentOverviewResponse>>> list(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(contentService.listOverviewsByPropertyId(propertyId), "Published property content overviews fetched"));
    }

    /** Fetches a content overview by id for a published property. */
    @GetMapping("/properties/{propertyId}/overviews/{overviewId}")
    public ResponseEntity<ApiResponse<ContentOverviewResponse>> getById(
        @PathVariable String propertyId,
        @PathVariable Long overviewId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(contentService.getOverviewById(propertyId, overviewId), "Published property content overview fetched"));
    }

    /** Creates a content overview for a published property. */
    @PostMapping("/properties/{propertyId}/overviews")
    public ResponseEntity<ApiResponse<ContentOverviewResponse>> create(
        @PathVariable String propertyId,
        @RequestBody ContentOverviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(contentService.createOverview(propertyId, request), "Published property content overview created"));
    }

    /** Updates a content overview for a published property. */
    @PutMapping("/properties/{propertyId}/overviews/{overviewId}")
    public ResponseEntity<ApiResponse<ContentOverviewResponse>> update(
        @PathVariable String propertyId,
        @PathVariable Long overviewId,
        @RequestBody ContentOverviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(contentService.updateOverview(propertyId, overviewId, request), "Published property content overview updated"));
    }

    /** Deletes a content overview from a published property. */
    @DeleteMapping("/properties/{propertyId}/overviews/{overviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable String propertyId,
        @PathVariable Long overviewId
    ) {
        contentService.deleteOverview(propertyId, overviewId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Published property content overview deleted"));
    }
}

