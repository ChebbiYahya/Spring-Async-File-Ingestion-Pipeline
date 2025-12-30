package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementSummaryDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementWithDetailsDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.LogChargementMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.LogChargementService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * REST endpoints to list and retrieve import logs (with optional filtering).
 */
@Tag(name = "Import Logs", description = "Search and retrieve import logs")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogChargementController {

    private final LogChargementService logChargementService;
    private final LogChargementMapper logChargementMapper;

    @Operation(summary = "List import logs (optional filtering)")
    @GetMapping
    public List<LogChargementSummaryDto> getAllLogs(
            @RequestParam(name = "fileName", required = false) String fileName,
            @RequestParam(name = "status", required = false) LogStatus status
    ) {
        return logChargementService.searchLogs(fileName, status).stream()
                .sorted(Comparator.comparing(LogChargement::getCreatedAt).reversed())
                .map(logChargementMapper::toSummaryDto)
                .toList();
    }

    @Operation(summary = "Get a log by id with line details")
    @GetMapping("/{id}")
    public LogChargementWithDetailsDto getLogById(@PathVariable Long id) {
        LogChargement log = logChargementService.getLogById(id);
        return logChargementMapper.toWithDetailsDto(log);
    }
}
