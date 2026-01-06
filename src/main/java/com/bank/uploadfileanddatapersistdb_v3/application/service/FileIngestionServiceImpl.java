package com.bank.uploadfileanddatapersistdb_v3.application.service;
// Orchestrateur d'ingestion CSV/XML.

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileIngestionService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.StreamProcessingException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.filesystem.PathMultipartFile;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser.CsvRecordReader;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser.RecordReader;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser.XmlRecordReader;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence.GenericDuplicateDbChecker;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence.GenericRecordPersister;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.pipeline.IngestionPipeline;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.MappingRegistry;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.CsvSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.ProgressReporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * FileIngestionServiceImpl
 *
 * Service applicatif qui orchestre l’ingestion d’un fichier (CSV/XML).
 *
 * Responsabilités :
 * - charger le "schéma" (mapping) depuis la configuration DB (via MappingRegistry)
 * - créer le RecordReader adapté (CSV ou XML) pour lire le fichier en streaming
 * - déléguer le traitement record-par-record au IngestionPipeline :
 *      - validation
 *      - détection de doublons (fichier + DB)
 *      - persistance
 *      - logs détaillés
 *      - callback de progression
 *
 * Important :
 * - Ce service ne fait pas les validations ni la persistance directement.
 * - Il connecte les composants (coordination).
 */
@Service
@RequiredArgsConstructor
public class FileIngestionServiceImpl implements FileIngestionService {

    /**
     * Charge les schémas CSV/XML à partir de la configuration en DB (FileReaderConfig).
     */
    private final MappingRegistry mappingRegistry;

    /**
     * Pipeline générique réutilisable pour CSV et XML.
     * Il applique les règles : validation, doublons, persistance, logs, progress.
     */
    private final IngestionPipeline pipeline;

    /**
     * Persiste une ligne validee pour l'entite cible.
     */
    private final GenericRecordPersister recordPersister;

    /**
     * Vérifie si le record (selon les champs duplicateCheck) existe déjà en DB.
     */
    private final GenericDuplicateDbChecker duplicateDbChecker;

    /**
     * Ingestion d’un fichier CSV (Path) avec reporting de progression.
     *
     * @param filePath chemin du fichier CSV dans DATA_TREATMENT
     * @param configId identifiant de config (ex: EMPLOYEES)
     * @param progressReporter callback appelé après chaque record traité
     * @return nombre de records insérés avec succès
     */
    @Override
    public int ingestCsvPathWithProgress(Path filePath, String configId, ProgressReporter progressReporter) {

        // 1) Charger la config/mapping CSV depuis la DB
        CsvSchema schema = mappingRegistry.loadCsv(configId);
        Class<?> entityClass = resolveEntityClass(configId, schema.getEntityClassName());

        // 2) Adapter Path -> MultipartFile pour réutiliser CsvRecordReader
        // 3) RecordReader est AutoCloseable => try-with-resources ferme parser/streams
        try (RecordReader rr = new CsvRecordReader(new PathMultipartFile(filePath), schema)) {

            // 4) Délégation au pipeline générique
            return pipeline.process(
                    filePath.getFileName().toString(),    // nom pour les logs
                    schema.getDuplicateCheck(),           // champs de détection doublons
                    rr.iterator(),                        // records (Map<String,String>) en streaming
                    schema.getColumns(),                  // règles de validation (CSV)
                    record -> recordPersister.persist(record, schema.getColumns(), entityClass),          // persister un record validé
                    (record, fields) -> duplicateDbChecker.exists(record, fields, schema.getColumns(), entityClass), // doublon DB
                    progressReporter                      // callback progression
            );

        } catch (Exception e) {
            // On normalise toute erreur technique comme StreamProcessingException
            throw new StreamProcessingException("CSV ingestion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Ingestion d’un fichier XML (Path) avec reporting de progression.
     *
     * @param filePath chemin du fichier XML dans DATA_TREATMENT
     * @param configId identifiant de config (ex: EMPLOYEES)
     * @param progressReporter callback appelé après chaque record traité
     * @return nombre de records insérés avec succès
     */
    @Override
    public int ingestXmlPathWithProgress(Path filePath, String configId, ProgressReporter progressReporter) {

        // 1) Charger la config/mapping XML depuis la DB
        XmlSchema schema = mappingRegistry.loadXml(configId);
        Class<?> entityClass = resolveEntityClass(configId, schema.getEntityClassName());

        // 2) Adapter Path -> MultipartFile pour réutiliser XmlRecordReader
        try (RecordReader rr = new XmlRecordReader(new PathMultipartFile(filePath), schema)) {

            // 3) Délégation au pipeline générique
            return pipeline.process(
                    filePath.getFileName().toString(),    // nom pour les logs
                    schema.getDuplicateCheck(),           // champs de détection doublons
                    rr.iterator(),                        // records (Map<String,String>) en streaming
                    schema.getFields(),                   // règles de validation (XML)
                    record -> recordPersister.persist(record, schema.getFields(), entityClass),           // persister un record validé
                    (record, fields) -> duplicateDbChecker.exists(record, fields, schema.getFields(), entityClass), // doublon DB
                    progressReporter                      // callback progression
            );

        } catch (Exception e) {
            throw new StreamProcessingException("XML ingestion failed: " + e.getMessage(), e);
        }
    }

    private Class<?> resolveEntityClass(String configId, String entityClassName) {
        if (entityClassName == null || entityClassName.isBlank()) {
            throw new StreamProcessingException(
                    "Missing entityClassName for config: " + configId + ". Update the file reader config in DB.",
                    new IllegalStateException("entityClassName is blank")
            );
        }
        try {
            return Class.forName(entityClassName.trim());
        } catch (ClassNotFoundException e) {
            throw new StreamProcessingException(
                    "Entity class not found: " + entityClassName + " for config: " + configId,
                    e
            );
        }
    }
}
