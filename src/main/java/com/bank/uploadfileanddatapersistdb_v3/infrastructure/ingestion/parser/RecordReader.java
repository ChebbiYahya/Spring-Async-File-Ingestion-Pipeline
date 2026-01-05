package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser;

import java.util.Iterator;
import java.util.Map;

/**
 * RecordReader
 *
 * Interface générique de lecture d’enregistrements (records).
 *
 * Objectif :
 * - fournir une abstraction commune pour tous les formats de fichiers (CSV, XML, ...)
 * - permettre au pipeline d’ingestion de rester totalement indépendant du format
 *
 * Chaque implémentation doit :
 * - lire le fichier en streaming
 * - produire un Iterator de records
 * - chaque record est représenté par une Map<String, String>
 *
 * Clé   : nom du champ (défini dans la configuration)
 * Valeur: valeur brute lue dans le fichier (toujours String)
 */
public interface RecordReader extends AutoCloseable {

    /**
     * Retourne un itérateur sur les enregistrements du fichier.
     *
     * Chaque élément de l’itérateur correspond à :
     * - une ligne CSV
     * - ou un élément XML (recordElement)
     *
     * @return Iterator de records (Map fieldName -> raw String value)
     */
    Iterator<Map<String, String>> iterator();
}
