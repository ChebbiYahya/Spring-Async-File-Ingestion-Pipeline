package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementSummaryDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementWithDetailsDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.LogChargementMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.LogChargementService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * LogChargementController
 *
 * Endpoints REST pour consulter les logs d'import.
 *
 * Un log correspond au traitement d'un fichier (CSV/XML).
 * - La liste (GET /logs) retourne un résumé (sans détails ligne par ligne).
 * - Le détail (GET /logs/{id}) retourne toutes les lignes (SUCCESS/FAILED + message).
 */
@Tag(
        name = "Import Logs",
        description = "Search and retrieve import logs (summary list and line-by-line details)"
)
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogChargementController {

    /**
     * Service applicatif (use-case) de gestion des logs.
     * Il encapsule la logique métier et l'accès DB via repository.
     */
    private final LogChargementService logChargementService;

    /**
     * Mapper Entity -> DTO.
     * Permet de ne jamais exposer les entités JPA directement via l'API.
     */
    private final LogChargementMapper logChargementMapper;

    /**
     * GET /logs
     *
     * Liste les logs d'import avec filtres optionnels :
     * - fileName : filtre (contains, case-insensitive)
     * - status   : filtre par statut final (SUCCESS/FAILED/PARTIALLY_TRAITED/IN_PROGRESS)
     *
     * Retourne une liste triée par createdAt DESC (du plus récent au plus ancien).
     */
    @Operation(
            summary = "List import logs (optional filtering)",
            description = "Returns a list of import logs as summary DTOs. Optional filters: fileName (contains) and status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logs retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = LogChargementSummaryDto.class))
                    )
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LogChargementSummaryDto> getAllLogs(
            @Parameter(
                    description = "Optional filter: match logs whose fileName contains this value (case-insensitive).",
                    example = "employees"
            )
            @RequestParam(name = "fileName", required = false) String fileName,

            @Parameter(
                    description = "Optional filter: log status",
                    example = "SUCCESS"
            )
            @RequestParam(name = "status", required = false) LogStatus status
    ) {
        // 1) Recherche via service (filtrage)
        // 2) Tri par date décroissante
        // 3) Mapping vers DTO summary (moins lourd que 'with details')
        return logChargementService.searchLogs(fileName, status).stream()
                .sorted(Comparator.comparing(LogChargement::getCreatedAt).reversed())
                .map(logChargementMapper::toSummaryDto)
                .toList();
    }

    /**
     * GET /logs/{id}
     *
     * Retourne un log spécifique + toutes ses lignes de détails.
     *
     * Si l'id n'existe pas :
     * - le service lève LogChargementNotFoundException
     * - normalement traduit en HTTP 404 via @ControllerAdvice
     */
    @Operation(
            summary = "Get a log by id with line details",
            description = "Returns one import log with its line-by-line details (success/failure per record)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Log retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LogChargementWithDetailsDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Log not found",
                    content = @Content
            )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public LogChargementWithDetailsDto getLogById(
            @Parameter(description = "Log identifier", example = "1", required = true)
            @PathVariable Long id
    ) {
        // Récupération de l'entité en base (ou exception si absent)
        LogChargement log = logChargementService.getLogById(id);

        // Conversion en DTO avec détails (évite d'exposer l'entité)
        return logChargementMapper.toWithDetailsDto(log);
    }
}
