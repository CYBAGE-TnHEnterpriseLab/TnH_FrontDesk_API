package com.pms.guest.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.entity.GuestProfile;
import com.pms.guest.mapper.GuestProfileMapper;
import com.pms.guest.repository.GuestProfileRepository;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestProfileServiceImplTest {

    private static final Pattern GENERATED_GUEST_ID =
            Pattern.compile("^GST-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Mock
    private GuestProfileRepository guestProfileRepository;

    private GuestProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuestProfileServiceImpl(guestProfileRepository, new GuestProfileMapper());
    }

    @Test
    void createMapsRequestGeneratesUniqueBusinessIdAndPersistsProfile() {
        GuestProfileCreateRequest request = new GuestProfileCreateRequest();
        request.setPropertyId("PROP001");
        request.setSalutation("Mr");
        request.setFirstName("Manish");
        request.setLastName("Das");
        request.setPersonalEmail("manish.das@example.com");
        request.setPhoneNumber("+1-555-0101");
        request.setAddress("42 Business Park");
        request.setCity("New York");
        request.setState("NY");
        request.setCountry("USA");
        request.setPostalCode("10001");
        request.setNationality("American");
        request.setVipStatus(false);
        request.setIdDocumentPath("/uploads/guests/P1234567.pdf");

        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> {
            GuestProfile profile = invocation.getArgument(0);
            profile.setId(27L);
            return profile;
        });

        var response = service.createGuestProfile(request);

        ArgumentCaptor<GuestProfile> savedProfile = ArgumentCaptor.forClass(GuestProfile.class);
        verify(guestProfileRepository).save(savedProfile.capture());
        GuestProfile profile = savedProfile.getValue();
        assertThat(profile.getId()).isEqualTo(27L);
        assertThat(profile.getGuestId()).matches(GENERATED_GUEST_ID);
        assertThat(profile.getPropertyId()).isEqualTo("PROP001");
        assertThat(profile.getFirstName()).isEqualTo("Manish");
        assertThat(profile.getLastName()).isEqualTo("Das");
        assertThat(profile.getVipStatus()).isFalse();
        assertThat(profile.getIdDocumentPath()).isEqualTo("/uploads/guests/P1234567.pdf");
        assertThat(profile.getLoyaltyMembershipNumber()).isNull();
        assertThat(profile.getLoyaltyTier()).isNull();
        assertThat(response.getId()).isEqualTo(27L);
        assertThat(response.getGuestId()).isEqualTo(profile.getGuestId());
        assertThat(response.getPropertyId()).isEqualTo("PROP001");
        assertThat(response.getFirstName()).isEqualTo("Manish");
        assertThat(response.getLastName()).isEqualTo("Das");
    }

    @Test
    void createRetriesWhenGeneratedBusinessIdAlreadyExists() {
        GuestProfileCreateRequest request = new GuestProfileCreateRequest();
        request.setPropertyId("PROP001");
        request.setFirstName("Manish");
        request.setLastName("Das");

        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(true, false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createGuestProfile(request);

        verify(guestProfileRepository, times(2)).existsByGuestId(anyString());
        verify(guestProfileRepository).save(any(GuestProfile.class));
    }
}
