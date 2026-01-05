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

/**
 * MappingRegistry
 *
 * Charge les configurations d’ingestion (CSV / XML) depuis la base de données
 * et les transforme en schémas techniques utilisables par le pipeline.
 *
 * Cette classe est le point central de traduction entre :
 * - le modèle métier persisté (FileReaderConfig)
 * - le modèle technique d’ingestion (CsvSchema / XmlSchema)
 */
@Component
@RequiredArgsConstructor
public class MappingRegistry {

    /**
     * Service applicatif permettant d'accéder aux configurations
     * stockées en base de données.
     */
    private final FileReaderConfigService configService;

    /**
     * Charge et construit le schéma CSV pour une configuration donnée.
     *
     * @param configId identifiant de configuration (ex: EMPLOYEES)
     * @return CsvSchema prêt à être utilisé par CsvRecordReader
     */
    public CsvSchema loadCsv(String configId) {

        // Récupération de la configuration complète (avec mappings chargés)
        FileReaderConfig cfg = configService.getEntity(configId);

        var m = cfg.getFileMappingCSV();
        if (m == null) {
            throw new IllegalStateException("CSV mapping missing for config: " + configId);
        }

        // Construction du schéma technique CSV
        var schema = new CsvSchema();
        schema.setDelimiter(m.getDelimiter());
        schema.setHasHeader(m.isHasHeader());

        /**
         * duplicateCheck est stocké côté DB sous forme de Set/List.
         * On le convertit en List simple pour le schéma technique.
         */
        schema.setDuplicateCheck(
                m.getDuplicateCheck() == null
                        ? List.of()
                        : new ArrayList<>(m.getDuplicateCheck())
        );

        /**
         * Conversion des entités CsvColumnEntity
         * en règles techniques CsvColumnRule
         */
        schema.setColumns(
                m.getColumns().stream().map(c -> {
                    var r = new CsvColumnRule();
                    r.setName(c.getName());
                    r.setHeader(c.getHeader());
                    r.setType(c.getType().name()); // Enum -> String
                    r.setRequired(c.isRequired());
                    r.setNullable(c.isNullable());
                    r.setPattern(c.getPattern());
                    return r;
                }).toList()
        );

        return schema;
    }

    /**
     * Charge et construit le schéma XML pour une configuration donnée.
     *
     * @param configId identifiant de configuration (ex: EMPLOYEES)
     * @return XmlSchema prêt à être utilisé par XmlRecordReader
     */
    public XmlSchema loadXml(String configId) {

        FileReaderConfig cfg = configService.getEntity(configId);

        var m = cfg.getFileMappingXML();
        if (m == null) {
            throw new IllegalStateException("XML mapping missing for config: " + configId);
        }

        var schema = new XmlSchema();
        schema.setRootElement(m.getRootElement());
        schema.setRecordElement(m.getRecordElement());

        schema.setDuplicateCheck(
                m.getDuplicateCheck() == null
                        ? List.of()
                        : new ArrayList<>(m.getDuplicateCheck())
        );

        /**
         * Conversion des entités XmlFieldEntity
         * en règles techniques XmlFieldRule
         */
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
