package com.pms.property.draft.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.NotFoundException;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.DraftResponse;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.entity.DraftLifecycleState;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.mapper.DraftMapper;
import com.pms.property.draft.repository.PropertyDraftRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DraftService {

    private final PropertyDraftRepository draftRepository;
    private final ObjectMapper objectMapper;

    public DraftService(PropertyDraftRepository draftRepository, ObjectMapper objectMapper) {
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DraftResponse createDraft(CreateDraftRequest request) {
        PropertyDraftEntity entity = new PropertyDraftEntity();
        entity.setSchemaVersion(request.schemaVersion());
        entity.setStatus(DraftStatus.DRAFT);
        entity.setLifecycleState(DraftLifecycleState.DRAFT);
        entity.setCurrentStep(defaultStep(request.currentStep()));
        entity.setCompletedSteps(joinSteps(request.completedSteps()));
        entity.setWizardData(writeJson(enrichRoomNumbers(request.wizardData())));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        PropertyDraftEntity saved = draftRepository.save(entity);
        return DraftMapper.toResponse(saved, objectMapper);
    }

    @Transactional
    public DraftResponse saveDraft(Long draftId, SaveDraftRequest request) {
        PropertyDraftEntity entity = getById(draftId);
        if (entity.getStatus() == DraftStatus.PUBLISHED) {
            throw new BadRequestException("Published draft cannot be edited");
        }
        if (request.expectedVersion() != null && !request.expectedVersion().equals(entity.getVersion())) {
            throw new BadRequestException("Draft version mismatch");
        }

        entity.setSchemaVersion(request.schemaVersion());
        entity.setCurrentStep(defaultStep(request.currentStep()));
        entity.setCompletedSteps(joinSteps(request.completedSteps()));
        if (entity.getLifecycleState() == DraftLifecycleState.DRAFT) {
            entity.setLifecycleState(DraftLifecycleState.CONFIGURED);
        }
        entity.setWizardData(writeJson(enrichRoomNumbers(request.wizardData())));
        entity.setUpdatedAt(Instant.now());
        return DraftMapper.toResponse(draftRepository.save(entity), objectMapper);
    }

    @Transactional(readOnly = true)
    public DraftResponse getDraft(Long draftId) {
        return DraftMapper.toResponse(getById(draftId), objectMapper);
    }

    @Transactional(readOnly = true)
    public PropertyDraftEntity getById(Long draftId) {
        return draftRepository.findById(draftId)
            .orElseThrow(() -> new NotFoundException("Draft not found: " + draftId));
    }

    @Transactional
    public void markPublished(PropertyDraftEntity draft, Long propertyId) {
        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setLifecycleState(DraftLifecycleState.ACTIVE);
        draft.setPublishedPropertyId(propertyId);
        draft.setUpdatedAt(Instant.now());
        draftRepository.save(draft);
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

    private JsonNode enrichRoomNumbers(JsonNode source) {
        JsonNode copied = source.deepCopy();
        if (!(copied instanceof ObjectNode root)) {
            return copied;
        }

        JsonNode roomConfigNode = root.path("roomConfiguration");
        if (!(roomConfigNode instanceof ObjectNode roomConfig)) {
            return root;
        }

        JsonNode floorsNode = roomConfig.path("floors");
        if (!(floorsNode instanceof ArrayNode floors)) {
            return root;
        }

        for (JsonNode floorNode : floors) {
            if (!(floorNode instanceof ObjectNode floor)) {
                continue;
            }

            JsonNode existingRoomNumbers = floor.path("roomNumbers");
            if (existingRoomNumbers.isArray() && !existingRoomNumbers.isEmpty()) {
                continue;
            }

            int roomCount = floor.path("roomCount").asInt(0);
            int startNumber = floor.path("startNumber").asInt(Integer.MIN_VALUE);
            if (roomCount <= 0 || startNumber == Integer.MIN_VALUE) {
                continue;
            }

            ArrayNode generatedRoomNumbers = objectMapper.createArrayNode();
            for (int i = 0; i < roomCount; i++) {
                generatedRoomNumbers.add(String.valueOf(startNumber + i));
            }
            floor.set("roomNumbers", generatedRoomNumbers);
        }

        return root;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid draft JSON payload");
        }
    }
}


