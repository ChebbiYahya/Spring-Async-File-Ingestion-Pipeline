package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for LogChargement entity.
 */
@Repository
public interface LogChargementRepository extends JpaRepository<LogChargement, Long> {
}
