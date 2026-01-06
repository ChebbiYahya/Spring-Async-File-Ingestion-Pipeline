package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Persists any entity without hardcoding setters.
 */
@Component
@RequiredArgsConstructor
public class GenericRecordPersister {

    private final RecordToEntityMapper recordToEntityMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public <T> void persist(Map<String, String> record,
                            List<? extends FieldRule> rules,
                            Class<T> entityClass) {
        T entity = recordToEntityMapper.toEntity(record, rules, entityClass);
        em.merge(entity);
    }
}
