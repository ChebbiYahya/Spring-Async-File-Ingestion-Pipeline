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
 * EmployeeDuplicateRepository
 *
 * Repository JPA custom utilisé pour détecter des doublons en base de données.
 *
 * Il construit dynamiquement une requête de type :
 *   SELECT COUNT(*) FROM Employee
 *   WHERE field1 = :value1 AND field2 = :value2 AND ...
 *
 * Les champs utilisés sont fournis dynamiquement via une Map.
 */
@Repository
public class EmployeeDuplicateRepository {

    /**
     * EntityManager injecté par JPA.
     * Il permet de créer et exécuter des requêtes Criteria.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Vérifie si un Employee existe en base selon des champs dynamiques.
     *
     * @param fields Map <nomDuChamp, valeurTypée>
     *               Ex: { "id"=10L, "firstName"="Ali", "lastName"="Ben" }
     *
     * @return true si au moins un enregistrement correspond en DB
     */
    public boolean existsByFields(Map<String, Object> fields) {

        // 1) Création du builder Criteria
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // 2) Requête de type SELECT COUNT(*)
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        // 3) FROM Employee
        Root<Employee> root = cq.from(Employee.class);

        /**
         * 4) Construction dynamique du WHERE.
         *
         * cb.conjunction() = predicate toujours vrai (1=1)
         * On va ajouter des AND successifs dessus.
         */
        Predicate p = cb.conjunction();

        for (var e : fields.entrySet()) {

            // Exemple : root.get("id") = 10
            //           root.get("firstName") = "Ali"
            p = cb.and(
                    p,
                    cb.equal(root.get(e.getKey()), e.getValue())
            );
        }

        // 5) SELECT COUNT(root) WHERE p
        cq.select(cb.count(root)).where(p);

        // 6) Exécution de la requête
        Long count = em.createQuery(cq).getSingleResult();

        // 7) Doublon si au moins un résultat
        return count != null && count > 0;
    }
}
