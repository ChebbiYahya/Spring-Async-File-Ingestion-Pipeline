package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.custom;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

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
     * Vérifie l'existence d'au moins une entité en base
     * correspondant à un ensemble dynamique de champs (clé de doublon).
     *
     * Cette méthode est générique :
     * - elle fonctionne pour n'importe quelle entité JPA
     * - elle construit la requête dynamiquement à partir des champs fournis
     *
     * Exemple conceptuel :
     * fields = { "id"=12, "firstName"="John", "lastName"="Doe" }
     *
     * => SELECT COUNT(e)
     *    FROM Entity e
     *    WHERE e.id = 12
     *      AND e.firstName = 'John'
     *      AND e.lastName = 'Doe'
     *
     * @param entityClass classe JPA cible (ex: Employee.class)
     * @param fields Map <nomDuChamp, valeurTypée>
     *               Les clés doivent correspondre aux attributs JPA de l'entité
     * @return true s'il existe au moins un enregistrement correspondant en base
     */

    public boolean existsByFields(Class<?> entityClass, Map<String, Object> fields) {

        // 1) Récupère le CriteriaBuilder depuis l'EntityManager
        //    Il sert à construire dynamiquement des requêtes typées JPA
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // 2) Création d'une requête Criteria qui retourne un Long
        //    (ici : le nombre d'enregistrements correspondants)
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        // 3) Définition de la racine de la requête (FROM EntityClass e)
        Root<?> root = cq.from(entityClass);

        // 4) Création d'un prédicat initial "TRUE"
        //    cb.conjunction() = 1=1
        //    Utile pour enchaîner dynamiquement des AND
        Predicate predicate = cb.conjunction();

        // 5) Pour chaque champ fourni :
        //    on ajoute une condition e.<field> = <value>
        for (var entry : fields.entrySet()) {

            // root.get(fieldName) fait référence à l'attribut JPA de l'entité
            // cb.equal(...) génère une condition d'égalité
            predicate = cb.and(
                    predicate,
                    cb.equal(root.get(entry.getKey()), entry.getValue())
            );
        }

        // 6) SELECT COUNT(e) WHERE <predicate>
        //    On ne charge pas l'entité, on compte simplement
        cq.select(cb.count(root)).where(predicate);

        // 7) Exécution de la requête
        //    getSingleResult() retourne toujours un Long (0 ou plus)
        Long count = em.createQuery(cq).getSingleResult();

        // 8) Si au moins un enregistrement existe, c'est un doublon
        return count != null && count > 0;
    }
}
