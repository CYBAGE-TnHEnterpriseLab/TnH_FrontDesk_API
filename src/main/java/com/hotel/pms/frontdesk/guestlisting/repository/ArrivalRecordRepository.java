package com.hotel.pms.frontdesk.guestlisting.repository;

import com.hotel.pms.frontdesk.guestlisting.entity.ArrivalRecord;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArrivalRecordRepository extends JpaRepository<ArrivalRecord, Long>, JpaSpecificationExecutor<ArrivalRecord> {

    Optional<ArrivalRecord> findByPropertyIdAndBusinessDateAndConfirmationNumber(
            String propertyId,
            LocalDate businessDate,
            String confirmationNumber
    );

    boolean existsByPropertyIdAndBusinessDate(String propertyId, LocalDate businessDate);
}
