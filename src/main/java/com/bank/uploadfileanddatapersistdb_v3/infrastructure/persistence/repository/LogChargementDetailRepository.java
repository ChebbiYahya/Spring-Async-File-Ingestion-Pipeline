package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargementDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for LogChargementDetail entity.
 */
@Repository
public interface LogChargementDetailRepository extends JpaRepository<LogChargementDetail, Long> {
}
