package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.DuplicateCheckUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigMetaUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderMappingCsvUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderMappingXmlUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.MappingItemNotFoundException;
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

    @Operation(
            summary = "Update description, modeChargement, and paths",
            description = "Updates configuration metadata. Only provided fields are updated."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PutMapping(
            value = "/{id}/meta",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto updateMeta(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to update: description, modeChargement, and/or paths.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderConfigMetaUpdateDto.class))
            )
            @RequestBody FileReaderConfigMetaUpdateDto body
    ) {
        return service.updateMeta(id, body);
    }

    @Operation(
            summary = "Update CSV mapping settings",
            description = "Updates CSV delimiter and hasHeader."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV mapping updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PutMapping(
            value = "/{id}/csv",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto updateCsvSettings(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "CSV settings to update (delimiter, hasHeader).",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderMappingCsvUpdateDto.class))
            )
            @RequestBody FileReaderMappingCsvUpdateDto body
    ) {
        return service.updateCsvSettings(id, body);
    }

    @Operation(
            summary = "Get CSV mapping by config id",
            description = "Returns CSV mapping (delimiter, hasHeader, duplicateCheck, columns) for a config."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV mapping returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.FileReaderMappingCsvDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "CSV mapping not found", content = @Content)
    })
    @GetMapping(
            value = "/{id}/csv",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto.FileReaderMappingCsvDto getCsvMapping(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id
    ) {
        FileReaderConfigDto dto = service.get(id);
        if (dto.getFileMappingCSV() == null) {
            throw new MappingItemNotFoundException("CSV mapping not found for config: " + id);
        }
        return dto.getFileMappingCSV();
    }

    @Operation(
            summary = "Get XML mapping by config id",
            description = "Returns XML mapping (rootElement, recordElement, duplicateCheck, fields) for a config."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML mapping returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.FileReaderMappingXmlDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "XML mapping not found", content = @Content)
    })
    @GetMapping(
            value = "/{id}/xml",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto.FileReaderMappingXmlDto getXmlMapping(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id
    ) {
        FileReaderConfigDto dto = service.get(id);
        if (dto.getFileMappingXML() == null) {
            throw new MappingItemNotFoundException("XML mapping not found for config: " + id);
        }
        return dto.getFileMappingXML();
    }

    @Operation(
            summary = "Add CSV duplicateCheck fields",
            description = "Adds fields to CSV duplicateCheck."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV duplicateCheck updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PostMapping(
            value = "/{id}/csv/duplicate-check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto addCsvDuplicateCheck(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to add into CSV duplicateCheck.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DuplicateCheckUpdateDto.class))
            )
            @RequestBody DuplicateCheckUpdateDto body
    ) {
        return service.addCsvDuplicateCheck(id, body);
    }

    @Operation(
            summary = "Remove CSV duplicateCheck fields",
            description = "Removes fields from CSV duplicateCheck."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV duplicateCheck updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @DeleteMapping(
            value = "/{id}/csv/duplicate-check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto removeCsvDuplicateCheck(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to remove from CSV duplicateCheck.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DuplicateCheckUpdateDto.class))
            )
            @RequestBody DuplicateCheckUpdateDto body
    ) {
        return service.removeCsvDuplicateCheck(id, body);
    }

    @Operation(
            summary = "Add a CSV column",
            description = "Adds a new column to CSV mapping."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV column added successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PostMapping(
            value = "/{id}/csv/columns",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto addCsvColumn(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "CSV column definition.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderConfigDto.CsvColumnDto.class))
            )
            @RequestBody FileReaderConfigDto.CsvColumnDto body
    ) {
        return service.addCsvColumn(id, body);
    }

    @Operation(
            summary = "Update a CSV column",
            description = "Updates an existing CSV column by id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV column updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "CSV column not found", content = @Content)
    })
    @PutMapping(
            value = "/{id}/csv/columns/{columnId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileReaderConfigDto> updateCsvColumn(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @Parameter(description = "CSV column id", required = true)
            @PathVariable Long columnId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "CSV column definition.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderConfigDto.CsvColumnDto.class))
            )
            @RequestBody FileReaderConfigDto.CsvColumnDto body
    ) {
        FileReaderConfigDto updated = service.updateCsvColumn(id, columnId, body);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete a CSV column",
            description = "Deletes a CSV column by id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV column deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "CSV column not found", content = @Content)
    })
    @DeleteMapping(
            value = "/{id}/csv/columns/{columnId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileReaderConfigDto> deleteCsvColumn(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @Parameter(description = "CSV column id", required = true)
            @PathVariable Long columnId
    ) {
        FileReaderConfigDto updated = service.deleteCsvColumn(id, columnId);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Update XML mapping settings",
            description = "Updates XML rootElement and recordElement."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML mapping updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PutMapping(
            value = "/{id}/xml",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto updateXmlSettings(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "XML settings to update (rootElement, recordElement).",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderMappingXmlUpdateDto.class))
            )
            @RequestBody FileReaderMappingXmlUpdateDto body
    ) {
        return service.updateXmlSettings(id, body);
    }

    @Operation(
            summary = "Add XML duplicateCheck fields",
            description = "Adds fields to XML duplicateCheck."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML duplicateCheck updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PostMapping(
            value = "/{id}/xml/duplicate-check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto addXmlDuplicateCheck(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to add into XML duplicateCheck.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DuplicateCheckUpdateDto.class))
            )
            @RequestBody DuplicateCheckUpdateDto body
    ) {
        return service.addXmlDuplicateCheck(id, body);
    }

    @Operation(
            summary = "Remove XML duplicateCheck fields",
            description = "Removes fields from XML duplicateCheck."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML duplicateCheck updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @DeleteMapping(
            value = "/{id}/xml/duplicate-check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto removeXmlDuplicateCheck(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to remove from XML duplicateCheck.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DuplicateCheckUpdateDto.class))
            )
            @RequestBody DuplicateCheckUpdateDto body
    ) {
        return service.removeXmlDuplicateCheck(id, body);
    }

    @Operation(
            summary = "Add an XML field",
            description = "Adds a new field to XML mapping."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML field added successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Configuration not found", content = @Content)
    })
    @PostMapping(
            value = "/{id}/xml/fields",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileReaderConfigDto addXmlField(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "XML field definition.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderConfigDto.XmlFieldDto.class))
            )
            @RequestBody FileReaderConfigDto.XmlFieldDto body
    ) {
        return service.addXmlField(id, body);
    }

    @Operation(
            summary = "Update an XML field",
            description = "Updates an existing XML field by id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML field updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "XML field not found", content = @Content)
    })
    @PutMapping(
            value = "/{id}/xml/fields/{fieldId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileReaderConfigDto> updateXmlField(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @Parameter(description = "XML field id", required = true)
            @PathVariable Long fieldId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "XML field definition.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FileReaderConfigDto.XmlFieldDto.class))
            )
            @RequestBody FileReaderConfigDto.XmlFieldDto body
    ) {
        FileReaderConfigDto updated = service.updateXmlField(id, fieldId, body);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete an XML field",
            description = "Deletes an XML field by id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "XML field deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileReaderConfigDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "XML field not found", content = @Content)
    })
    @DeleteMapping(
            value = "/{id}/xml/fields/{fieldId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileReaderConfigDto> deleteXmlField(
            @Parameter(description = "Configuration id (example: EMPLOYEES)", example = "EMPLOYEES", required = true)
            @PathVariable String id,
            @Parameter(description = "XML field id", required = true)
            @PathVariable Long fieldId
    ) {
        FileReaderConfigDto updated = service.deleteXmlField(id, fieldId);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
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
