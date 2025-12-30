package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FolderService;
import com.bank.uploadfileanddatapersistdb_v3.config.DataFoldersProperties;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-system service responsible for DATA folders:
 * - ensure folders exist
 * - list files in each folder
 * - save HTTP upload into DATA_IN
 * - move one file from DATA_IN -> DATA_TREATMENT (timestamped)
 * - move treatment file to backup/failed
 */
@Service
@RequiredArgsConstructor
class FolderServiceImpl implements FolderService {


    private final DataFoldersProperties props;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public void ensureFoldersExist() {
        try {
            Files.createDirectories(props.inPath());
            Files.createDirectories(props.treatmentPath());
            Files.createDirectories(props.backupPath());
            Files.createDirectories(props.failedPath());
        } catch (Exception e) {
            throw new FileProcessingException("Cannot create DATA folders: " + e.getMessage(), e);
        }
    }

    public List<String> listIn() {
        return listFiles(props.inPath());
    }

    public List<String> listTreatment() {
        return listFiles(props.treatmentPath());
    }

    public List<String> listBackup() {
        return listFiles(props.backupPath());
    }

    public List<String> listFailed() {
        return listFiles(props.failedPath());
    }

    private List<String> listFiles(Path dir) {
        ensureFoldersExist();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FileProcessingException("Cannot list folder: " + dir + " => " + e.getMessage(), e);
        }
    }

    /** Upload API -> save file into DATA_IN */
    public Path saveToInFolder(MultipartFile file) {
        ensureFoldersExist();
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("Uploaded file is empty");
        }
        String original = sanitize(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        Path dest = props.inPath().resolve(original);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest;
        } catch (Exception e) {
            throw new FileProcessingException("Cannot save uploaded file into DATA_IN: " + e.getMessage(), e);
        }
    }

    /**
     * Pick ONE file from DATA_IN, move to DATA_TREATMENT and rename with timestamp.
     * Strategy: pick oldest by lastModified.
     */
    public Path moveOneFromInToTreatmentWithTimestamp() {
        ensureFoldersExist();
        try (Stream<Path> s = Files.list(props.inPath())) {
            Path chosen = s.filter(Files::isRegularFile)
                    .min(Comparator.comparingLong(this::lastModifiedSafe))
                    .orElse(null);

            if (chosen == null) return null;

            String fileName = chosen.getFileName().toString();
            String renamed = appendTimestamp(fileName, LocalDateTime.now());
            Path target = props.treatmentPath().resolve(renamed);

            return Files.move(chosen, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new FileProcessingException("Cannot move file DATA_IN -> DATA_TREATMENT: " + e.getMessage(), e);
        }
    }

    /** Move from treatment to backup (success). */
    public Path moveTreatmentToBackup(Path treatmentFile) {
        ensureFoldersExist();
        if (treatmentFile == null) throw new FileProcessingException("treatmentFile is null");
        Path target = props.backupPath().resolve(treatmentFile.getFileName().toString());
        try {
            return Files.move(treatmentFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new FileProcessingException("Cannot move file DATA_TREATMENT -> DATA_BACKUP: " + e.getMessage(), e);
        }
    }

    /** Move from treatment to failed (error). */
    public Path moveTreatmentToFailed(Path treatmentFile) {
        ensureFoldersExist();
        if (treatmentFile == null) throw new FileProcessingException("treatmentFile is null");
        Path target = props.failedPath().resolve(treatmentFile.getFileName().toString());
        try {
            return Files.move(treatmentFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new FileProcessingException("Cannot move file DATA_TREATMENT -> DATA_FAILED: " + e.getMessage(), e);
        }
    }

    private long lastModifiedSafe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    private String appendTimestamp(String fileName, LocalDateTime now) {
        String ts = now.format(TS);
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            String base = fileName.substring(0, dot);
            String ext = fileName.substring(dot);
            return base + "_" + ts + ext;
        }
        return fileName + "_" + ts;
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
