package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;
// Interface pour gerer la config de lecture des fichiers.

import com.bank.uploadfileanddatapersistdb_v3.api.dto.DuplicateCheckUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigMetaUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderMappingCsvUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderMappingXmlUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;

public interface FileReaderConfigService {

    FileReaderConfig getEntity(String id);

    FileReaderConfigDto get(String id);

    FileReaderConfigDto upsert(FileReaderConfigDto dto);

    void delete(String id);

    FileReaderConfigDto updateMeta(String id, FileReaderConfigMetaUpdateDto update);

    FileReaderConfigDto updateCsvSettings(String id, FileReaderMappingCsvUpdateDto update);

    FileReaderConfigDto addCsvDuplicateCheck(String id, DuplicateCheckUpdateDto update);

    FileReaderConfigDto removeCsvDuplicateCheck(String id, DuplicateCheckUpdateDto update);

    FileReaderConfigDto addCsvColumn(String id, FileReaderConfigDto.CsvColumnDto column);

    FileReaderConfigDto updateCsvColumn(String id, Long columnId, FileReaderConfigDto.CsvColumnDto column);

    FileReaderConfigDto deleteCsvColumn(String id, Long columnId);

    FileReaderConfigDto updateXmlSettings(String id, FileReaderMappingXmlUpdateDto update);

    FileReaderConfigDto addXmlDuplicateCheck(String id, DuplicateCheckUpdateDto update);

    FileReaderConfigDto removeXmlDuplicateCheck(String id, DuplicateCheckUpdateDto update);

    FileReaderConfigDto addXmlField(String id, FileReaderConfigDto.XmlFieldDto field);

    FileReaderConfigDto updateXmlField(String id, Long fieldId, FileReaderConfigDto.XmlFieldDto field);

    FileReaderConfigDto deleteXmlField(String id, Long fieldId);
}
