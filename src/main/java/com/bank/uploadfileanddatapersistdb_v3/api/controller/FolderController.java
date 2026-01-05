package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FolderService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.InvalidFileFormatException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;

@Tag(name = "Folders", description = "Upload files to DATA_IN and inspect DATA_* folders")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @Operation(
            summary = "Upload CSV/XML files into DATA_IN",
            description = "Accepts multiple files via multipart/form-data and saves valid CSV/XML files into DATA_IN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "No file uploaded or no valid CSV/XML files provided")
    })
    @PostMapping(
            value = "/upload-to-in",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadToIn(

            // IMPORTANT:
            // - @RequestPart => multipart/form-data (champ de formulaire)
            // - @Parameter + schema(format=binary) => Swagger UI affiche un "Choose File"
            @Parameter(
                    name = "files",
                    description = "CSV/XML files to upload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @RequestPart("files") MultipartFile[] files
    ) {

        if (files == null || files.length == 0) {
            throw new InvalidFileFormatException("No files uploaded");
        }

        List<String> savedAs = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                rejected.add("empty-file");
                continue;
            }

            String originalName = (file.getOriginalFilename() == null) ? "" : file.getOriginalFilename();
            String lower = originalName.toLowerCase(Locale.ROOT);

            if (!(lower.endsWith(".csv") || lower.endsWith(".xml"))) {
                rejected.add(originalName);
                continue;
            }

            Path saved = folderService.saveToInFolder(file);
            savedAs.add(saved.getFileName().toString());
        }

        if (savedAs.isEmpty()) {
            throw new InvalidFileFormatException("No valid CSV/XML file provided (all files empty or rejected)");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("savedAs", savedAs);
        out.put("rejected", rejected);
        out.put("folders", folderStatus());
        return ResponseEntity.ok(out);
    }

    @DeleteMapping(value = "/in/{fileName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Delete one file from DATA_IN",
            description = "Deletes a single file from DATA_IN by file name."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> deleteFromIn(
            @Parameter(description = "File name in DATA_IN", required = true)
            @PathVariable String fileName
    ) {
        boolean deleted = folderService.deleteFromIn(fileName);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> out = new HashMap<>();
        out.put("deleted", fileName);
        out.put("folders", folderStatus());
        return ResponseEntity.ok(out);
    }

    @DeleteMapping(value = "/in", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Delete all files from DATA_IN",
            description = "Deletes all regular files from DATA_IN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files deleted successfully")
    })
    public ResponseEntity<Map<String, Object>> deleteAllFromIn() {
        List<String> deleted = folderService.deleteAllFromIn();

        Map<String, Object> out = new HashMap<>();
        out.put("deleted", deleted);
        out.put("count", deleted.size());
        out.put("folders", folderStatus());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get DATA folders status",
            description = "Returns the list of files present in each DATA folder: "
                    + "DATA_IN, DATA_TREATMENT, DATA_BACKUP, and DATA_FAILED."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Folder status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                {
                                  "DATA_IN": ["employees_01.csv", "employees_02.xml"],
                                  "DATA_TREATMENT": ["employees_03.csv"],
                                  "DATA_BACKUP": ["employees_00.csv"],
                                  "DATA_FAILED": ["employees_bad.pdf"]
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Filesystem access error",
                    content = @Content
            )
    })
    public Map<String, List<String>> folderStatus() {
        folderService.ensureFoldersExist();
        Map<String, List<String>> out = new HashMap<>();
        out.put("DATA_IN", folderService.listIn());
        out.put("DATA_TREATMENT", folderService.listTreatment());
        out.put("DATA_BACKUP", folderService.listBackup());
        out.put("DATA_FAILED", folderService.listFailed());
        return out;
    }
}
