package com.pms.property.draft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.dto.SaveDraftRequest;
import com.pms.property.draft.entity.DraftLifecycleState;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.repository.PropertyDraftRepository;
import com.pms.property.domain.property.repository.PropertyRepository;
import com.pms.property.upload.service.LocalImageStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DraftServiceTest {

    @Test
    void shouldNotGenerateRoomNumbersWhenMissing() throws Exception {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);
        LocalImageStorageService imageStorageService = Mockito.mock(LocalImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, propertyRepository, objectMapper, imageStorageService);

        when(repository.save(any(PropertyDraftEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JsonNode wizardData = objectMapper.readTree("""
            {
              "roomConfiguration": {
                "floors": [
                  {"floorName": "1", "roomTypeName": "Deluxe", "roomCount": 3, "startNumber": 101}
                ]
              }
            }
            """);

        draftService.createDraft(new CreateDraftRequest(1, wizardData, null, null), "admin");

        PropertyDraftEntity saved = Mockito.mockingDetails(repository)
            .getInvocations()
            .stream()
            .filter(invocation -> invocation.getMethod().getName().equals("save"))
            .map(invocation -> (PropertyDraftEntity) invocation.getArgument(0))
            .findFirst()
            .orElseThrow();

        JsonNode persisted = objectMapper.readTree(saved.getWizardData());
        JsonNode roomNumbers = persisted.path("roomConfiguration").path("floors").get(0).path("roomNumbers");
        assertTrue(roomNumbers.isMissingNode());
    }

    @Test
    void shouldListDraftsByStatus() {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);
        LocalImageStorageService imageStorageService = Mockito.mock(LocalImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, propertyRepository, objectMapper, imageStorageService);

        PropertyDraftEntity draft = new PropertyDraftEntity();
        draft.setSchemaVersion(1);
        draft.setStatus(DraftStatus.DRAFT);
        draft.setLifecycleState(DraftLifecycleState.DRAFT);
        draft.setCurrentStep("PROPERTY_DETAILS");
        draft.setCompletedSteps("");
        draft.setWizardData("{}");
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(Instant.now());

        when(repository.findByStatusInOrderByUpdatedAtDesc(any()))
            .thenReturn(List.of(draft));

        assertEquals(1, draftService.getDraftsByStatus(List.of(DraftStatus.DRAFT)).size());
    }

    @Test
    void shouldDeleteDraftWhenOwnedAndNotPublished() {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);
        LocalImageStorageService imageStorageService = Mockito.mock(LocalImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, propertyRepository, objectMapper, imageStorageService);

        PropertyDraftEntity draft = new PropertyDraftEntity();
        draft.setStatus(DraftStatus.DRAFT);
        draft.setCreatedBy("admin");
        draft.setWizardData("{\"wizardData\":{\"content\":{\"propertyOverview\":{\"propertyHeroImage\":\"/uploads/hero.png\"}}}}");

        when(repository.findById(10L)).thenReturn(Optional.of(draft));

        draftService.deleteDraft(10L, "admin");

        verify(repository).delete(draft);
        verify(imageStorageService).deleteByPublicUrl("/uploads/hero.png");
    }

    @Test
    void shouldRejectDeleteForPublishedDraft() {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);
        LocalImageStorageService imageStorageService = Mockito.mock(LocalImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, propertyRepository, objectMapper, imageStorageService);

        PropertyDraftEntity draft = new PropertyDraftEntity();
        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setCreatedBy("admin");

        when(repository.findById(11L)).thenReturn(Optional.of(draft));

        assertThrows(BadRequestException.class, () -> draftService.deleteDraft(11L, "admin"));
    }

    @Test
    void shouldAllowSaveForPublishedDraft() throws Exception {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);
        LocalImageStorageService imageStorageService = Mockito.mock(LocalImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, propertyRepository, objectMapper, imageStorageService);

        PropertyDraftEntity draft = new PropertyDraftEntity();
        draft.setId(12L);
        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setLifecycleState(DraftLifecycleState.ACTIVE);
        draft.setVersion(5L);
        draft.setCurrentStep("PROPERTY_DETAILS");
        draft.setCompletedSteps("PROPERTY_DETAILS");
        draft.setSchemaVersion(1);
        draft.setWizardData("{}");
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(Instant.now());

        JsonNode updatedWizard = objectMapper.readTree("{\"propertyDetails\":{\"propertyName\":\"Updated\"}}");
        SaveDraftRequest request = new SaveDraftRequest(2, updatedWizard, 5L, "CONTENT", List.of("PROPERTY_DETAILS", "CONTENT"));

        when(repository.findById(12L)).thenReturn(Optional.of(draft));
        when(repository.save(any(PropertyDraftEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = draftService.saveDraft(12L, request, "admin");

        assertEquals(DraftStatus.PUBLISHED.name(), response.status());
        assertEquals(2, response.schemaVersion());
        verify(repository).save(draft);
    }

    @Test
    void shouldDeleteRemovedImagesOnDraftSave() throws Exception {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        PropertyRepository propertyRepository = Mockito.mock(PropertyRepository.class);
        LocalImageStorageService imageStorageService = Mockito.mock(LocalImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, propertyRepository, objectMapper, imageStorageService);

        PropertyDraftEntity draft = new PropertyDraftEntity();
        draft.setId(13L);
        draft.setStatus(DraftStatus.DRAFT);
        draft.setLifecycleState(DraftLifecycleState.CONFIGURED);
        draft.setVersion(2L);
        draft.setCurrentStep("CONTENT");
        draft.setCompletedSteps("PROPERTY_DETAILS,CONTENT");
        draft.setSchemaVersion(1);
        draft.setWizardData("{\"content\":{\"propertyOverview\":{\"propertyHeroImage\":\"/uploads/old-hero.png\"},\"gallery\":[\"/uploads/keep.png\",\"/uploads/remove.png\"]}}");
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(Instant.now());

        JsonNode updatedWizard = objectMapper.readTree("{\"content\":{\"propertyOverview\":{\"propertyHeroImage\":\"/uploads/new-hero.png\"},\"gallery\":[\"/uploads/keep.png\"]}}");
        SaveDraftRequest request = new SaveDraftRequest(1, updatedWizard, 2L, "CONTENT", List.of("PROPERTY_DETAILS", "CONTENT"));

        when(repository.findById(13L)).thenReturn(Optional.of(draft));
        when(repository.save(any(PropertyDraftEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        draftService.saveDraft(13L, request, "admin");

        verify(imageStorageService).deleteByPublicUrl("/uploads/old-hero.png");
        verify(imageStorageService).deleteByPublicUrl("/uploads/remove.png");
        verify(imageStorageService, never()).deleteByPublicUrl("/uploads/keep.png");
        verify(imageStorageService, never()).deleteByPublicUrl("/uploads/new-hero.png");
    }
}

