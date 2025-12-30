package com.bank.uploadfileanddatapersistdb_v3.infrastructure.filesystem;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Adapter that exposes a local file (Path) as a Spring MultipartFile.
 * This allows reusing the same ingestion pipeline for both HTTP uploads and batch filesystem processing.
 */
public class PathMultipartFile implements MultipartFile {

    private final Path path;
    private final String originalFilename;
    private final String contentType;

    public PathMultipartFile(Path path) {
        this.path = path;
        this.originalFilename = path.getFileName().toString();
        this.contentType = guessContentType(path);
    }

    private String guessContentType(Path p) {
        try {
            String ct = Files.probeContentType(p);
            if (ct != null) return ct;
        } catch (Exception ignored) {
        }
        return "application/octet-stream";
    }

    @Override
    public String getName() {
        return originalFilename;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(path, dest.toPath());
    }
}
