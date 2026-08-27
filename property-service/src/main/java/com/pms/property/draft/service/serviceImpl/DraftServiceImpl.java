package com.pms.property.draft.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.property.entity.PropertyEntity;
import com.pms.property.domain.property.repository.PropertyRepository;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.dto.WizardPropertyOptionResponse;
import com.pms.property.draft.entity.DraftLifecycleState;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.mapper.DraftMapper;
import com.pms.property.draft.repository.PropertyDraftRepository;
import com.pms.property.draft.service.DraftService;
import com.pms.property.upload.service.LocalImageStorageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DraftServiceImpl implements DraftService {

    private final PropertyDraftRepository draftRepository;
    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;
    private final LocalImageStorageService localImageStorageService;

    public DraftServiceImpl(
        PropertyDraftRepository draftRepository,
        PropertyRepository propertyRepository,
        ObjectMapper objectMapper,
        LocalImageStorageService localImageStorageService
    ) {
        this.draftRepository = draftRepository;
        this.propertyRepository = propertyRepository;
        this.objectMapper = objectMapper;
        this.localImageStorageService = localImageStorageService;
    }

    @Override
    @Transactional
    public DraftResponse createDraft(CreateDraftRequest request, String actor) {
        PropertyDraftEntity entity = new PropertyDraftEntity();
        entity.setSchemaVersion(request.schemaVersion());
        entity.setStatus(DraftStatus.DRAFT);
        entity.setLifecycleState(DraftLifecycleState.DRAFT);
        entity.setCurrentStep(defaultStep(request.currentStep()));
        entity.setCompletedSteps(joinSteps(request.completedSteps()));
        entity.setWizardData(writeJson(request.wizardData()));
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        PropertyDraftEntity saved = draftRepository.save(entity);
        return DraftMapper.toResponse(saved, objectMapper);
    }

    @Override
    @Transactional
    public DraftResponse saveDraft(Long draftId, SaveDraftRequest request, String actor) {
        PropertyDraftEntity entity = getById(draftId);
        if (request.expectedVersion() != null && !request.expectedVersion().equals(entity.getVersion())) {
            throw new BadRequestException("Draft version mismatch");
        }

        String existingWizardData = entity.getWizardData();
        String updatedWizardData = writeJson(request.wizardData());
        deleteRemovedDraftImages(existingWizardData, updatedWizardData);

        entity.setSchemaVersion(request.schemaVersion());
        entity.setCurrentStep(defaultStep(request.currentStep()));
        entity.setCompletedSteps(joinSteps(request.completedSteps()));
        if (entity.getLifecycleState() == DraftLifecycleState.DRAFT) {
            entity.setLifecycleState(DraftLifecycleState.CONFIGURED);
        }
        entity.setWizardData(updatedWizardData);
        entity.setUpdatedBy(actor);
        entity.setUpdatedAt(Instant.now());
        return DraftMapper.toResponse(draftRepository.save(entity), objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public DraftResponse getDraft(Long draftId) {
        return DraftMapper.toResponse(getById(draftId), objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public DraftResponse getPublishedDraftByPropertyId(String propertyId, String actor) {
        PropertyEntity property = getOwnedProperty(propertyId, actor);
        PropertyDraftEntity draft = draftRepository
            .findFirstByPublishedPropertyIdAndStatusOrderByUpdatedAtDesc(property.getId(), DraftStatus.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Published draft not found for property: " + propertyId));
        return DraftMapper.toResponse(draft, objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WizardPropertyOptionResponse> getMyPublishedWizardProperties(String actor) {
        List<PropertyEntity> properties = propertyRepository.findByCreatedBy(actor);
        if (properties.isEmpty()) {
            return List.of();
        }

        Set<String> propertyIds = new HashSet<>();
        for (PropertyEntity property : properties) {
            propertyIds.add(property.getId());
        }

        List<PropertyDraftEntity> publishedDrafts = draftRepository.findByPublishedPropertyIdInAndStatus(
            propertyIds,
            DraftStatus.PUBLISHED
        );
        Map<String, Long> latestDraftByProperty = new HashMap<>();
        Map<String, Instant> latestDraftUpdatedAt = new HashMap<>();
        for (PropertyDraftEntity draft : publishedDrafts) {
            String publishedPropertyId = draft.getPublishedPropertyId();
            if (publishedPropertyId == null || publishedPropertyId.isBlank()) {
                continue;
            }
            Instant existing = latestDraftUpdatedAt.get(publishedPropertyId);
            if (existing == null || draft.getUpdatedAt().isAfter(existing)) {
                latestDraftUpdatedAt.put(publishedPropertyId, draft.getUpdatedAt());
                latestDraftByProperty.put(publishedPropertyId, draft.getId());
            }
        }

        List<WizardPropertyOptionResponse> response = new ArrayList<>();
        for (PropertyEntity property : properties) {
            response.add(
                new WizardPropertyOptionResponse(
                    property.getId(),
                    property.getTitle(),
                    latestDraftByProperty.get(property.getId())
                )
            );
        }
        response.sort(Comparator.comparing(WizardPropertyOptionResponse::propertyName, String.CASE_INSENSITIVE_ORDER));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftResponse> getDraftsByStatus(Collection<DraftStatus> requestedStatuses, String actor) {
        EnumSet<DraftStatus> statuses = EnumSet.of(DraftStatus.DRAFT, DraftStatus.PUBLISHED);
        if (requestedStatuses != null && !requestedStatuses.isEmpty()) {
            statuses = EnumSet.copyOf(requestedStatuses);
        }

        List<PropertyDraftEntity> drafts = draftRepository.findByCreatedByAndStatusInOrderByUpdatedAtDesc(actor, statuses);
        List<DraftResponse> responses = new ArrayList<>();
        for (PropertyDraftEntity draft : drafts) {
            responses.add(DraftMapper.toResponse(draft, objectMapper));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyDraftEntity getById(Long draftId) {
        return draftRepository.findById(draftId)
            .orElseThrow(() -> new NotFoundException("Draft not found: " + draftId));
    }

    @Override
    @Transactional
    public void deleteDraft(Long draftId, String actor) {
        PropertyDraftEntity draft = getById(draftId);
        if (draft.getStatus() == DraftStatus.PUBLISHED) {
            throw new BadRequestException("Published draft cannot be deleted");
        }
        if (draft.getCreatedBy() != null && !draft.getCreatedBy().equals(actor)) {
            throw new BadRequestException("Draft does not belong to the current user");
        }
        deleteDraftImages(draft.getWizardData());
        draftRepository.delete(draft);
    }

    @Override
    public void deleteImagesFromWizardData(String wizardDataJson) {
        deleteDraftImages(wizardDataJson);
    }

    @Override
    @Transactional
    public void markPublished(PropertyDraftEntity draft, String propertyId, String actor) {
        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setLifecycleState(DraftLifecycleState.ACTIVE);
        draft.setPublishedPropertyId(propertyId);
        draft.setPublishedBy(actor);
        draft.setUpdatedBy(actor);
        draft.setUpdatedAt(Instant.now());
        draftRepository.save(draft);
    }

    private PropertyEntity getOwnedProperty(String propertyId, String actor) {
        if (propertyId == null || propertyId.isBlank()) {
            throw new BadRequestException("propertyId is required");
        }
        PropertyEntity property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
        if (!actor.equals(property.getCreatedBy())) {
            throw new BadRequestException("Selected property does not belong to the current user");
        }
        return property;
    }

    private String defaultStep(String currentStep) {
        return (currentStep == null || currentStep.isBlank()) ? "PROPERTY_DETAILS" : currentStep.trim();
    }

    private String joinSteps(List<String> completedSteps) {
        if (completedSteps == null || completedSteps.isEmpty()) {
            return "";
        }
        return completedSteps.stream()
            .map(String::trim)
            .filter(step -> !step.isBlank())
            .distinct()
            .reduce((left, right) -> left + "," + right)
            .orElse("");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid draft JSON payload");
        }
    }

    private void deleteDraftImages(String wizardDataJson) {
        for (String imageUrl : extractDraftImageUrls(wizardDataJson)) {
            localImageStorageService.deleteByPublicUrl(imageUrl);
        }
    }

    private void deleteRemovedDraftImages(String existingWizardData, String updatedWizardData) {
        Set<String> existingImages = extractDraftImageUrls(existingWizardData);
        Set<String> updatedImages = extractDraftImageUrls(updatedWizardData);
        for (String existingImage : existingImages) {
            if (!updatedImages.contains(existingImage)) {
                localImageStorageService.deleteByPublicUrl(existingImage);
            }
        }
    }

    private Set<String> extractDraftImageUrls(String wizardDataJson) {
        Set<String> imageUrls = new HashSet<>();
        if (wizardDataJson == null || wizardDataJson.isBlank()) {
            return imageUrls;
        }
        try {
            JsonNode root = objectMapper.readTree(wizardDataJson);
            collectImagesRecursively(root, imageUrls);
        } catch (JsonProcessingException ignored) {
            // Legacy malformed data should still be deletable.
        }
        return imageUrls;
    }

    private void collectImagesRecursively(JsonNode node, Set<String> imageUrls) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            if (value.startsWith("/uploads/")) {
                imageUrls.add(value);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectImagesRecursively(child, imageUrls);
            }
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> collectImagesRecursively(entry.getValue(), imageUrls));
        }
    }
}


