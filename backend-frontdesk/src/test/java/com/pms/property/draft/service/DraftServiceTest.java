package com.pms.property.draft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.draft.dto.CreateDraftRequest;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.repository.PropertyDraftRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DraftServiceTest {

    @Test
    void shouldGenerateRoomNumbersInDraftJsonWhenMissing() throws Exception {
        PropertyDraftRepository repository = Mockito.mock(PropertyDraftRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DraftService draftService = new DraftService(repository, objectMapper);

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

        draftService.createDraft(new CreateDraftRequest(1, wizardData, null, null));

        PropertyDraftEntity saved = Mockito.mockingDetails(repository)
            .getInvocations()
            .stream()
            .filter(invocation -> invocation.getMethod().getName().equals("save"))
            .map(invocation -> (PropertyDraftEntity) invocation.getArgument(0))
            .findFirst()
            .orElseThrow();

        JsonNode persisted = objectMapper.readTree(saved.getWizardData());
        JsonNode roomNumbers = persisted.path("roomConfiguration").path("floors").get(0).path("roomNumbers");

        assertTrue(roomNumbers.isArray());
        assertEquals(3, roomNumbers.size());
        assertEquals("101", roomNumbers.get(0).asText());
        assertEquals("102", roomNumbers.get(1).asText());
        assertEquals("103", roomNumbers.get(2).asText());
    }
}

