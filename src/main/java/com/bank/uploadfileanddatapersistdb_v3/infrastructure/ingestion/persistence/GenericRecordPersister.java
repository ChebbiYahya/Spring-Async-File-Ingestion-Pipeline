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
 * GenericRecordPersister
 *
 * Persiste une entité JPA à partir d’un record CSV/XML
 * sans coder en dur les setters.
 *
 * Cette classe :
 * - est générique (fonctionne avec n’importe quelle entité)
 * - utilise les règles de mapping (FieldRule)
 * - délègue la conversion record -> entité à RecordToEntityMapper
 * - persiste via EntityManager
 */
@Component
@RequiredArgsConstructor
public class GenericRecordPersister {

    /**
     * Mapper générique qui transforme un record (Map<String,String>)
     * en entité JPA en utilisant les règles de mapping.
     */
    private final RecordToEntityMapper recordToEntityMapper;

    /**
     * EntityManager JPA pour persister dynamiquement
     * sans dépendre d’un repository spécifique.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Persiste un record sous forme d’entité JPA.
     *
     * @param record      ligne du fichier (valeurs brutes String)
     * @param rules       règles de mapping (type, nullable, etc.)
     * @param entityClass classe de l’entité cible (ex: Employee.class)
     * @param <T>         type générique de l’entité
     */
    @Transactional
    public <T> void persist(
            Map<String, String> record,
            List<? extends FieldRule> rules,
            Class<T> entityClass
    ) {
        // Conversion dynamique record -> entité JPA
        T entity = recordToEntityMapper.toEntity(record, rules, entityClass);

        // merge = insert ou update selon la présence de l’ID
        em.merge(entity);
    }
}
