package com.pms.property.domain.property.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pms.property.domain.property.service.PropertyService;
import com.pms.common.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

class PropertyControllerTest {

    private static final UUID ACTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void shouldDeletePropertyForCurrentActor() {
        PropertyService propertyService = mock(PropertyService.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        PropertyController controller = new PropertyController(propertyService, currentUserProvider);

        SecurityContextHolder.setContext(
            new SecurityContextImpl(new UsernamePasswordAuthenticationToken(ACTOR.toString(), null, List.of())));

        var response = controller.deleteById("P-300").getBody();

        verify(propertyService).deleteOwnedProperty("P-300", ACTOR);
        assertEquals(true, response.success());
        assertEquals("Published property deleted", response.message());
    }
}
