package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/file-reader")
@RequiredArgsConstructor
public class FileReaderConfigController {

    private final FileReaderConfigService service;

    @GetMapping("/{id}")
    public FileReaderConfigDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public FileReaderConfigDto upsert(@PathVariable String id, @RequestBody FileReaderConfigDto body) {
        body.setIdConfigFichier(id);
        return service.upsert(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}