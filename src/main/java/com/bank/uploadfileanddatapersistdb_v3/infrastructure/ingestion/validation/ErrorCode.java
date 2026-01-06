package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

/**
 * ErrorCode
 *
 * Enum des codes d'erreur normalisés pour l'ingestion des fichiers.
 *
 * Ces codes sont utilisés pour :
 * - identifier précisément la cause d'échec d'un record
 * - écrire des logs cohérents (LogChargementDetail)
 * - exposer des erreurs compréhensibles via l'API
 *
 * Chaque code représente une catégorie d'erreur fonctionnelle
 * ou technique liée au traitement d'une ligne (record).
 */
public enum ErrorCode {

    /**
     * Colonne attendue par le mapping absente dans le fichier.
     * Ex: mapping attend "salary" mais la colonne n'existe pas.
     */
    MISSING_COLUMN,

    /**
     * Colonne présente dans le fichier mais non définie dans le mapping.
     * (utile en mode strict).
     */
    UNEXPECTED_COLUMN,

    /**
     * Champ requis (required=true) mais valeur absente ou vide.
     */
    REQUIRED_FIELD_MISSING,

    /**
     * Valeur nulle alors que nullable=false dans le mapping.
     */
    NULL_NOT_ALLOWED,

    /**
     * Valeur ne respectant pas la regex définie dans le mapping.
     */
    PATTERN_MISMATCH,

    /**
     * Impossible de convertir la valeur vers le type attendu
     * (ex: "abc" vers Long, date invalide, etc.).
     */
    TYPE_MISMATCH,

    /**
     * Doublon détecté à l'intérieur du même fichier.
     */
    DUPLICATE_IN_FILE,

    /**
     * Doublon détecté en base de données.
     */
    DUPLICATE_IN_DB,

    /**
     * Root XML du fichier ne correspond pas au root attendu par le mapping.
     */
    XML_ROOT_MISMATCH
}
