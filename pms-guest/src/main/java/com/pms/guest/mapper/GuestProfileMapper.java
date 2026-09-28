package com.pms.guest.mapper;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.dto.response.GuestProfileResponse;
import com.pms.guest.entity.GuestProfile;
import org.springframework.stereotype.Component;

@Component
public class GuestProfileMapper {

    public GuestProfile toEntity(GuestProfileCreateRequest request) {
        return GuestProfile.builder()
                .propertyId(request.getPropertyId())
                .salutation(request.getSalutation())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .personalEmail(request.getPersonalEmail())
                .officialEmail(request.getOfficialEmail())
                .phoneNumber(request.getPhoneNumber())
                .mobileNumber(request.getMobileNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .nationality(request.getNationality())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .companyName(request.getCompanyName())
                .vipStatus(request.getVipStatus())
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .idDocumentPath(request.getIdDocumentPath())
                .loyaltyMembershipNumber(request.getLoyaltyMembershipNumber())
                .loyaltyTier(request.getLoyaltyTier())
                .build();
    }

    public void updateEntity(GuestProfileUpdateRequest request, GuestProfile entity) {
        entity.setSalutation(request.getSalutation());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setPersonalEmail(request.getPersonalEmail());
        entity.setOfficialEmail(request.getOfficialEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setMobileNumber(request.getMobileNumber());
        entity.setAddress(request.getAddress());
        entity.setCity(request.getCity());
        entity.setState(request.getState());
        entity.setCountry(request.getCountry());
        entity.setPostalCode(request.getPostalCode());
        entity.setNationality(request.getNationality());
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setGender(request.getGender());
        entity.setCompanyName(request.getCompanyName());
        entity.setVipStatus(request.getVipStatus());
        entity.setIdType(request.getIdType());
        entity.setIdNumber(request.getIdNumber());
        entity.setIdDocumentPath(request.getIdDocumentPath());
        entity.setLoyaltyMembershipNumber(request.getLoyaltyMembershipNumber());
        entity.setLoyaltyTier(request.getLoyaltyTier());
    }

    public GuestProfileResponse toResponse(GuestProfile entity) {
        return GuestProfileResponse.builder()
                .id(entity.getId())
                .guestId(entity.getGuestId())
                .propertyId(entity.getPropertyId())
                .salutation(entity.getSalutation())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .personalEmail(entity.getPersonalEmail())
                .officialEmail(entity.getOfficialEmail())
                .phoneNumber(entity.getPhoneNumber())
                .mobileNumber(entity.getMobileNumber())
                .address(entity.getAddress())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .postalCode(entity.getPostalCode())
                .nationality(entity.getNationality())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .companyName(entity.getCompanyName())
                .vipStatus(entity.getVipStatus())
                .idType(entity.getIdType())
                .idNumber(entity.getIdNumber())
                .idDocumentPath(entity.getIdDocumentPath())
                .loyaltyMembershipNumber(entity.getLoyaltyMembershipNumber())
                .loyaltyTier(entity.getLoyaltyTier())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
