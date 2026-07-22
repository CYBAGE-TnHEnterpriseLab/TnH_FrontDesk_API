package com.pms.property.domain.property.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.property.dto.PropertyResponse;
import com.pms.property.domain.property.service.PropertyService;
import com.pms.property.security.CurrentUserProvider;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property")
public class PropertyController {

    private final PropertyService propertyService;
    private final CurrentUserProvider currentUserProvider;

    public PropertyController(PropertyService propertyService, CurrentUserProvider currentUserProvider) {
        this.propertyService = propertyService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Fetches a single published property by property id. */
    @GetMapping("/getPublishedProperty/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getById(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getById(propertyId), "Published property fetched"));
    }

    /** Fetches published properties created by the current user. */
    @GetMapping("/getAllPublishedProperties")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> listMine() {
        String actor = currentUserProvider.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.ok(propertyService.listByCreator(actor), "Published properties fetched"));
    }

    /** Deletes a published property owned by the current user. */
    @DeleteMapping("/deletePublishedProperty/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable String propertyId) {
        String actor = currentUserProvider.getCurrentUsername();
        propertyService.deleteOwnedProperty(propertyId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null, "Published property deleted"));
    }
}

