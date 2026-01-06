package com.bank.uploadfileanddatapersistdb_v3.application.service;
// Gestion transactionnelle des configurations de lecture.

import com.bank.uploadfileanddatapersistdb_v3.api.dto.DuplicateCheckUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigMetaUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderMappingCsvUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderMappingXmlUpdateDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.FileReaderConfigMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.ConfigNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.FileProcessingException;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.MappingItemNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.CsvColumnEntity;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.DataFoldersEmbeddable;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderMappingCSV;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderMappingXML;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.XmlFieldEntity;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.FieldType;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.FileReaderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier responsable de la gestion des configurations de lecture (FileReaderConfig).
 *
 * Responsabilités principales :
 * - Charger une configuration (Entity) avec son "graphe" (mappings CSV/XML + collections associées)
 * - Exposer une vue DTO pour l'API (sans exposer directement l'entité JPA)
 * - Créer / Mettre à jour (UPSERT) une configuration à partir d'un DTO
 * - Supprimer une configuration
 *
 * Remarque d'architecture :
 * - Le mapping DTO <-> Entity est externalisé dans FileReaderConfigMapper.
 *   Le service reste concentré sur : transactions, orchestration, persistance.
 */
@Service
@RequiredArgsConstructor
public class FileReaderConfigServiceImpl implements FileReaderConfigService {

    // Repository JPA pour accéder à FileReaderConfig (DB).
    private final FileReaderConfigRepository repo;

    // Mapper dédié pour convertir Entity <-> DTO (sorti du service pour SRP/testabilité).
    private final FileReaderConfigMapper mapper;

    /**
     * Récupère l'entité FileReaderConfig en garantissant que les sous-graphes nécessaires
     * (CSV/XML + collections) sont initialisés.
     *
     * Pourquoi :
     * - Les relations JPA sont souvent LAZY.
     * - Si on accède à des collections après la fin de la transaction, on risque une LazyInitializationException.
     *
     * Stratégie :
     * - Utilise une requête "JOIN FETCH" (repo.findWithMappingsByIdConfigFichier)
     * - Puis "touche" certaines collections avec size() pour forcer leur initialisation dans la transaction.
     *
     * @param id identifiant fonctionnel de config (ex: "EMPLOYEES")
     * @return entité JPA totalement exploitable dans le reste du pipeline
     */

    /**
     @Override
     Indique que cette méthode implémente/redéfinit une méthode
     définie dans une interface ou une classe parente.
     Permet au compilateur de détecter les erreurs de signature
     et sécurise le refactoring.

     @Transactional
     Exécute la méthode dans une transaction Spring.
     Garantit la cohérence des données et le rollback automatique
     en cas d’exception.
     @Transactional(readOnly = true)
     Démarre une transaction en lecture seule.
     Optimise les performances et permet le chargement des relations LAZY
     sans autoriser de modification en base.
     **/

    @Override
    @Transactional(readOnly = true) // Transaction de lecture (optimisée) + safe pour le LAZY loading
    public FileReaderConfig getEntity(String id) {

        // Requête custom conçue pour charger la config + mapping CSV/XML (et certaines collections)
        FileReaderConfig cfg = repo.findWithMappingsByIdConfigFichier(id)
                .orElseThrow(() -> new ConfigNotFoundException("Config not found: " + id));

        // "Touch" défensif : force l'initialisation de collections LAZY avant la fin de la transaction.
        // Utile si certains éléments restent LAZY selon les mappings / provider / query.
        if (cfg.getFileMappingCSV() != null) {
            cfg.getFileMappingCSV().getDuplicateCheck().size();
            cfg.getFileMappingCSV().getColumns().size();
        }
        if (cfg.getFileMappingXML() != null) {
            cfg.getFileMappingXML().getDuplicateCheck().size();
            cfg.getFileMappingXML().getFields().size();
        }

        return cfg;
    }

    /**
     * Retourne la configuration sous forme de DTO (utilisé par l'API).
     * - On charge l'entité via getEntity() (graph complet),
     * - puis on convertit en DTO via FileReaderConfigMapper.
     */
    @Override
    @Transactional(readOnly = true) // Lecture seule
    public FileReaderConfigDto get(String id) {
        return mapper.toDto(getEntity(id));
    }

