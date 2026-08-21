package com.folio.billing.repository;

import com.folio.billing.entity.Folio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FolioRepository extends JpaRepository<Folio, Long> {
    Optional<Folio> findByConfirmationNumberAndFolioCode(String confirmationNumber, String folioCode);
    List<Folio> findByConfirmationNumberOrderByFolioCode(String confirmationNumber);
}
