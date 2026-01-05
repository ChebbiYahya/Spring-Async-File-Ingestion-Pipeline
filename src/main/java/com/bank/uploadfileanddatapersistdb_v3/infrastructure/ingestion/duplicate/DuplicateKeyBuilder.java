package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DuplicateKeyBuilder
 *
 * Construit une clé de doublon à partir :
 * - d’une liste de champs configurés (duplicateCheck)
 * - d’un record validé (Map<String, ?>)
 *
 * Cette clé est utilisée pour :
 * - détecter les doublons dans le fichier
 * - servir de base aux contrôles de doublons en base
 *
 * Exemple :
 *   fields = ["id", "firstName"]
 *   record = {id=10, firstName="Alice"}
 *
 *   => clé = "10|Alice"
 */
public class DuplicateKeyBuilder {

    /**
     * Construit une clé de doublon déterministe.
     *
     * @param fields noms des champs utilisés pour la détection de doublons
     * @param record record validé (clé = nom champ, valeur = valeur du champ)
     * @return clé de doublon sous forme de String
     */
    public String buildKey(List<String> fields, Map<String, ?> record) {

        return fields.stream()

                // Récupère la valeur du champ dans le record
                // Si la valeur est null, on utilise "" pour éviter les NPE
                .map(f -> java.util.Objects.toString(record.get(f), ""))

                // Concatène les valeurs avec un séparateur stable
                .collect(Collectors.joining("|"));
    }
}
