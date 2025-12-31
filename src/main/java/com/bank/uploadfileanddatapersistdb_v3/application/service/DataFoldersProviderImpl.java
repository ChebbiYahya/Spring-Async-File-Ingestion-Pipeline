package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.DataFoldersProvider;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class DataFoldersProviderImpl implements DataFoldersProvider {

    private final FileReaderConfigService configService;

    private FileReaderConfig cfg(String configId) {
        return configService.getEntity(configId);
    }

    @Override
    public Path basePath(String configId) {
        return Path.of(cfg(configId).getPaths().getBaseDir());
    }

    @Override
    public Path inPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir()).resolve(c.getPaths().getInDir());
    }

    @Override
    public Path treatmentPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir()).resolve(c.getPaths().getTreatmentDir());
    }

    @Override
    public Path backupPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir()).resolve(c.getPaths().getBackupDir());
    }

    @Override
    public Path failedPath(String configId) {
        FileReaderConfig c = cfg(configId);
        return Path.of(c.getPaths().getBaseDir()).resolve(c.getPaths().getFailedDir());
    }
}