    /**
     * UPSERT (Update or Insert) d'une configuration.
     *
     * Règles :
     * - dto.idConfigFichier est obligatoire (clé fonctionnelle).
     * - Si l'entité existe => mise à jour.
     * - Sinon => création.
     *
     * Le mapping DTO -> Entity est délégué au mapper (updateEntityFromDto),
     * ce qui permet de garder ce service focalisé sur l'orchestration + transactions.
     *
     * @param dto configuration à créer/mettre à jour
     * @return DTO complet relu en base (sans problèmes LAZY)
     */
    @Override
    @Transactional // Transaction d'écriture : commit/rollback atomique
    public FileReaderConfigDto upsert(FileReaderConfigDto dto) {

        // Validation minimale
        if (dto == null || dto.getIdConfigFichier() == null || dto.getIdConfigFichier().isBlank()) {
            throw new IllegalArgumentException("idConfigFichier is required");
        }

        // Charger l'existant ou créer une nouvelle entité
        FileReaderConfig cfg = repo.findById(dto.getIdConfigFichier())
                .orElseGet(() -> FileReaderConfig.builder()
                        .idConfigFichier(dto.getIdConfigFichier())
                        .build());

        // Appliquer dto -> entity (paths + mappings CSV/XML + colonnes/champs + relations bidirectionnelles)
        mapper.updateEntityFromDto(dto, cfg);

        // Persister l'aggregate. Cascade/OrphanRemoval gèrent les sous-objets.
        FileReaderConfig saved = repo.save(cfg);

        // Très important :
        // On relit l'entité via getEntity() (JOIN FETCH + init collections)
        // pour retourner un DTO "complet" et éviter des Lazy issues (open-in-view souvent désactivé).
        return mapper.toDto(getEntity(saved.getIdConfigFichier()));
    }

