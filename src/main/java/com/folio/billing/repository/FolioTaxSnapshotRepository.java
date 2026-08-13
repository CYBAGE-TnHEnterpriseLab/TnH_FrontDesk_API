package com.folio.billing.repository;

import com.folio.billing.entity.FolioTaxSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolioTaxSnapshotRepository extends JpaRepository<FolioTaxSnapshot, Long> {
}
