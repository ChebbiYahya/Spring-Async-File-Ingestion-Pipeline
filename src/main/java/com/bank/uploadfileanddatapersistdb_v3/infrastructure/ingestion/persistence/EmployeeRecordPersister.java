package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Persiste un Employee sans setters codés en dur.
 * La conversion + affectation est faite par RecordToEntityMapper.
 */
@Component
@RequiredArgsConstructor
public class EmployeeRecordPersister {

    private final EmployeeRepository employeeRepository;
    private final RecordToEntityMapper recordToEntityMapper;

    public void persist(Map<String, String> record, List<? extends FieldRule> rules) {
        Employee entity = recordToEntityMapper.toEntity(record, rules, Employee.class);
        employeeRepository.save(entity);
    }
}
