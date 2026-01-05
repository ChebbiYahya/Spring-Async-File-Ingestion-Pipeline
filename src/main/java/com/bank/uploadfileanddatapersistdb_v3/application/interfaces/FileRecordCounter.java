package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import java.nio.file.Path;

/**
 * FileRecordCounter
 *
 * Contrat pour compter le nombre d’enregistrements
 * dans un fichier de données (CSV / XML).
 *
 * Objectif :
 * - permettre le calcul précis de la progression (percent, ETA)
 * - utiliser un comptage "streaming" (sans charger tout le fichier en mémoire)
 */
public interface FileRecordCounter {

    /**
     * Compte le nombre d’enregistrements métier dans un fichier.
     *
     * @param filePath chemin du fichier (CSV ou XML)
     * @param configId identifiant de configuration (ex: "EMPLOYEES")
     * @return nombre d’enregistrements trouvés (0 si non supporté)
     */
    int countRecords(Path filePath, String configId);
}
