package com.bank.uploadfileanddatapersistdb_v3.infrastructure.filesystem;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PathMultipartFile
 *
 * Adaptateur qui expose un fichier local (Path)
 * comme un MultipartFile Spring.
 *
 * Objectif :
 * - réutiliser le même pipeline d’ingestion
 *   pour :
 *      - upload HTTP
 *      - traitement batch sur filesystem
 *
 * Pattern utilisé :
 * - Adapter
 */
public class PathMultipartFile implements MultipartFile {

    /**
     * Chemin réel du fichier sur disque.
     */
    private final Path path;

    /**
     * Nom du fichier (équivalent à MultipartFile.getOriginalFilename()).
     */
    private final String originalFilename;

    /**
     * Type MIME du fichier (ex: text/csv).
     */
    private final String contentType;

    /**
     * Construit un MultipartFile à partir d’un Path.
     *
     * @param path fichier existant sur disque
     */
    public PathMultipartFile(Path path) {
        this.path = path;
        this.originalFilename = path.getFileName().toString();
        this.contentType = guessContentType(path);
    }

    /**
     * Tente de déterminer le content-type du fichier.
     * Utilise le système (OS) si possible, sinon fallback générique.
     */
    private String guessContentType(Path p) {
        try {
            String ct = Files.probeContentType(p);
            if (ct != null) return ct;
        } catch (Exception ignored) {
            // En cas d'erreur, on utilise un type générique
        }
        return "application/octet-stream";
    }

    /**
     * Nom logique du fichier (utilisé par Spring).
     */
    @Override
    public String getName() {
        return originalFilename;
    }

    /**
     * Nom original du fichier.
     */
    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    /**
     * Type MIME du fichier.
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Indique si le fichier est vide.
     */
    @Override
    public boolean isEmpty() {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Taille du fichier en octets.
     */
    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Retourne tout le contenu du fichier sous forme de byte[].
     *
     * ⚠️ Attention : charge tout en mémoire.
     * À éviter pour les gros fichiers.
     */
    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Retourne un InputStream pour lire le fichier en streaming.
     * C’est cette méthode qui est utilisée par ton pipeline.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * Copie le fichier vers une destination.
     */
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(path, dest.toPath());
    }
}
