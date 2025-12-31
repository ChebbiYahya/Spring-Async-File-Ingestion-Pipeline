package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.ConfigNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.*;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.FieldType;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.FileReaderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileReaderConfigServiceImpl implements FileReaderConfigService {

    private final FileReaderConfigRepository repo;

    @Override
    @Transactional(readOnly = true)
    public FileReaderConfig getEntity(String id) {
        FileReaderConfig cfg = repo.findWithMappingsByIdConfigFichier(id)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found: " + id));

        if (cfg.getFileMappingCSV() != null) {
            cfg.getFileMappingCSV().getDuplicateCheck().size();
            cfg.getFileMappingCSV().getColumns().size();   // chargement séparé OK
        }
        if (cfg.getFileMappingXML() != null) {
            cfg.getFileMappingXML().getDuplicateCheck().size();
            cfg.getFileMappingXML().getFields().size();    // chargement séparé OK
        }
        return cfg;
    }


    @Override
    @Transactional(readOnly = true)
    public FileReaderConfigDto get(String id) {
        // IMPORTANT: ne plus appeler getEntityBasic
        return toDto(getEntity(id));
    }

    @Override
    @Transactional
    public FileReaderConfigDto upsert(FileReaderConfigDto dto) {
        if (dto == null || dto.getIdConfigFichier() == null || dto.getIdConfigFichier().isBlank()) {
            throw new IllegalArgumentException("idConfigFichier is required");
        }

        FileReaderConfig cfg = repo.findById(dto.getIdConfigFichier())
                .orElseGet(() -> FileReaderConfig.builder().idConfigFichier(dto.getIdConfigFichier()).build());

        cfg.setDescription(dto.getDescription());
        cfg.setModeChargement(dto.getModeChargement());

        // paths
        if (dto.getPaths() != null) {
            cfg.setPaths(DataFoldersEmbeddable.builder()
                    .baseDir(dto.getPaths().getBaseDir())
                    .inDir(dto.getPaths().getInDir())
                    .treatmentDir(dto.getPaths().getTreatmentDir())
                    .backupDir(dto.getPaths().getBackupDir())
                    .failedDir(dto.getPaths().getFailedDir())
                    .build());
        }

        // CSV mapping
        if (dto.getFileMappingCSV() != null) {
            FileReaderMappingCSV csv = cfg.getFileMappingCSV();
            if (csv == null) csv = new FileReaderMappingCSV();

            csv.setDelimiter(dto.getFileMappingCSV().getDelimiter());
            csv.setHasHeader(dto.getFileMappingCSV().isHasHeader());

            csv.getDuplicateCheck().clear();
            if (dto.getFileMappingCSV().getDuplicateCheck() != null) {
                csv.getDuplicateCheck().addAll(dto.getFileMappingCSV().getDuplicateCheck());
            }

            csv.clearColumns();
            if (dto.getFileMappingCSV().getColumns() != null) {
                for (var c : dto.getFileMappingCSV().getColumns()) {
                    csv.addColumn(CsvColumnEntity.builder()
                            .orderIndex(c.getOrderIndex())
                            .name(c.getName())
                            .header(c.getHeader())
                            .type(FieldType.valueOf(c.getType()))
                            .required(c.isRequired())
                            .nullable(c.isNullable())
                            .pattern(c.getPattern())
                            .build());
                }
            }

            cfg.attachCsv(csv);
        }

        // XML mapping
        if (dto.getFileMappingXML() != null) {
            FileReaderMappingXML xml = cfg.getFileMappingXML();
            if (xml == null) xml = new FileReaderMappingXML();

            xml.setRootElement(dto.getFileMappingXML().getRootElement());
            xml.setRecordElement(dto.getFileMappingXML().getRecordElement());

            xml.getDuplicateCheck().clear();
            if (dto.getFileMappingXML().getDuplicateCheck() != null) {
                xml.getDuplicateCheck().addAll(dto.getFileMappingXML().getDuplicateCheck());
            }

            xml.clearFields();
            if (dto.getFileMappingXML().getFields() != null) {
                for (var f : dto.getFileMappingXML().getFields()) {
                    xml.addField(XmlFieldEntity.builder()
                            .orderIndex(f.getOrderIndex())
                            .name(f.getName())
                            .tag(f.getTag())
                            .type(FieldType.valueOf(f.getType()))
                            .required(f.isRequired())
                            .nullable(f.isNullable())
                            .pattern(f.getPattern())
                            .build());
                }
            }

            cfg.attachXml(xml);
        }

        FileReaderConfig saved = repo.save(cfg);

        // Très important: retourner une entité relue avec graph (évite Lazy si open-in-view désactivé)
        return toDto(getEntity(saved.getIdConfigFichier()));
    }

    @Override
    @Transactional
    public void delete(String id) {
        repo.deleteById(id);
    }

    // --- Mapper minimal ---
    private FileReaderConfigDto toDto(FileReaderConfig cfg) {
        var dto = FileReaderConfigDto.builder()
                .idConfigFichier(cfg.getIdConfigFichier())
                .description(cfg.getDescription())
                .modeChargement(cfg.getModeChargement())
                .build();

        if (cfg.getPaths() != null) {
            dto.setPaths(FileReaderConfigDto.PathsDto.builder()
                    .baseDir(cfg.getPaths().getBaseDir())
                    .inDir(cfg.getPaths().getInDir())
                    .treatmentDir(cfg.getPaths().getTreatmentDir())
                    .backupDir(cfg.getPaths().getBackupDir())
                    .failedDir(cfg.getPaths().getFailedDir())
                    .build());
        }

        if (cfg.getFileMappingCSV() != null) {
            var csv = cfg.getFileMappingCSV();
            dto.setFileMappingCSV(FileReaderConfigDto.FileReaderMappingCsvDto.builder()
                    .delimiter(csv.getDelimiter())
                    .hasHeader(csv.isHasHeader())
                    .duplicateCheck(csv.getDuplicateCheck() == null ? java.util.List.of()
                            : new java.util.ArrayList<>(csv.getDuplicateCheck()))
                    .columns(csv.getColumns().stream().map(c -> FileReaderConfigDto.CsvColumnDto.builder()
                            .orderIndex(c.getOrderIndex())
                            .name(c.getName())
                            .header(c.getHeader())
                            .type(c.getType().name())
                            .required(c.isRequired())
                            .nullable(c.isNullable())
                            .pattern(c.getPattern())
                            .build()).toList())
                    .build());
        }

        if (cfg.getFileMappingXML() != null) {
            var xml = cfg.getFileMappingXML();
            dto.setFileMappingXML(FileReaderConfigDto.FileReaderMappingXmlDto.builder()
                    .rootElement(xml.getRootElement())
                    .recordElement(xml.getRecordElement())
                    .duplicateCheck(xml.getDuplicateCheck() == null ? java.util.List.of()
                            : new java.util.ArrayList<>(xml.getDuplicateCheck()))
                    .fields(xml.getFields().stream().map(f -> FileReaderConfigDto.XmlFieldDto.builder()
                            .orderIndex(f.getOrderIndex())
                            .name(f.getName())
                            .tag(f.getTag())
                            .type(f.getType().name())
                            .required(f.isRequired())
                            .nullable(f.isNullable())
                            .pattern(f.getPattern())
                            .build()).toList())
                    .build());
        }

        return dto;
    }
}