    /**
     * Supprime une configuration par ID.
     * En fonction des mappings JPA (cascade/orphanRemoval), les sous-entités associées
     * (mappings CSV/XML, colonnes, champs) peuvent être supprimées également.
     */
    @Override
    @Transactional // Écriture
    public void delete(String id) {
        if (id == null || id.isBlank()) {
            throw new ConfigNotFoundException("Config not found: " + id);
        }
        if (!repo.existsById(id)) {
            throw new ConfigNotFoundException("Config not found: " + id);
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public FileReaderConfigDto updateMeta(String id, FileReaderConfigMetaUpdateDto update) {
        if (update == null) {
            throw new FileProcessingException("Update payload is required");
        }

        FileReaderConfig cfg = getEntity(id);

        if (update.getDescription() != null) {
            cfg.setDescription(update.getDescription());
        }
        if (update.getModeChargement() != null) {
            cfg.setModeChargement(update.getModeChargement());
        }
        if (update.getEntityClassName() != null) {
            cfg.setEntityClassName(update.getEntityClassName());
        }
        if (update.getPaths() != null) {
            DataFoldersEmbeddable paths = cfg.getPaths();
            if (paths == null) {
                paths = new DataFoldersEmbeddable();
            }

            if (update.getPaths().getBaseDir() != null) {
                paths.setBaseDir(update.getPaths().getBaseDir());
            }
            if (update.getPaths().getInDir() != null) {
                paths.setInDir(update.getPaths().getInDir());
            }
            if (update.getPaths().getTreatmentDir() != null) {
                paths.setTreatmentDir(update.getPaths().getTreatmentDir());
            }
            if (update.getPaths().getBackupDir() != null) {
                paths.setBackupDir(update.getPaths().getBackupDir());
            }
            if (update.getPaths().getFailedDir() != null) {
                paths.setFailedDir(update.getPaths().getFailedDir());
            }

            cfg.setPaths(paths);
        }

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto updateCsvSettings(String id, FileReaderMappingCsvUpdateDto update) {
        if (update == null) {
            throw new FileProcessingException("Update payload is required");
        }

        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingCSV csv = getOrCreateCsv(cfg);

        if (update.getDelimiter() != null) {
            csv.setDelimiter(update.getDelimiter());
        }
        if (update.getHasHeader() != null) {
            csv.setHasHeader(update.getHasHeader());
        }

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto addCsvDuplicateCheck(String id, DuplicateCheckUpdateDto update) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingCSV csv = getOrCreateCsv(cfg);

        java.util.List<String> fields = normalizeDuplicateFields(update);
        csv.getDuplicateCheck().addAll(fields);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto removeCsvDuplicateCheck(String id, DuplicateCheckUpdateDto update) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingCSV csv = getOrCreateCsv(cfg);

        java.util.List<String> fields = normalizeDuplicateFields(update);
        csv.getDuplicateCheck().removeAll(fields);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto addCsvColumn(String id, FileReaderConfigDto.CsvColumnDto column) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingCSV csv = getOrCreateCsv(cfg);

        if (column == null) {
            throw new FileProcessingException("CSV column payload is required");
        }
        String name = normalizeName(column.getName(), "CSV column");

        if (findCsvColumnByName(csv, name) != null) {
            throw new FileProcessingException("CSV column already exists: " + name);
        }
        if (hasCsvOrderIndexConflict(csv, column.getOrderIndex(), null)) {
            throw new FileProcessingException("CSV column orderIndex already exists: " + column.getOrderIndex());
        }

        CsvColumnEntity entity = CsvColumnEntity.builder()
                .orderIndex(column.getOrderIndex())
                .name(name)
                .header(column.getHeader())
                .type(parseFieldType(column.getType()))
                .required(column.isRequired())
                .nullable(column.isNullable())
                .pattern(column.getPattern())
                .build();

        csv.addColumn(entity);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto updateCsvColumn(String id, Long columnId, FileReaderConfigDto.CsvColumnDto column) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingCSV csv = getOrCreateCsv(cfg);

        if (columnId == null) {
            throw new MappingItemNotFoundException("CSV column not found: " + columnId);
        }

        CsvColumnEntity existing = findCsvColumnById(csv, columnId);
        if (existing == null) {
            throw new MappingItemNotFoundException("CSV column not found: " + columnId);
        }

        if (column == null) {
            throw new FileProcessingException("CSV column payload is required");
        }

        if (column.getName() != null && !column.getName().isBlank()) {
            String newName = column.getName().trim();
            CsvColumnEntity conflict = findCsvColumnByName(csv, newName);
            if (conflict != null && !columnId.equals(conflict.getId())) {
                throw new FileProcessingException("CSV column already exists: " + newName);
            }
            existing.setName(newName);
        }
        if (hasCsvOrderIndexConflict(csv, column.getOrderIndex(), columnId)) {
            throw new FileProcessingException("CSV column orderIndex already exists: " + column.getOrderIndex());
        }

        applyCsvColumnUpdate(existing, column);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto deleteCsvColumn(String id, Long columnId) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingCSV csv = getOrCreateCsv(cfg);

        if (columnId == null) {
            throw new MappingItemNotFoundException("CSV column not found: " + columnId);
        }

        boolean removed = csv.getColumns().removeIf(c -> columnId.equals(c.getId()));
        if (!removed) {
            throw new MappingItemNotFoundException("CSV column not found: " + columnId);
        }

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto updateXmlSettings(String id, FileReaderMappingXmlUpdateDto update) {
        if (update == null) {
            throw new FileProcessingException("Update payload is required");
        }

        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingXML xml = getOrCreateXml(cfg);

        if (update.getRootElement() != null) {
            xml.setRootElement(update.getRootElement());
        }
        if (update.getRecordElement() != null) {
            xml.setRecordElement(update.getRecordElement());
        }

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto addXmlDuplicateCheck(String id, DuplicateCheckUpdateDto update) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingXML xml = getOrCreateXml(cfg);

        java.util.List<String> fields = normalizeDuplicateFields(update);
        xml.getDuplicateCheck().addAll(fields);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto removeXmlDuplicateCheck(String id, DuplicateCheckUpdateDto update) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingXML xml = getOrCreateXml(cfg);

        java.util.List<String> fields = normalizeDuplicateFields(update);
        xml.getDuplicateCheck().removeAll(fields);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto addXmlField(String id, FileReaderConfigDto.XmlFieldDto field) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingXML xml = getOrCreateXml(cfg);

        if (field == null) {
            throw new FileProcessingException("XML field payload is required");
        }
        String name = normalizeName(field.getName(), "XML field");

        if (findXmlFieldByName(xml, name) != null) {
            throw new FileProcessingException("XML field already exists: " + name);
        }
        if (hasXmlOrderIndexConflict(xml, field.getOrderIndex(), null)) {
            throw new FileProcessingException("XML field orderIndex already exists: " + field.getOrderIndex());
        }

        XmlFieldEntity entity = XmlFieldEntity.builder()
                .orderIndex(field.getOrderIndex())
                .name(name)
                .tag(field.getTag())
                .type(parseFieldType(field.getType()))
                .required(field.isRequired())
                .nullable(field.isNullable())
                .pattern(field.getPattern())
                .build();

        xml.addField(entity);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto updateXmlField(String id, Long fieldId, FileReaderConfigDto.XmlFieldDto field) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingXML xml = getOrCreateXml(cfg);

        if (fieldId == null) {
            throw new MappingItemNotFoundException("XML field not found: " + fieldId);
        }

        XmlFieldEntity existing = findXmlFieldById(xml, fieldId);
        if (existing == null) {
            throw new MappingItemNotFoundException("XML field not found: " + fieldId);
        }

        if (field == null) {
            throw new FileProcessingException("XML field payload is required");
        }

        if (field.getName() != null && !field.getName().isBlank()) {
            String newName = field.getName().trim();
            XmlFieldEntity conflict = findXmlFieldByName(xml, newName);
            if (conflict != null && !fieldId.equals(conflict.getId())) {
                throw new FileProcessingException("XML field already exists: " + newName);
            }
            existing.setName(newName);
        }
        if (hasXmlOrderIndexConflict(xml, field.getOrderIndex(), fieldId)) {
            throw new FileProcessingException("XML field orderIndex already exists: " + field.getOrderIndex());
        }

        applyXmlFieldUpdate(existing, field);

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    @Override
    @Transactional
    public FileReaderConfigDto deleteXmlField(String id, Long fieldId) {
        FileReaderConfig cfg = getEntity(id);
        FileReaderMappingXML xml = getOrCreateXml(cfg);

        if (fieldId == null) {
            throw new MappingItemNotFoundException("XML field not found: " + fieldId);
        }

        boolean removed = xml.getFields().removeIf(f -> fieldId.equals(f.getId()));
        if (!removed) {
            throw new MappingItemNotFoundException("XML field not found: " + fieldId);
        }

        repo.save(cfg);
        return mapper.toDto(cfg);
    }

    private FileReaderMappingCSV getOrCreateCsv(FileReaderConfig cfg) {
        FileReaderMappingCSV csv = cfg.getFileMappingCSV();
        if (csv == null) {
            csv = new FileReaderMappingCSV();
            cfg.attachCsv(csv);
        }
        return csv;
    }

    private FileReaderMappingXML getOrCreateXml(FileReaderConfig cfg) {
        FileReaderMappingXML xml = cfg.getFileMappingXML();
        if (xml == null) {
            xml = new FileReaderMappingXML();
            cfg.attachXml(xml);
        }
        return xml;
    }

    private java.util.List<String> normalizeDuplicateFields(DuplicateCheckUpdateDto update) {
        if (update == null || update.getFields() == null || update.getFields().isEmpty()) {
            throw new FileProcessingException("duplicateCheck fields are required");
        }

        java.util.List<String> fields = new java.util.ArrayList<>();
        for (String field : update.getFields()) {
            if (field == null) {
                continue;
            }
            String trimmed = field.trim();
            if (!trimmed.isEmpty()) {
                fields.add(trimmed);
            }
        }

        if (fields.isEmpty()) {
            throw new FileProcessingException("duplicateCheck fields are required");
        }
        return fields;
    }

    private String normalizeName(String name, String label) {
        if (name == null || name.isBlank()) {
            throw new FileProcessingException(label + " name is required");
        }
        return name.trim();
    }

    private CsvColumnEntity findCsvColumnByName(FileReaderMappingCSV csv, String name) {
        for (CsvColumnEntity column : csv.getColumns()) {
            if (name.equals(column.getName())) {
                return column;
            }
        }
        return null;
    }

    private CsvColumnEntity findCsvColumnById(FileReaderMappingCSV csv, Long id) {
        if (id == null) {
            return null;
        }
        for (CsvColumnEntity column : csv.getColumns()) {
            if (id.equals(column.getId())) {
                return column;
            }
        }
        return null;
    }

    private XmlFieldEntity findXmlFieldByName(FileReaderMappingXML xml, String name) {
        for (XmlFieldEntity field : xml.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private XmlFieldEntity findXmlFieldById(FileReaderMappingXML xml, Long id) {
        if (id == null) {
            return null;
        }
        for (XmlFieldEntity field : xml.getFields()) {
            if (id.equals(field.getId())) {
                return field;
            }
        }
        return null;
    }

    private boolean hasCsvOrderIndexConflict(FileReaderMappingCSV csv, Integer orderIndex, Long excludeId) {
        if (orderIndex == null) {
            return false;
        }
        for (CsvColumnEntity column : csv.getColumns()) {
            if (orderIndex.equals(column.getOrderIndex())
                    && (excludeId == null || !excludeId.equals(column.getId()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasXmlOrderIndexConflict(FileReaderMappingXML xml, Integer orderIndex, Long excludeId) {
        if (orderIndex == null) {
            return false;
        }
        for (XmlFieldEntity field : xml.getFields()) {
            if (orderIndex.equals(field.getOrderIndex())
                    && (excludeId == null || !excludeId.equals(field.getId()))) {
                return true;
            }
        }
        return false;
    }

    private void applyCsvColumnUpdate(CsvColumnEntity target, FileReaderConfigDto.CsvColumnDto source) {
        target.setOrderIndex(source.getOrderIndex());
        target.setHeader(source.getHeader());
        target.setType(parseFieldType(source.getType()));
        target.setRequired(source.isRequired());
        target.setNullable(source.isNullable());
        target.setPattern(source.getPattern());
    }

    private void applyXmlFieldUpdate(XmlFieldEntity target, FileReaderConfigDto.XmlFieldDto source) {
        target.setOrderIndex(source.getOrderIndex());
        target.setTag(source.getTag());
        target.setType(parseFieldType(source.getType()));
        target.setRequired(source.isRequired());
        target.setNullable(source.isNullable());
        target.setPattern(source.getPattern());
    }

    private FieldType parseFieldType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return FieldType.valueOf(type.trim());
        } catch (IllegalArgumentException e) {
            throw new FileProcessingException("Invalid field type: " + type);
        }
    }
}
