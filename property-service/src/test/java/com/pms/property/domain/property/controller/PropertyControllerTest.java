package com.pms.property.domain.property.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.property.domain.property.service.PropertyService;
import com.pms.security.jwt.CurrentUserProvider;
import org.junit.jupiter.api.Test;

class PropertyControllerTest {

    @Test
    void shouldDeletePropertyForCurrentActor() {
        PropertyService propertyService = mock(PropertyService.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        PropertyController controller = new PropertyController(propertyService, currentUserProvider);

        when(currentUserProvider.getCurrentUsername()).thenReturn("owner");

        var response = controller.deleteById("P-300").getBody();

        verify(propertyService).deleteOwnedProperty("P-300", "owner");
        assertEquals(true, response.success());
        assertEquals("Published property deleted", response.message());
    }
}

