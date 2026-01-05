package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FileReaderConfigController
 *
 * API REST pour gérer la configuration de lecture de fichiers (CSV/XML),
 * stockée en base et utilisée par la pipeline d’ingestion (MappingRegistry, parsers, etc.).
 *
 * - GET : lire une config
 * - PUT : créer ou mettre à jour une config (UPSERT)
 * - DELETE : supprimer une config
 *
 * Cette API expose des DTO (FileReaderConfigDto) et délègue au service.
 */
@Tag(
        name = "File Reader Config",
        description = "CRUD endpoints for file reader configuration (CSV/XML mapping stored in DB)"
)
@RestController
@RequestMapping("/config/file-reader")
@RequiredArgsConstructor
public class FileReaderConfigController {

    /**
     * Service applicatif : contient la logique transactionnelle (lecture/écriture DB)
     * et la conversion Entity <-> DTO (ou via mapper si vous l’avez refactorisé).
     */
    private final FileReaderConfigService service;

    /**
     * GET /config/file-reader/{id}
     *
     * Retourne la configuration complète (paths + mapping CSV + mapping XML).
     */
    @Operation(
            summary = "Get a file reader configuration by id",
            description = "Returns the configuration identified by id (paths, CSV mapping, XML mapping)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Configuration not found",
                    content = @Content
            )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FileReaderConfigDto get(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id
    ) {
        return service.get(id);
    }

    /**
     * PUT /config/file-reader/{id}
     *
     * UPSERT : crée la configuration si elle n’existe pas, sinon la met à jour.
     *
     * Important :
     * - l’ID de référence est celui dans l’URL
     * - on force body.idConfigFichier = id pour éviter toute incohérence
     */
    @Operation(
            summary = "Create or update (upsert) a file reader configuration",
            description = "Upserts the configuration. The id in URL is authoritative and will override body.idConfigFichier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration created/updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input (missing required fields, invalid types, etc.)",
                    content = @Content
            )
    })
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto upsert(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Configuration payload (paths + CSV/XML mapping). The id in URL overrides body.idConfigFichier.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderConfigDto.class))
            )
            @RequestBody FileReaderConfigDto body
    ) {
        // L’ID de l’URL est la source de vérité : on l’impose au body
        body.setIdConfigFichier(id);

        // Délégation au service pour sauvegarde transactionnelle (insert ou update)
        return service.upsert(body);
    }

    /**
     * DELETE /config/file-reader/{id}
     *
     * Supprime la configuration. Réponse standard : 204 No Content.
     */
    @Operation(
            summary = "Delete a file reader configuration by id",
            description = "Deletes the configuration identified by id."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
