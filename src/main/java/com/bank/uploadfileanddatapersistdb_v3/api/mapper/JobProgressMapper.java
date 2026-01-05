package com.bank.uploadfileanddatapersistdb_v3.api.mapper;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.JobProgressDto;
import org.springframework.stereotype.Component;

/**
 * JobProgressMapper
 *
 * Mapper responsable de la construction du JobProgressDto
 * à partir des valeurs calculées par le service.
 *
 * Rôle :
 * - centraliser la création du DTO
 * - garder le service léger (pas de builder dans le service)
 * - faciliter les tests unitaires
 */
@Component
public class JobProgressMapper {

    /**
     * Construit un JobProgressDto.
     *
     * @param jobId identifiant du job
     * @param status état du job (RUNNING / FINISHED / FAILED)
     * @param totalRecords nombre total d’enregistrements
     * @param processedRecords nombre déjà traité
     * @param percent pourcentage d’avancement (0..100)
     * @param timeLeft estimation du temps restant en secondes (nullable)
     * @param totalTimeSeconds temps écoulé depuis le début du job
     * @return JobProgressDto prêt à être exposé via l’API
     */
    public JobProgressDto toDto(
            String jobId,
            String status,
            int totalRecords,
            int processedRecords,
            int percent,
            Long timeLeft,
            long totalTimeSeconds
    ) {
        return JobProgressDto.builder()
                .jobId(jobId)
                .status(status)
                .totalRecords(totalRecords)
                .processedRecords(processedRecords)
                .percent(percent)
                .timeLeft(timeLeft)
                .totalTimeSeconds(totalTimeSeconds)
                .build();
    }
}
