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
 * Vérifie l’existence d’un doublon en base de données
 * pour n’importe quelle entité JPA.
 *
 * Fonctionne uniquement à partir :
 * - du record lu (Map<String,String>)
 * - de la configuration duplicateCheck
 * - des règles de mapping (FieldRule)
 */
@Component
@RequiredArgsConstructor
public class GenericDuplicateDbChecker {

    /**
     * Repository générique capable de construire
     * une requête dynamique via Criteria API.
     */
    private final GenericDuplicateRepository duplicateRepository;

    /**
     * Convertit les valeurs String en types Java
     * compatibles avec les colonnes JPA.
     */
    private final TypeConverter typeConverter = new TypeConverter();

    /**
     * Vérifie si un record équivalent existe déjà en base.
     *
     * @param record          ligne lue depuis le fichier (valeurs String)
     * @param duplicateFields champs utilisés pour la détection de doublons
     * @param rules           règles de mapping (type, nullable, etc.)
     * @param entityClass     classe de l’entité JPA ciblée
     * @return true si un doublon existe en base
     */
    public boolean exists(
            Map<String, String> record,
            List<String> duplicateFields,
            List<? extends FieldRule> rules,
            Class<?> entityClass
    ) {

        // Indexation des règles par nom de champ
        Map<String, FieldRule> ruleByName = rules.stream()
                .collect(Collectors.toMap(
                        FieldRule::getName,
                        Function.identity(),
                        (a, b) -> a
                ));

        // Critères typés pour la requête DB
        Map<String, Object> criteria = new HashMap<>();

        for (String field : duplicateFields) {

            // Valeur brute issue du fichier
            String raw = record.get(field);

            // Règle associée (type, nullable…)
            FieldRule rule = ruleByName.get(field);

            // Fallback si règle absente
            if (rule == null) {
                criteria.put(field, raw);
                continue;
            }

            // Valeur vide => null
            if (raw == null || raw.trim().isEmpty()) {
                criteria.put(field, null);
                continue;
            }

            // Conversion générique String -> type Java
            Object typedValue = typeConverter.convert(rule.getType(), raw.trim());
            criteria.put(field, typedValue);
        }

        // Exécution de la requête dynamique
        return duplicateRepository.existsByFields(entityClass, criteria);
    }
}
