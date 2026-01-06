package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;

/**
 * FieldValidator
 *
 * Valide un champ individuel (String) en appliquant les règles du mapping :
 * - required / nullable
 * - type attendu
 * - regex (pattern)
 *
 * Cette classe est utilisée par IngestionPipeline pour chaque champ
 * de chaque record.
 */
public class FieldValidator {

    /**
     * Vérifie la compatibilité d'une valeur String avec un type logique
     * (LONG, STRING, LOCAL_DATE, DECIMAL, etc.).
     */
    private final TypeChecker typeChecker = new TypeChecker();

    /**
     * Valide une valeur brute provenant du fichier.
     *
     * @param rule règle de mapping du champ (nom, type, required, nullable, pattern)
     * @param raw  valeur brute lue depuis le fichier (String ou null)
     * @param line numéro de ligne/record (pour logs et erreurs)
     * @return valeur normalisée (trim) ou null
     *
     * @throws RecordValidationException si une règle n'est pas respectée
     */
    public String validate(FieldRule rule, String raw, int line) {

        // Détection valeur absente ou vide
        boolean blank = (raw == null || raw.trim().isEmpty());

        // 1) REQUIRED / NULLABLE
        if (blank) {

            // Champ obligatoire mais valeur absente
            if (rule.isRequired()) {
                throw new RecordValidationException(
                        ErrorCode.REQUIRED_FIELD_MISSING,
                        rule.getName(),
                        line,
                        "Required field '" + rule.getName() + "' is missing/empty"
                );
            }

            // Champ non nullable mais valeur absente
            if (!rule.isNullable()) {
                throw new RecordValidationException(
                        ErrorCode.NULL_NOT_ALLOWED,
                        rule.getName(),
                        line,
                        "Field '" + rule.getName() + "' cannot be null/empty"
                );
            }

            // Champ optionnel et nullable → OK
            return null;
        }

        // Normalisation (suppression des espaces)
        String value = raw.trim();

        // 2) TYPE CHECK
        // Vérifie que la valeur correspond au type déclaré dans le mapping
        if (!typeChecker.matches(rule.getType(), value)) {
            throw new RecordValidationException(
                    ErrorCode.TYPE_MISMATCH,
                    rule.getName(),
                    line,
                    "Type mismatch for '" + rule.getName() + "': expected " + rule.getType()
            );
        }

        // 3) PATTERN CHECK (si une regex est définie)
        if (rule.getPattern() != null && !rule.getPattern().isBlank()) {
            if (!java.util.regex.Pattern.matches(rule.getPattern(), value)) {
                throw new RecordValidationException(
                        ErrorCode.PATTERN_MISMATCH,
                        rule.getName(),
                        line,
                        "Field '" + rule.getName() + "' does not match pattern"
                );
            }
        }

        // Valeur valide et normalisée
        return value;
    }
}
