package com.bank.uploadfileanddatapersistdb_v3.application.service;
// Service filesystem pour deplacer et lister les fichiers.

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.DataFoldersProvider;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FolderService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FolderServiceImpl
 *
 * Service filesystem qui gere le cycle de vie des fichiers pour une config.
 */
@Service
@RequiredArgsConstructor
class FolderServiceImpl implements FolderService {

    /**
     * Fournit les chemins vers les dossiers (base/in/treatment/backup/failed)
     * en fonction de la configuration en base (FileReaderConfig).
     */
    private final DataFoldersProvider folders;

    // Helpers pour obtenir les chemins des repertoires (resolus via DataFoldersProvider)
    private Path inPath(String configId)        { return folders.inPath(requireConfigId(configId)); }
    private Path treatmentPath(String configId) { return folders.treatmentPath(requireConfigId(configId)); }
    private Path backupPath(String configId)    { return folders.backupPath(requireConfigId(configId)); }
    private Path failedPath(String configId)    { return folders.failedPath(requireConfigId(configId)); }
    /**
     * Format du timestamp ajouté au nom de fichier lorsqu’on le déplace dans DATA_TREATMENT.
     * Exemple : employees_2026-01-02_12-05-44.csv
     */
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Crée les répertoires DATA_* si ils n’existent pas.
     * Files.createDirectories(...) :
     *  - crée le dossier et tous les parents si nécessaire
     *  - ne lève pas d’erreur si le dossier existe déjà
     */
    @Override
    public void ensureFoldersExist(String configId) {
        try {
            Files.createDirectories(inPath(configId));
            Files.createDirectories(treatmentPath(configId));
            Files.createDirectories(backupPath(configId));
            Files.createDirectories(failedPath(configId));
        } catch (Exception e) {
            // Exception métier unique pour tout ce qui touche au traitement de fichiers
            throw new FileProcessingException("Cannot create DATA folders: " + e.getMessage(), e);
        }
    }

    // Listes de fichiers (noms seulement) par dossier
    @Override public List<String> listIn(String configId)        { return listFiles(configId, inPath(configId)); }
    @Override public List<String> listTreatment(String configId) { return listFiles(configId, treatmentPath(configId)); }
    @Override public List<String> listBackup(String configId)    { return listFiles(configId, backupPath(configId)); }
    @Override public List<String> listFailed(String configId)    { return listFiles(configId, failedPath(configId)); }

