package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.custom;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Custom repository using Criteria API to check existence by dynamic fields.
 * Used to detect duplicates in DB based on a list of configured columns.
 */
@Repository
public class EmployeeDuplicateRepository {

    @PersistenceContext
    private EntityManager em;

    public boolean existsByFields(Map<String, Object> fields) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Employee> root = cq.from(Employee.class);

        Predicate p = cb.conjunction();
        for (var e : fields.entrySet()) {
            p = cb.and(p, cb.equal(root.get(e.getKey()), e.getValue()));
        }

        cq.select(cb.count(root)).where(p);

        Long count = em.createQuery(cq).getSingleResult();
        return count != null && count > 0;
    }
}
