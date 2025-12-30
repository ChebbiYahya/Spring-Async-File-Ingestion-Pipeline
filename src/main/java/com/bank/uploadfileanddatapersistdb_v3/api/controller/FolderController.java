package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FolderService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;

/**
 * Upload endpoint used to deposit CSV/XML files into DATA_IN.
 * Actual parsing/processing is triggered via /process endpoints.
 * Folder inspection endpoints for DATA_* directories.
 */

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping("/upload-to-in")
    public ResponseEntity<Map<String, Object>> uploadToIn(@RequestParam("files") MultipartFile[] files) {
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

            String name = (file.getOriginalFilename() == null) ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            if (!(name.endsWith(".csv") || name.endsWith(".xml"))) {
                rejected.add(file.getOriginalFilename());
                continue;
            }

            Path saved = folderService.saveToInFolder(file);
            savedAs.add(saved.getFileName().toString());
        }
        // If nothing was saved, treat it as client error (400)

        if (savedAs.isEmpty()) {
            throw new InvalidFileFormatException("No valid CSV/XML file provided (all files empty or rejected)");
        }

        // If you want to fail the whole request when any file is invalid, throw instead of "rejected".
        Map<String, Object> out = new HashMap<>();
        out.put("savedAs", savedAs);
        out.put("rejected", rejected);
        out.put("folders", folderStatus());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/status")
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
