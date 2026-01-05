package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.pipeline;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.LogChargementService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate.DuplicateKeyBuilder;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate.InFileDuplicateChecker;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.ErrorCode;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.FieldValidator;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.RecordValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.ProgressReporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * IngestionPipeline
 *
 * Pipeline générique d'ingestion (réutilisable pour CSV, XML, etc.)
 *
 * Il attend :
 * - un Iterator de records (Map<fieldName, rawValue>)
 * - une liste de règles de mapping (FieldRule) pour valider chaque champ
 * - une stratégie de détection de doublons en base (DuplicateDbChecker)
 * - une stratégie de persistance (RecordPersister)
 * - optionnel : progressReporter pour notifier un job async
 *
 * Flow par record :
 * 1) validation (FieldRule)
 * 2) doublon dans fichier (InFileDuplicateChecker) + doublon en base (DuplicateDbChecker)
 * 3) persist (RecordPersister)
 * 4) log success/fail
 * 5) notifier progress
 */
@Component
@RequiredArgsConstructor
public class IngestionPipeline {

    /**
     * Service de log d'import en base :
     * - startLog() crée un LogChargement
     * - addLine() ajoute un détail pour chaque ligne (SUCCESS/FAILED)
     * - finalizeLog() marque SUCCESS/PARTIAL/FAILED
     */
    private final LogChargementService logService;

    /**
     * Valide un champ (required, nullable, pattern, type, etc.)
     * selon la règle FieldRule.
     */
    private final FieldValidator fieldValidator = new FieldValidator();

    /**
     * Construit une "clé de doublon" en concaténant les champs configurés
     * (duplicateCheck) pour détecter les doublons.
     */
    private final DuplicateKeyBuilder keyBuilder = new DuplicateKeyBuilder();

    /**
     * Méthode "compatibilité" : même pipeline mais sans progressReporter.
     * Elle appelle la méthode principale avec progressReporter=null.
     */
//    public int process(
//            String fileName,
//            List<String> duplicateCheck,
//            Iterator<Map<String, String>> rawRecords,
//            List<? extends FieldRule> rules,
//            RecordPersister persister,
//            DuplicateDbChecker dbChecker
//    ) {
//        return process(fileName, duplicateCheck, rawRecords, rules, persister, dbChecker, null);
//    }

    /**
     * Traite tous les records d’un fichier.
     *
     * @param fileName nom du fichier (utilisé dans les logs)
     * @param duplicateCheck liste des champs qui définissent un doublon (ex: ["id","firstName","lastName"])
     * @param rawRecords iterator des records bruts (valeurs String)
     * @param rules règles de mapping/validation (CSV columns ou XML fields)
     * @param persister stratégie de persistance (ex: save Employee)
     * @param dbChecker stratégie doublon DB (ex: existsByFields)
     * @param progressReporter callback optionnel, appelé après chaque record
     *
     * @return nombre de records persistés avec succès
     */
    public int process(
            String fileName,
            List<String> duplicateCheck,
            Iterator<Map<String, String>> rawRecords,
            List<? extends FieldRule> rules,
            RecordPersister persister,
            DuplicateDbChecker dbChecker,
            ProgressReporter progressReporter
    ) {
        // 0) Démarre un log d'import pour ce fichier
        LogChargement log = logService.startLog(fileName);

        // Détecteur de doublons internes au fichier (mémoire)
        InFileDuplicateChecker inFile = new InFileDuplicateChecker();

        int success = 0; // compteur des records persistés
        int line = 0;    // compteur logique de lignes/records

        // Boucle principale : record par record
        while (rawRecords.hasNext()) {
            line++;
            try {
                // Record brut lu depuis le parser (CSV/XML)
                Map<String, String> raw = rawRecords.next();

                // 1) VALIDATION
                // On reconstruit un record "validé" (mêmes clés) avec valeurs normalisées
                Map<String, String> validated = new HashMap<>();

                for (FieldRule r : rules) {
                    // validate() peut :
                    // - vérifier required/nullable
                    // - appliquer pattern regex
                    // - vérifier type (ou préparer la conversion)
                    // - lever RecordValidationException si invalide
                    String v = fieldValidator.validate(r, raw.get(r.getName()), line);
                    validated.put(r.getName(), v);
                }

                // 2) DOUBLONS (si configuré)
                if (duplicateCheck != null && !duplicateCheck.isEmpty()) {

                    // Construit une clé à partir des champs duplicateCheck
                    // ex: "12|John|Doe"
                    String key = keyBuilder.buildKey(duplicateCheck, new HashMap<>(validated));

                    // 2.a) Doublon dans le même fichier
                    if (inFile.isDuplicate(key)) {
                        throw new RecordValidationException(
                                ErrorCode.DUPLICATE_IN_FILE,
                                String.join(",", duplicateCheck),
                                line,
                                "Duplicate key in file for fields: " + duplicateCheck
                        );
                    }

                    // 2.b) Doublon en base (délégué à dbChecker)
                    if (dbChecker.exists(validated, duplicateCheck)) {
                        throw new RecordValidationException(
                                ErrorCode.DUPLICATE_IN_DB,
                                String.join(",", duplicateCheck),
                                line,
                                "Duplicate key in DB for fields: " + duplicateCheck
                        );
                    }
                }

                // 3) PERSISTENCE
                // Le pipeline ne connaît pas l'entité ; il délègue au persister
                persister.persist(validated);
                success++;

                // 4) LOG : ligne OK
                logService.addLine(log, line, LineStatus.SUCCESS, null);

            } catch (RecordValidationException e) {
                // Erreur métier/validation : on log en FAILED avec code précis
                logService.addLine(log, line, LineStatus.FAILED, e.getCode() + " - " + e.getMessage());

            } catch (Exception e) {
                // Erreur technique inattendue (NPE, DB down, etc.)
                logService.addLine(log, line, LineStatus.FAILED, "TECHNICAL - " + e.getMessage());

            } finally {
                // 5) Progress reporter : on notifie après chaque record (succès ou échec)
                if (progressReporter != null) {
                    progressReporter.onRecordProcessed();
                }
            }
        }

        // Finalisation : met à jour le status global du log (SUCCESS / FAILED / PARTIAL)
        // NB: les compteurs total/success/failed sont déjà maintenus dans addLine()
        logService.finalizeLog(log, 0, 0, 0);

        // Retourne le nombre de records persistés
        return success;
    }

    /**
     * Contrat "persister" : le pipeline fournit un record validé,
     * et une implémentation concrète décide comment le sauvegarder.
     *
     * Exemple :
     * - persister.persist(record) -> mapper vers Employee -> repository.save(employee)
     */
    public interface RecordPersister {
        void persist(Map<String, String> record);
    }

    /**
     * Contrat "dbChecker" : détection de doublon en DB.
     * Le pipeline fournit :
     * - record validé
     * - champs utilisés pour la clé de doublon
     *
     * Exemple :
     * - construire criteria typé puis existsByFields(criteria)
     */
    public interface DuplicateDbChecker {
        boolean exists(Map<String, String> record, List<String> duplicateFields);
    }
}
