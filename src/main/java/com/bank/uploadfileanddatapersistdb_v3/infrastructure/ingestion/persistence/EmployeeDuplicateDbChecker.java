package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.TypeConverter;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.custom.EmployeeDuplicateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * EmployeeDuplicateDbChecker
 *
 * Objectif:
 * - Détecter si un record (ligne CSV / record XML) existe déjà en base
 * - Selon les champs configurés dans duplicateCheck (ex: ["id","firstName","lastName"])
 *
 * Pourquoi:
 * - Eviter d'insérer des doublons
 * - Produire un statut "DUPLICATE_IN_DB" dans les logs d'import
 *
 * Principe:
 * - Le pipeline fournit un record validé sous forme Map<String,String>
 * - On convertit chaque valeur en type Java correct (Long, LocalDate, BigDecimal, String, ...)
 *   en se basant sur le mapping (FieldRule.type)
 * - On délègue au repository custom qui construit une requête SQL dynamique
 */
@Component
@RequiredArgsConstructor
public class EmployeeDuplicateDbChecker {

    /**
     * Repository custom (Criteria API) capable de faire une requête dynamique du type :
     * SELECT COUNT(*) FROM employees
     * WHERE id = :id AND firstName = :firstName AND lastName = :lastName
     */
    private final EmployeeDuplicateRepository duplicateRepository;

    /**
     * Convertisseur générique (déjà présent dans ton projet)
     * - LONG -> Long
     * - LOCAL_DATE -> LocalDate
     * - DECIMAL -> BigDecimal
     * - STRING -> String
     */
    private final TypeConverter typeConverter = new TypeConverter();

    /**
     * Vérifie si un record équivalent existe déjà en base.
     *
     * @param record          record validé (valeurs en String)
     * @param duplicateFields champs utilisés pour détecter le doublon (ex: ["id","firstName","lastName"])
     * @param rules           règles issues du mapping (CSV columns ou XML fields) :
     *                        contiennent le type de chaque champ (LONG/DECIMAL/LOCAL_DATE/STRING)
     *
     * @return true si un doublon existe déjà en DB, false sinon
     */
    public boolean exists(Map<String, String> record,
                          List<String> duplicateFields,
                          List<? extends FieldRule> rules) {

        /**
         * Construire un index pour accéder rapidement à la règle d'un champ par son nom :
         *   ruleByName.get("salary") -> FieldRule(type="DECIMAL", ...)
         */
        Map<String, FieldRule> ruleByName = rules.stream()
                .collect(Collectors.toMap(
                        FieldRule::getName,          // clé = name du champ (ex: "id")
                        Function.identity(),         // valeur = l'objet FieldRule lui-même
                        (a, b) -> a                  // si doublon de clé, on garde le premier
                ));

        /**
         * Criteria typés à envoyer au repository.
         * Important : JPA compare sur les types réels des colonnes.
         * Exemple:
         * - id doit être un Long (pas "123" en String)
         * - hireDate doit être un LocalDate
         */
        Map<String, Object> criteria = new HashMap<>();

        // Pour chaque champ défini comme "clé de doublon"
        for (String field : duplicateFields) {

            // Valeur brute issue du fichier (String)
            String raw = record.get(field);

            // Règle associée (contient le type)
            FieldRule rule = ruleByName.get(field);

            /**
             * Si le champ n'existe pas dans le mapping, on fait un fallback :
             * - on garde raw (String) tel quel
             * Alternative possible : lever une exception de configuration.
             */
            if (rule == null) {
                criteria.put(field, raw);
                continue;
            }

            // Si la valeur est absente/vides => null dans la requête
            if (raw == null || raw.trim().isEmpty()) {
                criteria.put(field, null);
                continue;
            }

            // Conversion générique selon le type défini dans le mapping
            Object typedValue = typeConverter.convert(rule.getType(), raw.trim());

            criteria.put(field, typedValue);
        }

        // Exécution de la requête dynamique (true si un enregistrement équivalent existe)
        return duplicateRepository.existsByFields(criteria);
    }
}
