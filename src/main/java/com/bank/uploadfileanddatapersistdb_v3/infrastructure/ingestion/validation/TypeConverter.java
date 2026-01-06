package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TypeConverter
 *
 * Convertit une valeur String déjà VALIDÉE
 * vers le type Java correspondant au mapping.
 *
 * IMPORTANT :
 * - Cette classe suppose que la valeur est valide
 *   (TypeChecker a déjà été exécuté).
 * - Elle ne gère pas la validation, uniquement la conversion.
 */
public class TypeConverter {

    /**
     * Convertit une valeur String vers le type Java attendu.
     *
     * @param type type logique du mapping (LONG, DECIMAL, LOCAL_DATE, STRING)
     * @param raw  valeur String validée et non nulle
     * @return valeur typée (Long, BigDecimal, LocalDate, String)
     */
    public Object convert(String type, String raw) {

        return switch (type) {

            // Conversion vers Long
            case "LONG" ->
                    Long.valueOf(raw);

            // Conversion vers BigDecimal
            case "DECIMAL" ->
                    new BigDecimal(raw);

            // Conversion vers LocalDate (ISO-8601 yyyy-MM-dd)
            case "LOCAL_DATE" ->
                    LocalDate.parse(raw);

            // Pas de conversion pour String
            case "STRING" ->
                    raw;

            // Sécurité : type inconnu → on retourne la String
            default ->
                    raw;
        };
    }
}
