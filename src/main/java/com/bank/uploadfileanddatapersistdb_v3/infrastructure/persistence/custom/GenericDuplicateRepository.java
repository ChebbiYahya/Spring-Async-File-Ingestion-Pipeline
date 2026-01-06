package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * GenericDuplicateRepository
 *
 * Generic JPA repository to detect duplicates
 * on any entity, using dynamic fields.
 */
@Repository
public class GenericDuplicateRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Checks if an entity exists in DB using dynamic fields.
     *
     * @param entityClass target JPA class
     * @param fields      Map <fieldName, typedValue>
     * @return true si au moins un enregistrement correspond en DB
     */
    public boolean existsByFields(Class<?> entityClass, Map<String, Object> fields) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<?> root = cq.from(entityClass);

        Predicate p = cb.conjunction();
        for (var e : fields.entrySet()) {
            p = cb.and(p, cb.equal(root.get(e.getKey()), e.getValue()));
        }

        cq.select(cb.count(root)).where(p);

        Long count = em.createQuery(cq).getSingleResult();
        return count != null && count > 0;
    }
}
