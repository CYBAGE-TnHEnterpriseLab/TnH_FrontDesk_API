package com.frontdesk.pms.service;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.entity.Property;
import com.frontdesk.pms.exception.BadRequestException;
import com.frontdesk.pms.exception.PropertyNotFoundException;
import com.frontdesk.pms.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceImplTest {

    @Mock
    private PropertyRepository repository;

    @InjectMocks
    private PropertyServiceImpl service;

    @Test
    void createDraftTrimsInputDefaultsDraftAndSaves() {
        UUID propertyId = UUID.randomUUID();
        PropertyRequestDTO request = fullRequest();
        request.setName("  Frontdesk Goa  ");
        request.setEmail("  goa@frontdesk.com  ");

        when(repository.existsByNameIgnoreCase("Frontdesk Goa")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("goa@frontdesk.com")).thenReturn(false);
        when(repository.save(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            property.setId(propertyId);
            return property;
        });

        PropertyResponseDTO response = service.createDraft(request);

        assertThat(response.getId()).isEqualTo(propertyId);
        assertThat(response.getName()).isEqualTo("Frontdesk Goa");
        assertThat(response.getEmail()).isEqualTo("goa@frontdesk.com");
        assertThat(response.getStatus()).isEqualTo(PropertyStatus.DRAFT);

        ArgumentCaptor<Property> savedCaptor = ArgumentCaptor.forClass(Property.class);
        verify(repository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getName()).isEqualTo("Frontdesk Goa");
        assertThat(savedCaptor.getValue().getEmail()).isEqualTo("goa@frontdesk.com");
    }

    @Test
    void createDraftRejectsDuplicateName() {
        PropertyRequestDTO request = fullRequest();
        when(repository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        assertThatThrownBy(() -> service.createDraft(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Property name already exists");

        verify(repository, never()).save(any(Property.class));
    }

    @Test
    void updatePropertyUpdatesExistingProperty() {
        UUID propertyId = UUID.randomUUID();
        Property existing = property(propertyId, "Old Name", "old@frontdesk.com", PropertyStatus.DRAFT);
        PropertyRequestDTO request = new PropertyRequestDTO();
        request.setName("  New Name  ");
        request.setEmail("new@frontdesk.com");

        when(repository.findById(propertyId)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCase("New Name")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("new@frontdesk.com")).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        PropertyResponseDTO response = service.updateProperty(propertyId, request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getEmail()).isEqualTo("new@frontdesk.com");
        verify(repository).save(existing);
    }

    @Test
    void updatePropertyAllowsSameNameAndEmailForCurrentProperty() {
        UUID propertyId = UUID.randomUUID();
        Property existing = property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.DRAFT);
        PropertyRequestDTO request = new PropertyRequestDTO();
        request.setName("frontdesk goa");
        request.setEmail("GOA@frontdesk.com");

        when(repository.findById(propertyId)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCase("frontdesk goa")).thenReturn(true);
        when(repository.existsByEmailIgnoreCase("GOA@frontdesk.com")).thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);

        PropertyResponseDTO response = service.updateProperty(propertyId, request);

        assertThat(response.getName()).isEqualTo("frontdesk goa");
        assertThat(response.getEmail()).isEqualTo("GOA@frontdesk.com");
    }

    @Test
    void findPropertiesByUUIDReturnsProperty() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findById(propertyId))
                .thenReturn(Optional.of(property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.DRAFT)));

        PropertyResponseDTO response = service.findPropertiesByUUID(propertyId);

        assertThat(response.getId()).isEqualTo(propertyId);
        assertThat(response.getName()).isEqualTo("Frontdesk Goa");
    }

    @Test
    void findPropertiesByUUIDThrowsWhenMissing() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findById(propertyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findPropertiesByUUID(propertyId))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void findPropertiesByNameReturnsEmptyForBlankName() {
        List<PropertyResponseDTO> results = service.findPropertiesByName("   ");

        assertThat(results).isEmpty();
        verify(repository, never()).findByNameIgnoreCase(any());
    }

    @Test
    void findPropertiesByNameTrimsAndMapsResults() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findByNameIgnoreCase("Frontdesk Goa"))
                .thenReturn(List.of(property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.DRAFT)));

        List<PropertyResponseDTO> results = service.findPropertiesByName("  Frontdesk Goa  ");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(propertyId);
    }

    @Test
    void searchPropertiesDelegatesToSpecificationRepository() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findAll(anyPropertySpecification()))
                .thenReturn(List.of(property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.PUBLISHED)));

        List<PropertyResponseDTO> results = service.searchProperties(
                "Goa",
                "Asia/Kolkata",
                LocalTime.of(12, 0),
                LocalTime.of(15, 0),
                PropertyStatus.PUBLISHED
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(PropertyStatus.PUBLISHED);
        verify(repository).findAll(anyPropertySpecification());
    }

    @Test
    void deletePropertyDeletesExistingProperty() {
        UUID propertyId = UUID.randomUUID();
        Property property = property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.DRAFT);
        when(repository.findById(propertyId)).thenReturn(Optional.of(property));

        service.deleteProperty(propertyId);

        verify(repository).delete(property);
    }

    @Test
    void publishChangesDraftPropertyToPublished() {
        UUID propertyId = UUID.randomUUID();
        Property property = property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.DRAFT);
        when(repository.findById(propertyId)).thenReturn(Optional.of(property));
        when(repository.save(property)).thenReturn(property);

        PropertyResponseDTO response = service.publish(propertyId);

        assertThat(response.getStatus()).isEqualTo(PropertyStatus.PUBLISHED);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.PUBLISHED);
    }

    @Test
    void publishRejectsNonDraftProperty() {
        UUID propertyId = UUID.randomUUID();
        Property property = property(propertyId, "Frontdesk Goa", "goa@frontdesk.com", PropertyStatus.PUBLISHED);
        when(repository.findById(propertyId)).thenReturn(Optional.of(property));

        assertThatThrownBy(() -> service.publish(propertyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only draft can be published");

        verify(repository, never()).save(any(Property.class));
    }

    private PropertyRequestDTO fullRequest() {
        PropertyRequestDTO request = new PropertyRequestDTO();
        request.setName("Frontdesk Goa");
        request.setEmail("goa@frontdesk.com");
        request.setAddress("Airport Road");
        request.setContactName("Riya D'Souza");
        request.setContactNumber("9876543210");
        request.setTimeZone("Asia/Kolkata");
        request.setCheckInTime(LocalTime.of(14, 0));
        request.setCheckOutTime(LocalTime.of(11, 0));
        request.setNightAuditTime(LocalTime.of(2, 0));
        return request;
    }

    private Property property(UUID id, String name, String email, PropertyStatus status) {
        Property property = Property.builder()
                .name(name)
                .email(email)
                .address("Airport Road")
                .contactName("Riya D'Souza")
                .contactNumber("9876543210")
                .timeZone("Asia/Kolkata")
                .checkInTime(LocalTime.of(14, 0))
                .checkOutTime(LocalTime.of(11, 0))
                .nightAuditTime(LocalTime.of(2, 0))
                .status(status)
                .build();
        property.setId(id);
        return property;
    }

    private Specification<Property> anyPropertySpecification() {
        return any();
    }
}
