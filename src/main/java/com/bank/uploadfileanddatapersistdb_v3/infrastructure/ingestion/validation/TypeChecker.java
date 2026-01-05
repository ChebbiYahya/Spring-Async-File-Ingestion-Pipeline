package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

/**
 * TypeChecker
 *
 * Vérifie si une valeur String correspond au type logique attendu
 * défini dans le mapping (FieldRule).
 *
 * IMPORTANT :
 * - Cette classe ne convertit PAS les valeurs
 * - Elle vérifie uniquement la compatibilité du format
 *
 * Elle est utilisée AVANT la conversion réelle pour éviter
 * les exceptions techniques (NumberFormatException, etc.).
 */
public class TypeChecker {

    /**
     * Vérifie si une valeur brute correspond au type attendu.
     *
     * @param type type logique attendu (LONG, DECIMAL, LOCAL_DATE, STRING)
     * @param raw  valeur brute (non nulle, déjà trimée)
     * @return true si la valeur est compatible avec le type
     */
    public boolean matches(String type, String raw) {

        return switch (type) {

            // Vérifie si la valeur peut être convertie en Long
            case "LONG" -> isLong(raw);

            // Vérifie si la valeur peut être convertie en BigDecimal
            case "DECIMAL" -> isDecimal(raw);

            // Vérifie si la valeur respecte le format ISO yyyy-MM-dd
            case "LOCAL_DATE" -> isIsoLocalDate(raw);

            // Une String est toujours valide
            case "STRING" -> true;

            // Sécurité : type inconnu → on ne bloque pas
            default -> true;
        };
    }

    /**
     * Teste la compatibilité avec le type Long.
     */
    private boolean isLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Teste la compatibilité avec le type BigDecimal.
     */
    private boolean isDecimal(String s) {
        try {
            new java.math.BigDecimal(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Teste la compatibilité avec le type LocalDate (ISO-8601).
     * Format attendu : yyyy-MM-dd
     */
    private boolean isIsoLocalDate(String s) {
        try {
            java.time.LocalDate.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
