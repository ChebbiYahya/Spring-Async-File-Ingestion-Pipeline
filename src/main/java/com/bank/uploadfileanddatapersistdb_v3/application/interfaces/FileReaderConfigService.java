package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;

public interface FileReaderConfigService {

    FileReaderConfig getEntity(String id);

    FileReaderConfigDto get(String id);

    FileReaderConfigDto upsert(FileReaderConfigDto dto);

    void delete(String id);
}
