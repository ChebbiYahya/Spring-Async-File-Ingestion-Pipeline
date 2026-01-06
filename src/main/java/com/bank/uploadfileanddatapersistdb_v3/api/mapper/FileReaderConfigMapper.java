package com.bank.uploadfileanddatapersistdb_v3.api.mapper;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.*;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.FieldType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper dédié pour convertir :
 * - FileReaderConfig (Entity JPA) <-> FileReaderConfigDto (DTO API)
 *
 * Objectif :
 * - garder le Service léger (orchestration/transactions)
 * - isoler la logique de conversion (testable, réutilisable)
 */
@Component
public class FileReaderConfigMapper {

    /**
     * Convertit Entity -> DTO.
     * Le Service doit garantir que les relations nécessaires sont déjà chargées (fetch/transaction).
     */
    public FileReaderConfigDto toDto(FileReaderConfig cfg) {
        if (cfg == null) return null;

        FileReaderConfigDto dto = FileReaderConfigDto.builder()
                .idConfigFichier(cfg.getIdConfigFichier())
                .description(cfg.getDescription())
                .modeChargement(cfg.getModeChargement())
                .entityClassName(cfg.getEntityClassName())
                .build();

        // Paths (Embeddable -> DTO)
        if (cfg.getPaths() != null) {
            dto.setPaths(FileReaderConfigDto.PathsDto.builder()
                    .baseDir(cfg.getPaths().getBaseDir())
                    .inDir(cfg.getPaths().getInDir())
                    .treatmentDir(cfg.getPaths().getTreatmentDir())
                    .backupDir(cfg.getPaths().getBackupDir())
                    .failedDir(cfg.getPaths().getFailedDir())
                    .build());
        }

        // CSV mapping -> DTO
        if (cfg.getFileMappingCSV() != null) {
            FileReaderMappingCSV csv = cfg.getFileMappingCSV();

            dto.setFileMappingCSV(FileReaderConfigDto.FileReaderMappingCsvDto.builder()
                    .delimiter(csv.getDelimiter())
                    .hasHeader(csv.isHasHeader())
                    .duplicateCheck(csv.getDuplicateCheck() == null
                            ? List.of()
                            : new ArrayList<>(csv.getDuplicateCheck()))
                    .columns(csv.getColumns() == null
                            ? List.of()
                            : csv.getColumns().stream()
                            .map(c -> FileReaderConfigDto.CsvColumnDto.builder()
                                    .id(c.getId())
                                    .orderIndex(c.getOrderIndex())
                                    .name(c.getName())
                                    .header(c.getHeader())
                                    .type(c.getType() == null ? null : c.getType().name())
                                    .required(c.isRequired())
                                    .nullable(c.isNullable())
                                    .pattern(c.getPattern())
                                    .build())
                            .toList())
                    .build());
        }

        // XML mapping -> DTO
        if (cfg.getFileMappingXML() != null) {
            FileReaderMappingXML xml = cfg.getFileMappingXML();

            dto.setFileMappingXML(FileReaderConfigDto.FileReaderMappingXmlDto.builder()
                    .rootElement(xml.getRootElement())
                    .recordElement(xml.getRecordElement())
                    .duplicateCheck(xml.getDuplicateCheck() == null
                            ? List.of()
                            : new ArrayList<>(xml.getDuplicateCheck()))
                    .fields(xml.getFields() == null
                            ? List.of()
                            : xml.getFields().stream()
                            .map(f -> FileReaderConfigDto.XmlFieldDto.builder()
                                    .id(f.getId())
                                    .orderIndex(f.getOrderIndex())
                                    .name(f.getName())
                                    .tag(f.getTag())
                                    .type(f.getType() == null ? null : f.getType().name())
                                    .required(f.isRequired())
                                    .nullable(f.isNullable())
                                    .pattern(f.getPattern())
                                    .build())
                            .toList())
                    .build());
        }

        return dto;
    }

    /**
     * Applique un DTO sur une entité existante (ou nouvelle).
     * - Met à jour les champs simples
     * - Reconstruit/replace les sous-structures (paths, mappings, colonnes, champs)
     * - Gère les relations bidirectionnelles (attachCsv/attachXml, addColumn/addField)
     *
     * Note : le Service reste responsable de find/save/transaction.
     */
    public void updateEntityFromDto(FileReaderConfigDto dto, FileReaderConfig cfg) {
        if (dto == null) throw new IllegalArgumentException("dto is null");
        if (cfg == null) throw new IllegalArgumentException("cfg is null");

        cfg.setDescription(dto.getDescription());
        cfg.setModeChargement(dto.getModeChargement());
        cfg.setEntityClassName(dto.getEntityClassName());

        // Paths
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

            // duplicateCheck : replace (clear + addAll)
            csv.getDuplicateCheck().clear();
            if (dto.getFileMappingCSV().getDuplicateCheck() != null) {
                csv.getDuplicateCheck().addAll(dto.getFileMappingCSV().getDuplicateCheck());
            }

            // columns : replace (orphanRemoval)
            csv.clearColumns();
            if (dto.getFileMappingCSV().getColumns() != null) {
                for (FileReaderConfigDto.CsvColumnDto c : dto.getFileMappingCSV().getColumns()) {
                    csv.addColumn(CsvColumnEntity.builder()
                            .orderIndex(c.getOrderIndex())
                            .name(c.getName())
                            .header(c.getHeader())
                            .type(parseFieldType(c.getType()))
                            .required(c.isRequired())
                            .nullable(c.isNullable())
                            .pattern(c.getPattern())
                            .build());
                }
            }

            // relation bidirectionnelle
            cfg.attachCsv(csv);
        }

        // XML mapping
        if (dto.getFileMappingXML() != null) {
            FileReaderMappingXML xml = cfg.getFileMappingXML();
            if (xml == null) xml = new FileReaderMappingXML();

            xml.setRootElement(dto.getFileMappingXML().getRootElement());
            xml.setRecordElement(dto.getFileMappingXML().getRecordElement());

            // duplicateCheck : replace (clear + addAll)
            xml.getDuplicateCheck().clear();
            if (dto.getFileMappingXML().getDuplicateCheck() != null) {
                xml.getDuplicateCheck().addAll(dto.getFileMappingXML().getDuplicateCheck());
            }

            // fields : replace (orphanRemoval)
            xml.clearFields();
            if (dto.getFileMappingXML().getFields() != null) {
                for (FileReaderConfigDto.XmlFieldDto f : dto.getFileMappingXML().getFields()) {
                    xml.addField(XmlFieldEntity.builder()
                            .orderIndex(f.getOrderIndex())
                            .name(f.getName())
                            .tag(f.getTag())
                            .type(parseFieldType(f.getType()))
                            .required(f.isRequired())
                            .nullable(f.isNullable())
                            .pattern(f.getPattern())
                            .build());
                }
            }

            // relation bidirectionnelle
            cfg.attachXml(xml);
        }
    }

    private FieldType parseFieldType(String type) {
        if (type == null || type.isBlank()) return null;
        return FieldType.valueOf(type.trim());
    }
}
