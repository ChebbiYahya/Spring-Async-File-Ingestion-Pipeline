package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

import lombok.Getter;

/**
 * RecordValidationException
 *
 * Exception métier utilisée pour signaler un problème fonctionnel
 * sur une ligne (record) lors de l’ingestion.
 *
 * Elle transporte :
 * - un code d'erreur normalisé (ErrorCode)
 * - le champ concerné
 * - le numéro de ligne
 * - un message explicatif
 *
 * Cette exception est levée volontairement et attrapée
 * par IngestionPipeline pour :
 * - marquer la ligne comme FAILED
 * - continuer le traitement
 * - alimenter les logs d'import
 */
@Getter
public class RecordValidationException extends RuntimeException {

    /**
     * Code d'erreur normalisé (enum ErrorCode).
     * Permet un traitement uniforme des erreurs.
     */
    private final ErrorCode code;

    /**
     * Nom du champ concerné par l'erreur.
     * Exemple : "salary", "firstName", ...
     */
    private final String field;

    /**
     * Numéro de ligne/record dans le fichier.
     * Utilisé pour le log et le reporting.
     */
    private final int line;

    /**
     * Construit une exception métier liée à un record.
     *
     * @param code    type d'erreur (ErrorCode)
     * @param field   champ concerné
     * @param line    numéro de ligne
     * @param message message explicatif
     */
    public RecordValidationException(ErrorCode code, String field, int line, String message) {
        super(message); // message accessible via getMessage()
        this.code = code;
        this.field = field;
        this.line = line;
    }
}
