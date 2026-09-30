package com.pms.guest.repository;

import com.pms.guest.entity.GuestProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestProfileRepository extends JpaRepository<GuestProfile, Long> {

    boolean existsByGuestId(String guestId);

    Optional<GuestProfile> findByIdAndPropertyId(Long id, String propertyId);

    List<GuestProfile> findByPropertyIdAndMobileNumber(String propertyId, String mobileNumber);

    List<GuestProfile> findByPropertyIdAndPhoneNumber(String propertyId, String phoneNumber);

    List<GuestProfile> findByPropertyIdAndPersonalEmail(String propertyId, String personalEmail);

    List<GuestProfile> findByPropertyIdAndOfficialEmail(String propertyId, String officialEmail);

    List<GuestProfile> findByPropertyIdAndLoyaltyMembershipNumber(
            String propertyId,
            String loyaltyMembershipNumber
    );

    List<GuestProfile> findByPropertyIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            String propertyId,
            String firstName,
            String lastName
    );
}
