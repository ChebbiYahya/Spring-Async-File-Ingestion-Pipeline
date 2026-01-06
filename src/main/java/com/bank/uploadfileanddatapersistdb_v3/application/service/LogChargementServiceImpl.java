package com.bank.uploadfileanddatapersistdb_v3.application.service;
// Service metier des logs de chargement.

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.LogChargementService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.LogChargementNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargementDetail;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.LogChargementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LogChargementServiceImpl
 *
 * Service métier responsable de la gestion des logs d'import (chargement de fichiers).
 *
 * Un "LogChargement" représente le traitement d’un fichier (CSV/XML) :
 * - fileName, createdAt
 * - statut global : IN_PROGRESS / SUCCESS / FAILED / PARTIALLY_TRAITED
 * - compteurs : totalLines / successLines / failedLines
 * - détails ligne par ligne (LogChargementDetail)
 *
 * Ce service est utilisé par IngestionPipeline :
 * - startLog() au début du fichier
 * - addLine() pour chaque ligne traitée (succès/échec + message)
 * - finalizeLog() à la fin pour fixer le statut global
 */
@Service
@RequiredArgsConstructor
public class LogChargementServiceImpl implements LogChargementService {

    /**
     * Repository JPA pour persister LogChargement et le récupérer depuis la DB.
     */
    private final LogChargementRepository logChargementRepository;

    /**
     * Démarre un log pour un fichier.
     * On crée une entrée LogChargement en DB avec statut IN_PROGRESS.
     *
     * @param fileName nom du fichier en cours de traitement
     * @return LogChargement persisté (avec ID DB)
     */
    @Override
    @Transactional
    public LogChargement startLog(String fileName) {

        // Création du log "racine" (un log par fichier)
        LogChargement log = LogChargement.builder()
                .fileName(fileName)
                .status(LogStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .details(new ArrayList<>()) // liste vide de détails au départ
                .build();

        // Sauvegarde immédiate pour obtenir un ID (utile pour les relations details)
        return logChargementRepository.save(log);
    }

    /**
     * Ajoute un détail pour une ligne/record traité.
     *
     * Ce détail indique :
     * - numéro de ligne (ou index record)
     * - status SUCCESS ou FAILED
     * - message d'erreur éventuel
     *
     * Important :
     * - cette méthode met aussi à jour les compteurs total/success/failed
     *
     * @param log log du fichier (entité racine)
     * @param lineNumber numéro de ligne dans le fichier (ou index record XML)
     * @param status résultat de cette ligne
     * @param detailProblem message d’erreur (null si SUCCESS)
     */
    @Override
    @Transactional
    public void addLine(LogChargement log, int lineNumber, LineStatus status, String detailProblem) {

        // 1) Mise à jour des compteurs globaux
        log.incrementTotal();

        if (status == LineStatus.SUCCESS) {
            log.incrementSuccess();
        } else {
            log.incrementFailed();
        }

        // 2) Création du détail ligne par ligne
        LogChargementDetail detail = LogChargementDetail.builder()
                .lineNumber(lineNumber)
                .status(status)
                .detailProblem(detailProblem)
                .logChargement(log) // FK vers le log
                .build();

        // 3) Ajout à la collection (relation OneToMany)
        // Si cascade=ALL est actif, Hibernate persistera le detail au commit.
        log.addDetail(detail);
    }

    /**
     * Finalise un log après traitement complet du fichier.
     * Détermine le statut global final (SUCCESS/FAILED/PARTIALLY_TRAITED) selon les compteurs.
     *
     * NOTE :
     * - les paramètres totalLines/successLines/failedLines ne sont pas utilisés ici.
     * - le statut est calculé à partir de log.getSuccessLines() / log.getFailedLines()
     *
     * @param log log à finaliser
     */
    @Override
    @Transactional
    public void finalizeLog(LogChargement log, int totalLines, int successLines, int failedLines) {

        // Compteurs internes (sécurisés si null)
        int ok = (log.getSuccessLines() == null) ? 0 : log.getSuccessLines();
        int ko = (log.getFailedLines() == null) ? 0 : log.getFailedLines();

        // Détermination du statut global
        if (ok > 0 && ko == 0) {
            log.setStatus(LogStatus.SUCCESS);
        } else if (ok == 0 && ko > 0) {
            log.setStatus(LogStatus.FAILED);
        } else if (ok > 0) {
            log.setStatus(LogStatus.PARTIALLY_TRAITED);
        } else {
            log.setStatus(LogStatus.FAILED);
        }

        // Sauvegarde du statut final en DB
        logChargementRepository.save(log);
    }

    /**
     * Retourne tous les logs (liste brute).
     * Utilisé par l’API /logs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LogChargement> getAllLogs() {
        return logChargementRepository.findAll();
    }

    /**
     * Récupère un log par son ID.
     *
     * @param id identifiant DB
     * @return LogChargement trouvé
     * @throws LogChargementNotFoundException si introuvable
     */
    @Override
    @Transactional(readOnly = true)
    public LogChargement getLogById(Long id) {
        return logChargementRepository.findById(id)
                .orElseThrow(() -> new LogChargementNotFoundException("LogChargement not found with id: " + id));
    }

    /**
     * Recherche simple sur les logs en mémoire (après findAll()).
     * Filtre par :
     * - fileName (contains, insensitive)
     * - status exact
     *
     * NOTE :
     * - Pour de gros volumes, il vaut mieux faire cette recherche en SQL via repository.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LogChargement> searchLogs(String fileName, LogStatus status) {
        return logChargementRepository.findAll().stream()

                // filtre sur fileName si fourni
                .filter(log -> {
                    if (fileName == null || fileName.isBlank()) return true;
                    return log.getFileName() != null
                            && log.getFileName().toLowerCase().contains(fileName.toLowerCase());
                })

                // filtre sur status si fourni
                .filter(log -> status == null || status.equals(log.getStatus()))

                .toList();
    }
}
