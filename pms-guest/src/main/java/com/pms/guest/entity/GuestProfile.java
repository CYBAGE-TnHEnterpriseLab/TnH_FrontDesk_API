package com.pms.guest.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "guest_profiles", schema = "guestdb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GuestProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_id", nullable = false, unique = true, length = 80)
    private String guestId;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(length = 20)
    private String salutation;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "personal_email", length = 160)
    private String personalEmail;

    @Column(name = "official_email", length = 160)
    private String officialEmail;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 80)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 80)
    private String nationality;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "company_name", length = 120)
    private String companyName;

    @Column(name = "vip_status", nullable = false)
    private Boolean vipStatus;

    @Column(name = "id_type", length = 40)
    private String idType;

    @Column(name = "id_number", length = 80)
    private String idNumber;

    @Column(name = "id_document_path", length = 500)
    private String idDocumentPath;

    @Column(name = "loyalty_membership_number", length = 40)
    private String loyaltyMembershipNumber;

    @Column(name = "loyalty_tier", length = 40)
    private String loyaltyTier;
}
