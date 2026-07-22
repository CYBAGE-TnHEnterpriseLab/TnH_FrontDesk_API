package com.pms.property.domain.content.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.domain.content.dto.ContentOverviewResponse;
import com.pms.property.domain.content.dto.ContentSummaryResponse;
import com.pms.property.domain.content.service.ContentService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentControllerTest {

    @Test
    void shouldReturnSummaryPayload() {
        ContentService service = mock(ContentService.class);
        ContentController controller = new ContentController(service);
        when(service.getSummaryByPropertyId("P-1")).thenReturn(new ContentSummaryResponse("P-1", "desc", "hero", 2, 1));

        var response = controller.getSummary("P-1").getBody();

        assertEquals(true, response.success());
        assertEquals("P-1", response.data().propertyId());
    }

    @Test
    void shouldReturnOverviewListPayload() {
        ContentService service = mock(ContentService.class);
        ContentController controller = new ContentController(service);
        when(service.listOverviewsByPropertyId("P-1")).thenReturn(List.of(new ContentOverviewResponse(1L, "P-1", "hero", "desc")));

        var response = controller.list("P-1").getBody();

        assertEquals(1, response.data().size());
    }
}

