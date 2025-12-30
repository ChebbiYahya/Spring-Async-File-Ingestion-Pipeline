package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for Employee entity.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
