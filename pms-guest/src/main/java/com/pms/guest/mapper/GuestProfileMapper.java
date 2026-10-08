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
                .vipStatus(Boolean.TRUE.equals(request.getVipStatus()))
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .idDocumentPath(request.getIdDocumentPath())
                .loyaltyMembershipNumber(request.getLoyaltyMembershipNumber())
                .loyaltyTier(request.getLoyaltyTier())
                .build();
    }

    public void updateEntity(GuestProfileUpdateRequest request, GuestProfile entity) {
        if (request.getSalutation() != null) entity.setSalutation(request.getSalutation());
        if (request.getFirstName() != null) entity.setFirstName(request.getFirstName());
        if (request.getLastName() != null) entity.setLastName(request.getLastName());
        if (request.getPersonalEmail() != null) entity.setPersonalEmail(request.getPersonalEmail());
        if (request.getOfficialEmail() != null) entity.setOfficialEmail(request.getOfficialEmail());
        if (request.getPhoneNumber() != null) entity.setPhoneNumber(request.getPhoneNumber());
        if (request.getMobileNumber() != null) entity.setMobileNumber(request.getMobileNumber());
        if (request.getAddress() != null) entity.setAddress(request.getAddress());
        if (request.getCity() != null) entity.setCity(request.getCity());
        if (request.getState() != null) entity.setState(request.getState());
        if (request.getCountry() != null) entity.setCountry(request.getCountry());
        if (request.getPostalCode() != null) entity.setPostalCode(request.getPostalCode());
        if (request.getNationality() != null) entity.setNationality(request.getNationality());
        if (request.getDateOfBirth() != null) entity.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) entity.setGender(request.getGender());
        if (request.getCompanyName() != null) entity.setCompanyName(request.getCompanyName());
        if (request.getVipStatus() != null) entity.setVipStatus(request.getVipStatus());
        if (request.getIdType() != null) entity.setIdType(request.getIdType());
        if (request.getIdNumber() != null) entity.setIdNumber(request.getIdNumber());
        if (request.getIdDocumentPath() != null) entity.setIdDocumentPath(request.getIdDocumentPath());
        if (request.getLoyaltyMembershipNumber() != null) {
            entity.setLoyaltyMembershipNumber(request.getLoyaltyMembershipNumber());
        }
        if (request.getLoyaltyTier() != null) entity.setLoyaltyTier(request.getLoyaltyTier());
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
