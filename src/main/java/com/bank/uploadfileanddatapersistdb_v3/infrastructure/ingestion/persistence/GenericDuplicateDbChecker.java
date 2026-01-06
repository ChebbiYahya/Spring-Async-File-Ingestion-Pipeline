package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.TypeConverter;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.custom.GenericDuplicateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GenericDuplicateDbChecker
 *
 * Detects duplicate records in DB for any entity class.
 */
@Component
@RequiredArgsConstructor
public class GenericDuplicateDbChecker {

    private final GenericDuplicateRepository duplicateRepository;
    private final TypeConverter typeConverter = new TypeConverter();

    public boolean exists(Map<String, String> record,
                          List<String> duplicateFields,
                          List<? extends FieldRule> rules,
                          Class<?> entityClass) {

        Map<String, FieldRule> ruleByName = rules.stream()
                .collect(Collectors.toMap(
                        FieldRule::getName,
                        Function.identity(),
                        (a, b) -> a
                ));

        Map<String, Object> criteria = new HashMap<>();

        for (String field : duplicateFields) {
            String raw = record.get(field);
            FieldRule rule = ruleByName.get(field);

            if (rule == null) {
                criteria.put(field, raw);
                continue;
            }

            if (raw == null || raw.trim().isEmpty()) {
                criteria.put(field, null);
                continue;
            }

            Object typedValue = typeConverter.convert(rule.getType(), raw.trim());
            criteria.put(field, typedValue);
        }

        return duplicateRepository.existsByFields(entityClass, criteria);
    }
}
