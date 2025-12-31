package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.AsyncProcessingService;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Triggers batch processing of files located in DATA_IN.
 * Provides both synchronous and asynchronous processing endpoints.
 */
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessingController {

    private final AsyncProcessingService asyncProcessingService;
    private final JobProgressService jobProgressService;



    @PostMapping("/start-async")
    public ResponseEntity<?> startAsync(
            @RequestParam(name = "configId", required = false) String configId
    ) {
        String jobId = asyncProcessingService.startJob(configId);
        asyncProcessingService.runJob(jobId, configId);
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/progress/{jobId}")
    public ResponseEntity<?> progress(@PathVariable String jobId) {
        var dto = jobProgressService.get(jobId);
        if (dto == null) return ResponseEntity.status(404).body(Map.of("error", "jobId not found"));
        return ResponseEntity.ok(dto);
    }


}
