package com.bank.uploadfileanddatapersistdb_v3.api.controller;
// Controleur REST pour demarrer le traitement et suivre le resultat.

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FinalResultDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.AsyncProcessingService;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobProgressService;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ProcessingController
 *
 * Déclenche le traitement batch des fichiers déposés dans DATA_IN.
 *
 * Fonctionnement global :
 * - Le client uploade des fichiers CSV/XML dans DATA_IN (via /folders/upload-to-in).
 * - Le client appelle /process/start-async pour démarrer le traitement asynchrone.
 * - Le client appelle /process/progress/{jobId} pour suivre la progression.
 *
 * Note : le traitement asynchrone suppose que @EnableAsync est activé
 * (souvent dans une classe @Configuration).
 */
@Tag(
        name = "Processing",
        description = "Trigger batch processing of files in DATA_IN and track job progress"
)
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessingController {

    /**
     * Service qui orchestre le traitement asynchrone :
     * - move DATA_IN -> DATA_TREATMENT
     * - ingestion CSV/XML
     * - move to BACKUP/FAILED
     * - updates job progress
     */
    private final AsyncProcessingService asyncProcessingService;

    /**
     * Service in-memory (dans ton projet) qui stocke l'état des jobs
     * et permet de récupérer : percent, timeLeft, processedRecords, etc.
     */
    private final JobProgressService jobProgressService;

    /**
     * Stocke le resultat final par job (fichiers traites / echoues).
     */
    private final JobResultService jobResultService;

    /**
     * POST /process/start-async
     *
     * Démarre un job asynchrone de traitement des fichiers présents dans DATA_IN.
     *
     * @param configId identifiant de configuration.
     * @return JSON contenant jobId pour suivre la progression.
     */
    @Operation(
            summary = "Start async processing job",
            description = "Creates a new job, counts total records in DATA_IN, then starts asynchronous file processing."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Job started successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    // Swagger affichera un objet JSON: {"jobId":"..."}
                                    example = "{\"jobId\":\"8a3f1b2c-1c1e-4f0b-9c7f-3a2a1c8d9e10\"}"
                            )
                    )
            )
    })
    @PostMapping(value = "/start-async", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startAsync(
            @Parameter(
                    description = "Configuration id",
                    example = "CONFIG_ID",
                    required = true
            )
            @RequestParam(name = "configId") String configId
    ) {
        // 1) Initialise un job (création du jobId + totalRecords)
        String jobId = asyncProcessingService.startJob(configId);

        // 2) Lance le traitement asynchrone (le thread HTTP répond immédiatement)
        asyncProcessingService.runJob(jobId, configId);

        // 3) Retourne le jobId au client
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    /**
     * GET /process/progress/{jobId}
     *
     * Retourne la progression du job.
     *
     * @param jobId identifiant du job renvoyé par /start-async
     * @return JobProgressDto si trouvé, sinon 404.
     */
    @Operation(
            summary = "Get job progress",
            description = "Returns current progress information for a previously started job (percent, status, ETA...)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Progress retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    // Optionnel: tu peux mettre implementation = JobProgressDto.class si tu l'importes ici.
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"error\":\"jobId not found\"}")
                    )
            )
    })
    @GetMapping(value = "/progress/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> progress(
            @Parameter(
                    description = "Job identifier returned by /process/start-async",
                    example = "8a3f1b2c-1c1e-4f0b-9c7f-3a2a1c8d9e10",
                    required = true
            )
            @PathVariable String jobId
    ) {
        var dto = jobProgressService.get(jobId);

        // Si le jobId n'existe pas en mémoire, on renvoie 404
        if (dto == null) {
            return ResponseEntity.status(404).body(Map.of("error", "jobId not found"));
        }

        // Sinon retourne le DTO complet (status, percent, ETA, etc.)
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /process/result/{jobId}
     *
     * Retourne le resultat final (fichiers traites + fichiers echoues).
     */
    @Operation(
            summary = "Get final result",
            description = "Returns final result with treated and failed files for a job."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Final result retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"error\":\"jobId not found\"}")
                    )
            )
    })
    @GetMapping(value = "/result/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> result(
            @Parameter(
                    description = "Job identifier returned by /process/start-async",
                    example = "8a3f1b2c-1c1e-4f0b-9c7f-3a2a1c8d9e10",
                    required = true
            )
            @PathVariable String jobId
    ) {
        FinalResultDto dto = jobResultService.get(jobId);
        if (dto == null) {
            return ResponseEntity.status(404).body(Map.of("error", "jobId not found"));
        }
        return ResponseEntity.ok(dto);
    }
}
