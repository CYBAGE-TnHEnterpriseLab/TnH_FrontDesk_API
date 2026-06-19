package com.pms.property.draft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.common.exception.BadRequestException;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.entity.DraftLifecycleState;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.repository.PropertyDraftRepository;
import com.pms.property.domain.property.PropertyRepository;
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
}

