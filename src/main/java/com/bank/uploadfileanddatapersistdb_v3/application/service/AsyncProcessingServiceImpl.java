package com.bank.uploadfileanddatapersistdb_v3.application.service;
// Orchestration du traitement batch asynchrone.

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * AsyncProcessingServiceImpl
 *
 * Service responsable de lancer un traitement "batch" asynchrone sur les fichiers déposés dans DATA_IN.
 *
 * Objectifs principaux :
 * 1) Calculer le nombre total d’enregistrements à traiter (pour afficher une progression fiable)
 * 2) Traiter les fichiers un par un :
 *    - déplacer 1 fichier de DATA_IN vers DATA_TREATMENT (zone de travail)
 *    - ingérer le contenu (CSV/XML) via FileIngestionService
 *    - déplacer le fichier traité vers DATA_BACKUP (succès) ou DATA_FAILED (erreur)
 * 3) Mettre à jour l’état d’avancement (JobProgressService) après chaque record traité
 *
 * Important :
 * - Le traitement est asynchrone grâce à @Async.
 * - Le controller appelle startJob(...) pour init le job, puis runJob(...) pour l’exécuter en background.
 */
@Service
@RequiredArgsConstructor
public class AsyncProcessingServiceImpl implements AsyncProcessingService {

    /**
     * Fournit les chemins des dossiers (base/in/treatment/backup/failed)
     * en fonction d’une configId (ex: CONFIG_ID).
     * Les chemins proviennent de la config stockée en DB.
     */
    private final DataFoldersProvider folders;

    /**
     * Service qui gère le filesystem :
     * - création dossiers
     * - déplacement des fichiers entre DATA_IN / DATA_TREATMENT / DATA_BACKUP / DATA_FAILED
     */
    private final FolderService folderService;

    /**
     * Service d’ingestion (parsing + validation + duplicate check + persistence + logs).
     * Il expose :
     * - ingestCsvPathWithProgress(...)
     * - ingestXmlPathWithProgress(...)
     */
    private final FileIngestionService ingestionService;

    /**
     * Stocke en mémoire l’état d’avancement d’un job (RUNNING/FINISHED/FAILED),
     * le totalRecords, processedRecords, le percent, etc.
     */
    private final JobProgressService jobProgressService;

    /**
     * Stocke le resultat final par job (fichiers traites / echoues).
     */
    private final JobResultService jobResultService;

    /**
     * Compte le nombre d’enregistrements dans un fichier CSV/XML
     * sans charger tout en mémoire (streaming).
     * Sert à calculer totalRecords au début du job.
     */
    private final FileRecordCounter fileRecordCounter;

    /**
     * Démarre un job :
     * - s’assure que les dossiers existent
     * - determine la configId a utiliser
     * - compte le nombre total d’enregistrements dans DATA_IN (tous fichiers)
     * - initialise le job dans JobProgressService et retourne son jobId
     *
     * @param configId identifiant de configuration
     * @return jobId unique (UUID) à utiliser ensuite pour suivre la progression
     */
    @Override
    public String startJob(String configId) {
        String id = requireConfigId(configId);
        folderService.ensureFoldersExist(id);

        // totalRecords sert a calculer un % realiste + ETA (timeLeft)
        int totalRecords = countTotalRecordsInDataIn(id);

        // Creation du job (status RUNNING) + stockage du totalRecords
        String jobId = jobProgressService.start(totalRecords);
        jobResultService.start(jobId);
        return jobId;
    }