    /**
     * Liste les fichiers présents dans un dossier.
     * - Ne retourne que les fichiers réguliers (pas les sous-dossiers)
     * - Retourne uniquement les noms, pas les chemins complets
     */
    private List<String> listFiles(String configId, Path dir) {
        ensureFoldersExist(configId);
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FileProcessingException("Cannot list folder: " + dir + " => " + e.getMessage(), e);
        }
    }

    /**
     * Sauvegarde un fichier uploadé via HTTP dans DATA_IN.
     *
     * Utilisée par FolderController (endpoint /folders/upload-to-in).
     *
     * - Sanitization : on remplace les caractères interdits dans un nom de fichier Windows/Linux.
     * - Si un fichier avec le même nom existe déjà dans DATA_IN,
     *   l'upload est refusé (aucun remplacement).
     *
     * @param file fichier reçu via Multipart/form-data
     * @return Path du fichier créé dans DATA_IN
     * @throws FileProcessingException si le fichier existe déjà
     */
    @Override
    public Path saveToInFolder(MultipartFile file, String configId) {
        ensureFoldersExist(configId);

        // Validation de base
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("Uploaded file is empty");
        }

        // Nom original sécurisé pour éviter des chemins invalides / injection de path
        String original = sanitize(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());

        // Destination dans DATA_IN
        Path dest = inPath(configId).resolve(original);

        // 🚫 RÈGLE MÉTIER : refuser si le fichier existe déjà
        if (Files.exists(dest)) {
            throw new FileProcessingException(
                    "File already exists in DATA_IN: " + original
            );
        }

        // Copie du flux d'upload vers le fichier de destination (sans remplacement)
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, dest); // ⚠️ pas de REPLACE_EXISTING
            return dest;
        } catch (Exception e) {
            throw new FileProcessingException(
                    "Cannot save uploaded file into DATA_IN: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Deletes a single file from DATA_IN by name.
     *
     * @param fileName file name as listed by listIn()
     * @return true if the file was deleted, false if it did not exist
     */
    @Override
    public boolean deleteFromIn(String configId, String fileName) {
        ensureFoldersExist(configId);
        Path target = resolveInFile(configId, fileName);

        try {
            return Files.deleteIfExists(target);
        } catch (Exception e) {
            throw new FileProcessingException(
                    "Cannot delete file from DATA_IN: " + fileName + " => " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Deletes all regular files from DATA_IN and returns the deleted names.
     */
    @Override
    public List<String> deleteAllFromIn(String configId) {
        ensureFoldersExist(configId);

        try (Stream<Path> s = Files.list(inPath(configId))) {
            List<Path> files = s.filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            List<String> deleted = new ArrayList<>();
            for (Path file : files) {
                try {
                    if (Files.deleteIfExists(file)) {
                        deleted.add(file.getFileName().toString());
                    }
                } catch (Exception e) {
                    throw new FileProcessingException(
                            "Cannot delete file from DATA_IN: " + file.getFileName() + " => " + e.getMessage(),
                            e
                    );
                }
            }

            deleted.sort(String::compareTo);
            return deleted;
        } catch (FileProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new FileProcessingException(
                    "Cannot delete files from DATA_IN: " + e.getMessage(),
                    e
            );
        }
    }
    /**
     * Déplace UN seul fichier de DATA_IN vers DATA_TREATMENT.
     *
     * Objectif :
     * - traiter les fichiers un par un
     * - éviter qu’un fichier reste dans DATA_IN pendant qu'il est en cours de traitement
     * - renommer le fichier avec un timestamp pour tracer le traitement et éviter collisions
     *
     * Stratégie de sélection :
     * - choisir le fichier le plus ancien (min lastModified)
     *
     * @return Path du fichier déplacé dans DATA_TREATMENT, ou null s’il n’y a aucun fichier à traiter
     */

    @Override
    public Path moveOneFromInToTreatmentWithTimestamp(String configId) {
        ensureFoldersExist(configId);

        try (Stream<Path> s = Files.list(inPath(configId))) {

            // Choisir le plus ancien fichier (par date de modification)
            Path chosen = s.filter(Files::isRegularFile)
                    .min(Comparator.comparingLong(this::lastModifiedSafe))
                    .orElse(null);

            // Aucun fichier à traiter
            if (chosen == null) return null;

            // Renommer avec timestamp
            String fileName = chosen.getFileName().toString();
            String renamed = appendTimestamp(fileName, LocalDateTime.now());

            // Nouveau chemin dans DATA_TREATMENT
            Path target = treatmentPath(configId).resolve(renamed);

            // Move (atomique si possible sur le même FS)
            return Files.move(chosen, target, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            throw new FileProcessingException("Cannot move file DATA_IN -> DATA_TREATMENT: " + e.getMessage(), e);
        }
    }

    /**
     * Déplace un fichier depuis DATA_TREATMENT vers DATA_BACKUP.
     * À utiliser après traitement réussi.
     */
    @Override
    public Path moveTreatmentToBackup(String configId, Path treatmentFile) {
        ensureFoldersExist(configId);
        if (treatmentFile == null) {
            throw new FileProcessingException("treatment File is null");
        }

        Path target = backupPath(configId).resolve(treatmentFile.getFileName().toString());

        try {
            return Files.move(treatmentFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new FileProcessingException("Cannot move file DATA_TREATMENT -> DATA_BACKUP: " + e.getMessage(), e);
        }
    }

    /**
     * Déplace un fichier depuis DATA_TREATMENT vers DATA_FAILED.
     * À utiliser si le parsing/validation/persistence a échoué.
     */
    @Override
    public Path moveTreatmentToFailed(String configId, Path treatmentFile) {
        ensureFoldersExist(configId);
        if (treatmentFile == null) {
            throw new FileProcessingException("treatment File is null");
        }

        Path target = failedPath(configId).resolve(treatmentFile.getFileName().toString());

        try {
            return Files.move(treatmentFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new FileProcessingException("Cannot move file DATA_TREATMENT -> DATA_FAILED: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère la date de modification d’un fichier en millisecondes.
     * Si erreur, renvoie Long.MAX_VALUE pour que ce fichier soit "moins prioritaire"
     * lors du min(...) (on évite de le sélectionner).
     */
    private long lastModifiedSafe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    private String requireConfigId(String configId) {
        if (configId == null || configId.isBlank()) {
            throw new FileProcessingException("configId is required");
        }
        return configId;
    }

    /**
     * Resolves a simple file name inside DATA_IN and rejects path traversal.
     */
    private Path resolveInFile(String configId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new FileProcessingException("File name is required");
        }

        Path name = Paths.get(fileName);
        String simpleName = name.getFileName().toString();
        if (name.isAbsolute()
                || name.getNameCount() != 1
                || ".".equals(simpleName)
                || "..".equals(simpleName)) {
            throw new FileProcessingException("Invalid file name: " + fileName);
        }

        return inPath(configId).resolve(simpleName);
    }

    /**
     * Ajoute un timestamp au nom de fichier avant extension.
     * Exemple : employees.csv -> employees_2026-01-02_10-15-00.csv
     *
     * Si pas d'extension, ajoute simplement à la fin.
     */
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

    /**
     * Nettoie un nom de fichier en remplaçant les caractères interdits (Windows/Linux).
     * Empêche aussi certaines injections de chemin (ex: "C:\..\..\file").
     */
    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
