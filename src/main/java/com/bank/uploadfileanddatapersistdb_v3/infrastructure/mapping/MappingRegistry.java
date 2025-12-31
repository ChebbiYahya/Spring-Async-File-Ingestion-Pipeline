package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.CsvSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.CsvColumnRule;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.XmlFieldRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MappingRegistry {

    private final FileReaderConfigService configService;

    public CsvSchema loadCsv(String configId) {
        FileReaderConfig cfg = configService.getEntity(configId);

        var m = cfg.getFileMappingCSV();
        if (m == null) {
            throw new IllegalStateException("CSV mapping missing for config: " + configId);
        }

        var schema = new CsvSchema();
        schema.setDelimiter(m.getDelimiter());
        schema.setHasHeader(m.isHasHeader());

        // duplicateCheck est désormais Set<String> (ou List), on le convertit en List pour le schema
        schema.setDuplicateCheck(m.getDuplicateCheck() == null ? List.of() : new ArrayList<>(m.getDuplicateCheck()));

        schema.setColumns(
                m.getColumns().stream().map(c -> {
                    var r = new CsvColumnRule();
                    r.setName(c.getName());
                    r.setHeader(c.getHeader());
                    r.setType(c.getType().name());
                    r.setRequired(c.isRequired());
                    r.setNullable(c.isNullable());
                    r.setPattern(c.getPattern());
                    return r;
                }).toList()
        );

        return schema;
    }

    public XmlSchema loadXml(String configId) {
        FileReaderConfig cfg = configService.getEntity(configId);

        var m = cfg.getFileMappingXML();
        if (m == null) {
            throw new IllegalStateException("XML mapping missing for config: " + configId);
        }

        var schema = new XmlSchema();
        schema.setRootElement(m.getRootElement());
        schema.setRecordElement(m.getRecordElement());

        schema.setDuplicateCheck(m.getDuplicateCheck() == null ? List.of() : new ArrayList<>(m.getDuplicateCheck()));

        schema.setFields(
                m.getFields().stream().map(f -> {
                    var r = new XmlFieldRule();
                    r.setName(f.getName());
                    r.setTag(f.getTag());
                    r.setType(f.getType().name());
                    r.setRequired(f.isRequired());
                    r.setNullable(f.isNullable());
                    r.setPattern(f.getPattern());
                    return r;
                }).toList()
        );

        return schema;
    }
}
