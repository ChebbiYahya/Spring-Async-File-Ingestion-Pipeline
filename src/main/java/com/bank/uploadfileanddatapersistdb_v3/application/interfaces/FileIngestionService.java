package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * Use-case contract for ingesting files (CSV / XML).
 */
public interface FileIngestionService {



    public int ingestCsvPathWithProgress(Path filePath, String mappingPath, ProgressReporter progressReporter);

    public int ingestXmlPathWithProgress(Path filePath, String mappingPath, ProgressReporter progressReporter);
}
