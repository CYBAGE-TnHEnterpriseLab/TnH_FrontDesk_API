package com.pms.guest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.guest.entity.GuestProfile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:guestdetails;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
    "spring.jpa.properties.hibernate.default_schema=guestdb"
})
class GuestDetailsRepositoryTest {

    @Autowired
    private GuestProfileRepository repository;

    @BeforeEach
    void setUp() {
        repository.saveAll(List.of(
                guest("GST-1", "PROP-A", "Ava@Example.com", null, "555", null),
                guest("GST-2", "PROP-A", null, "ava@example.com", null, "555"),
                guest("GST-3", "PROP-B", "Ava@Example.com", null, "555", null)));
    }

    @Test
    void emailMatchingIsExactAndCaseSensitive() {
        assertThat(repository.findByPropertyIdAndPersonalEmail("PROP-A", "Ava@Example.com"))
                .extracting(GuestProfile::getGuestId).containsExactly("GST-1");
        assertThat(repository.findByPropertyIdAndPersonalEmail("PROP-A", "ava@example.com")).isEmpty();
        assertThat(repository.findByPropertyIdAndOfficialEmail("PROP-A", "ava@example.com"))
                .extracting(GuestProfile::getGuestId).containsExactly("GST-2");
        assertThat(repository.findByPropertyIdAndOfficialEmail("PROP-A", "AVA@EXAMPLE.COM")).isEmpty();
    }

    @Test
    void phoneMatchingIsPropertyScopedAcrossPhoneAndMobile() {
        assertThat(repository.findByPropertyIdAndPhoneNumber("PROP-A", "555"))
                .extracting(GuestProfile::getGuestId).containsExactly("GST-1");
        assertThat(repository.findByPropertyIdAndMobileNumber("PROP-A", "555"))
                .extracting(GuestProfile::getGuestId).containsExactly("GST-2");
    }

    @Test
    void idLookupIsPropertyScoped() {
        List<Long> allIds = repository.findAll().stream().map(GuestProfile::getId).toList();
        assertThat(repository.findByPropertyIdAndIdIn("PROP-A", allIds))
                .extracting(GuestProfile::getGuestId).containsExactlyInAnyOrder("GST-1", "GST-2");
    }

    private static GuestProfile guest(
            String guestId, String propertyId, String personalEmail, String officialEmail,
            String phone, String mobile) {
        return GuestProfile.builder()
                .guestId(guestId)
                .propertyId(propertyId)
                .firstName("Ava")
                .lastName("Guest")
                .personalEmail(personalEmail)
                .officialEmail(officialEmail)
                .phoneNumber(phone)
                .mobileNumber(mobile)
                .vipStatus(false)
                .build();
    }
}
