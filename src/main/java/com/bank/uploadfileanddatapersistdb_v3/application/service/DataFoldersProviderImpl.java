package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.DataFoldersProvider;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * DataFoldersProviderImpl
 *
 * Implémentation du provider de chemins filesystem.
 *
 * Rôle :
 * - Lire la configuration FileReaderConfig depuis la base de données
 * - Construire dynamiquement les chemins :
 *     - base
 *     - DATA_IN
 *     - DATA_TREATMENT
 *     - DATA_BACKUP
 *     - DATA_FAILED
 *
 * Ce composant permet :
 * - d'éviter les chemins codés en dur
 * - de supporter plusieurs configurations (configId)
 * - de centraliser la logique de résolution des dossiers
 */
@Component
@RequiredArgsConstructor
public class DataFoldersProviderImpl implements DataFoldersProvider {

    /**
     * Service applicatif qui fournit la configuration FileReaderConfig
     * depuis la base de données (avec graphe chargé).
     */
    private final FileReaderConfigService configService;

    /**
     * Méthode utilitaire interne pour récupérer la configuration
     * correspondant à un configId donné.
     *
     * @param configId identifiant fonctionnel (ex: "EMPLOYEES")
     * @return configuration complète FileReaderConfig
     */
    private FileReaderConfig cfg(String configId) {
        return configService.getEntity(configId);
    }

    /**
     * Retourne le chemin racine (baseDir).
     *
     * Exemple :
     *   D:/DATA
     */
    @Override
    public Path basePath(String configId) {
        return Path.of(cfg(configId).getPaths().getBaseDir());
    }

    /**
     * Retourne le chemin du dossier DATA_IN.
     *
     * Exemple :
     *   D:/DATA/DATA_IN
     */
    @Override
    public Path inPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir())
                .resolve(c.getPaths().getInDir());
    }

    /**
     * Retourne le chemin du dossier DATA_TREATMENT.
     *
     * Exemple :
     *   D:/DATA/DATA_TREATMENT
     */
    @Override
    public Path treatmentPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir())
                .resolve(c.getPaths().getTreatmentDir());
    }

    /**
     * Retourne le chemin du dossier DATA_BACKUP.
     *
     * Exemple :
     *   D:/DATA/DATA_BACKUP
     */
    @Override
    public Path backupPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir())
                .resolve(c.getPaths().getBackupDir());
    }

    /**
     * Retourne le chemin du dossier DATA_FAILED.
     *
     * Exemple :
     *   D:/DATA/DATA_FAILED
     */
    @Override
    public Path failedPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir())
                .resolve(c.getPaths().getFailedDir());
    }
}
