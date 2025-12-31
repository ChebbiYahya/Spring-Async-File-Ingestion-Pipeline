package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;

import java.nio.file.Path;

public interface DataFoldersProvider {
    public Path basePath(String configId);

    public Path inPath(String configId) ;

    public Path treatmentPath(String configId);

    public Path backupPath(String configId) ;

    public Path failedPath(String configId) ;
}
