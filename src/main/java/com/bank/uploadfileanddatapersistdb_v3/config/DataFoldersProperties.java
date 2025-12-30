package com.bank.uploadfileanddatapersistdb_v3.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Binds file-system folders configuration under prefix "data.folders".
 * This centralizes folder resolution for DATA_IN / DATA_TREATMENT / DATA_BACKUP / DATA_FAILED.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "data.folders")
public class DataFoldersProperties {

    /** Example: D:/DATA */
    private String baseDir;

    /** Folder name under baseDir */
    private String inDir;

    /** Folder name under baseDir */
    private String treatmentDir;

    /** Folder name under baseDir */
    private String backupDir;

    /** Folder name under baseDir */
    private String failedDir;

    public Path basePath() {
        return Path.of(baseDir);
    }

    public Path inPath() {
        return basePath().resolve(inDir);
    }

    public Path treatmentPath() {
        return basePath().resolve(treatmentDir);
    }

    public Path backupPath() {
        return basePath().resolve(backupDir);
    }

    public Path failedPath() {
        return basePath().resolve(failedDir);
    }
}
