package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Converts a validated record into an Employee entity and persists it via JPA.
 */
@Component
@RequiredArgsConstructor
public class EmployeeRecordPersister {

    private final EmployeeRepository employeeRepository;

    public void persist(Map<String, String> record) {
        Employee e = new Employee();
        e.setId(record.get("id") == null ? null : Long.parseLong(record.get("id")));
        e.setFirstName(record.get("firstName"));
        e.setLastName(record.get("lastName"));
        e.setPosition(record.get("position"));
        e.setDepartment(record.get("department"));
        e.setHireDate(record.get("hireDate") == null ? null : java.time.LocalDate.parse(record.get("hireDate")));
        e.setSalary(record.get("salary") == null ? null : new java.math.BigDecimal(record.get("salary")));
        employeeRepository.save(e);
    }
}
