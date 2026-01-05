package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate;

import java.util.HashSet;
import java.util.Set;

/**
 * InFileDuplicateChecker
 *
 * Détecte les doublons à l’intérieur d’un même fichier.
 *
 * Fonctionnement :
 * - construit une clé de doublon (ex: "10|Alice|Martin")
 * - stocke chaque clé rencontrée dans un Set
 * - si une clé existe déjà => doublon détecté
 *
 * Important :
 * - ce checker est utilisé uniquement pour le fichier en cours
 * - il ne remplace PAS la vérification des doublons en base de données
 */
public class InFileDuplicateChecker {

    /**
     * Ensemble des clés déjà rencontrées dans le fichier courant.
     *
     * HashSet :
     * - accès très rapide (O(1))
     * - pas de doublons
     */
    private final Set<String> seen = new HashSet<>();

    /**
     * Vérifie si une clé a déjà été rencontrée.
     *
     * @param key clé de doublon construite à partir du record
     * @return true si la clé existe déjà (doublon),
     *         false si c’est la première occurrence
     */
    public boolean isDuplicate(String key) {

        // add(key) retourne :
        // - true si la clé n'existait pas encore
        // - false si la clé existe déjà
        return !seen.add(key);
    }
}
