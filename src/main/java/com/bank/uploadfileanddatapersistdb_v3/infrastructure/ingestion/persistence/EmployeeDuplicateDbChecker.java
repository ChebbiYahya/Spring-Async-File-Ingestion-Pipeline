package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.custom.EmployeeDuplicateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks whether an equivalent record already exists in DB using configured duplicate fields.
 */
@Component
@RequiredArgsConstructor
public class EmployeeDuplicateDbChecker {

    private final EmployeeDuplicateRepository duplicateRepository;

    public boolean exists(Map<String, String> record, List<String> duplicateFields) {
        Map<String, Object> criteria = new HashMap<>();

        for (String f : duplicateFields) {
            String v = record.get(f);
            Object typed = switch (f) {
                case "id" -> (v == null ? null : Long.parseLong(v));
                case "hireDate" -> (v == null ? null : java.time.LocalDate.parse(v));
                case "salary" -> (v == null ? null : new java.math.BigDecimal(v));
                default -> v;
            };
            criteria.put(f, typed);
        }

        return duplicateRepository.existsByFields(criteria);
    }
}