    /**
     * Exécute le job de façon asynchrone.
     *
     * @Async signifie :
     * - la méthode s’exécute dans un thread séparé
     * - l’appel HTTP (controller) n’attend pas la fin du traitement
     *
     * Traitement :
     * - boucle infinie:
     *    - déplacer un fichier depuis DATA_IN vers DATA_TREATMENT
     *    - si aucun fichier => stop
     *    - selon extension => ingestion CSV/XML
     *    - en succès => move vers BACKUP
     *    - en erreur => move vers FAILED (et on continue)
     * - fin => jobProgressService.finish(jobId)
     * - erreur globale => jobProgressService.fail(jobId)
     *
     * @param jobId identifiant du job (retourné par startJob)
     * @param configId identifiant de config
     */
    @Override
    @Async
    public void runJob(String jobId, String configId) {

        String id = requireConfigId(configId);

        try {
            while (true) {

                // 1) Prendre 1 fichier du dossier IN et le déplacer en TREATMENT
                //    (le fichier est renommé avec timestamp par FolderService)
                Path treatmentFile = folderService.moveOneFromInToTreatmentWithTimestamp(id);

                // S'il n'y a plus de fichiers à traiter, on sort de la boucle
                if (treatmentFile == null) break;

                // Nom du fichier en minuscule pour tester l’extension
                String name = treatmentFile.getFileName().toString().toLowerCase(Locale.ROOT);

                try {
                    // 2) Ingestion selon le type de fichier
                    if (name.endsWith(".csv")) {

                        // ingestCsvPathWithProgress(...) traite chaque record et
                        // appelle progressReporter.onRecordProcessed() après chaque record.
                        // Ici, on branche ce callback sur jobProgressService.incrementProcessed(jobId).
                        ingestionService.ingestCsvPathWithProgress(
                                treatmentFile,
                                id,
                                () -> jobProgressService.incrementProcessed(jobId)
                        );

                    } else if (name.endsWith(".xml")) {

                        ingestionService.ingestXmlPathWithProgress(
                                treatmentFile,
                                id,
                                () -> jobProgressService.incrementProcessed(jobId)
                        );

                    } else {
                        // 3) Type non supporté => on le met en FAILED.
                        // Note : on ne fait pas incrementProcessed car ce fichier
                        // ne fait normalement pas partie du "totalRecords" (countRecords renvoie 0).
                        jobResultService.addFailed(jobId, treatmentFile.getFileName().toString(), "Unsupported file type");
                        folderService.moveTreatmentToFailed(id, treatmentFile);
                        continue;
                    }

                    // 4) Si ingestion OK => on archive en BACKUP
                    folderService.moveTreatmentToBackup(id, treatmentFile);
                    jobResultService.addTreated(jobId, treatmentFile.getFileName().toString());

                } catch (Exception ex) {
                    // Erreur sur ce fichier : on log et on le déplace en FAILED
                    org.slf4j.LoggerFactory.getLogger(AsyncProcessingServiceImpl.class)
                            .error("Processing failed for file {}: {}", treatmentFile.getFileName(), ex.getMessage(), ex);

                    jobResultService.addFailed(
                            jobId,
                            treatmentFile.getFileName().toString(),
                            ex.getMessage()
                    );
                    folderService.moveTreatmentToFailed(id, treatmentFile);

                    // Important : on continue la boucle => le job traite les autres fichiers
                    // (on ne stoppe pas tout le batch sur une erreur isolée)
                }
            }

            // Tous les fichiers ont été traités (ou plus de fichiers dans DATA_IN)
            jobProgressService.finish(jobId);

        } catch (Exception ex) {
            // Erreur globale "hors fichier" (ex: problème listing dossier, etc.)
            jobProgressService.fail(jobId);
        }
    }

    /**
     * Calcule le total d’enregistrements à traiter dans DATA_IN.
     *
     * Étapes :
     * 1) récupérer le chemin du dossier IN (dépendant de configId)
     * 2) lister tous les fichiers réguliers
     * 3) pour chaque fichier, compter le nombre de records (CSV ou XML)
     * 4) sommer pour obtenir totalRecords
     *
     * Pourquoi :
     * - JobProgressService a besoin d’un totalRecords pour afficher percent et ETA.
     */
    private int countTotalRecordsInDataIn(String configId) {
        try {
            // Récupère le dossier IN depuis la config DB via DataFoldersProvider
            Path inDir = folders.inPath(configId);

            // Liste tous les fichiers réguliers
            List<Path> files;
            try (var s = Files.list(inDir)) {
                files = s.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString))
                        .collect(Collectors.toList());
            }

            // Somme des records de tous les fichiers
            int total = 0;
            for (Path p : files) {
                // countRecords(...) retourne 0 si extension inconnue
                total += Math.max(0, fileRecordCounter.countRecords(p, configId));
            }
            return total;

        } catch (Exception e) {
            // Si le comptage échoue, on log et on retourne 0
            org.slf4j.LoggerFactory.getLogger(AsyncProcessingServiceImpl.class)
                    .error("Failed to count total records in DATA_IN: {}", e.getMessage(), e);
            return 0;
        }
    }

    private String requireConfigId(String configId) {
        if (configId == null || configId.isBlank()) {
            throw new IllegalArgumentException("configId is required");
        }
        return configId;
    }
}
