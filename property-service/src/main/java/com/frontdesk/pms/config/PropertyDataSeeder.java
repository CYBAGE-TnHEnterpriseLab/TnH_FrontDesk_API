package com.frontdesk.pms.config;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.entity.Property;
import com.frontdesk.pms.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Slf4j
public class PropertyDataSeeder implements CommandLineRunner {

    private final PropertyRepository propertyRepository;

    @Override
    public void run(String... args) {
        // Idempotent: only seed when table is empty.
        long count = propertyRepository.count();
        if (count > 0) {
            log.info("Seed skipped: properties table already has {} rows", count);
            return;
        }

        List<Property> properties = List.of(
                Property.builder()
                        .name("Frontdesk Grand Mumbai")
                        .email("mumbai@frontdesk.com")
                        .address("BKC, Mumbai, MH")
                        .contactName("Amit Sharma")
                        .contactNumber("9000000001")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(14, 0))
                        .checkOutTime(LocalTime.of(11, 0))
                        .nightAuditTime(LocalTime.of(2, 0))
                        .status(PropertyStatus.ACTIVE)
                        .build(),
                Property.builder()
                        .name("Frontdesk Business Pune")
                        .email("pune@frontdesk.com")
                        .address("Hinjewadi, Pune, MH")
                        .contactName("Neha Kulkarni")
                        .contactNumber("9000000002")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(13, 0))
                        .checkOutTime(LocalTime.of(11, 0))
                        .nightAuditTime(LocalTime.of(1, 30))
                        .status(PropertyStatus.ACTIVE)
                        .build(),
                Property.builder()
                        .name("Frontdesk City Bengaluru")
                        .email("blr@frontdesk.com")
                        .address("Indiranagar, Bengaluru, KA")
                        .contactName("Rahul Iyer")
                        .contactNumber("9000000003")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(12, 0))
                        .checkOutTime(LocalTime.of(10, 0))
                        .nightAuditTime(LocalTime.of(2, 30))
                        .status(PropertyStatus.DRAFT)
                        .build(),
                Property.builder()
                        .name("Frontdesk Heritage Jaipur")
                        .email("jaipur@frontdesk.com")
                        .address("MI Road, Jaipur, RJ")
                        .contactName("Priya Singh")
                        .contactNumber("9000000004")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(15, 0))
                        .checkOutTime(LocalTime.of(11, 0))
                        .nightAuditTime(LocalTime.of(3, 0))
                        .status(PropertyStatus.DRAFT)
                        .build(),
                Property.builder()
                        .name("Frontdesk Airport Delhi")
                        .email("delhi-airport@frontdesk.com")
                        .address("Aerocity, New Delhi, DL")
                        .contactName("Karan Mehta")
                        .contactNumber("9000000005")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(14, 0))
                        .checkOutTime(LocalTime.of(12, 0))
                        .nightAuditTime(LocalTime.of(1, 0))
                        .status(PropertyStatus.ACTIVE)
                        .build(),
                Property.builder()
                        .name("Frontdesk Marina Chennai")
                        .email("chennai@frontdesk.com")
                        .address("Marina Beach Rd, Chennai, TN")
                        .contactName("Sundar Raj")
                        .contactNumber("9000000006")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(13, 30))
                        .checkOutTime(LocalTime.of(11, 0))
                        .nightAuditTime(LocalTime.of(2, 15))
                        .status(PropertyStatus.ACTIVE)
                        .build(),
                Property.builder()
                        .name("Frontdesk Gachibowli Hyderabad")
                        .email("hyd@frontdesk.com")
                        .address("Gachibowli, Hyderabad, TS")
                        .contactName("Ananya Reddy")
                        .contactNumber("9000000007")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(14, 0))
                        .checkOutTime(LocalTime.of(11, 30))
                        .nightAuditTime(LocalTime.of(2, 45))
                        .status(PropertyStatus.DRAFT)
                        .build(),
                Property.builder()
                        .name("Frontdesk Park Kolkata")
                        .email("kolkata@frontdesk.com")
                        .address("Park Street, Kolkata, WB")
                        .contactName("Soumya Bose")
                        .contactNumber("9000000008")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(12, 30))
                        .checkOutTime(LocalTime.of(10, 30))
                        .nightAuditTime(LocalTime.of(1, 45))
                        .status(PropertyStatus.ACTIVE)
                        .build(),
                Property.builder()
                        .name("Frontdesk Fort Kochi")
                        .email("kochi@frontdesk.com")
                        .address("Fort Kochi, Kochi, KL")
                        .contactName("Joseph Mathew")
                        .contactNumber("9000000009")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(15, 0))
                        .checkOutTime(LocalTime.of(11, 0))
                        .nightAuditTime(LocalTime.of(3, 15))
                        .status(PropertyStatus.DRAFT)
                        .build(),
                Property.builder()
                        .name("Frontdesk Beach Goa")
                        .email("goa@frontdesk.com")
                        .address("Calangute, Goa")
                        .contactName("Riya D'Souza")
                        .contactNumber("9000000010")
                        .timeZone("Asia/Kolkata")
                        .checkInTime(LocalTime.of(14, 30))
                        .checkOutTime(LocalTime.of(11, 30))
                        .nightAuditTime(LocalTime.of(2, 30))
                        .status(PropertyStatus.ACTIVE)
                .build()
        );

        propertyRepository.saveAll(properties);
        log.info("Seeded {} properties", properties.size());
    }
}
