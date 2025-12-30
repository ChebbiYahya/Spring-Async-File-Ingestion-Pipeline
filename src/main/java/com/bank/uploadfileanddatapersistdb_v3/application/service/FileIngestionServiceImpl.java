package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileIngestionService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.InvalidFileFormatException;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.StreamProcessingException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.filesystem.PathMultipartFile;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser.CsvRecordReader;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser.RecordReader;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser.XmlRecordReader;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence.EmployeeDuplicateDbChecker;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.persistence.EmployeeRecordPersister;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.pipeline.IngestionPipeline;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.MappingRegistry;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.CsvSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.ProgressReporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * Coordinates ingestion of one CSV/XML file:
 * - loads YAML mapping
 * - builds corresponding record reader
 * - delegates to IngestionPipeline (validation, duplicates, persistence, logs)
 */
@Service
@RequiredArgsConstructor
public class FileIngestionServiceImpl implements FileIngestionService {

    private final MappingRegistry mappingRegistry;
    private final IngestionPipeline pipeline;
    private final EmployeeRecordPersister employeePersister;
    private final EmployeeDuplicateDbChecker employeeDuplicateDbChecker;

//    @Override
//    public int ingestCsv(MultipartFile file) {
//        return ingestCsv(file, "mapping/employees-csv.yml");
//    }
//
//    @Override
//    public int ingestXml(MultipartFile file) {
//        return ingestXml(file, "mapping/employees-xml.yml");
//    }
//
//    public int ingestCsv(MultipartFile file, String mappingPath) {
//        if (file == null || file.isEmpty()) {
//            throw new InvalidFileFormatException("CSV file is empty");
//        }
//
//        CsvSchema schema = mappingRegistry.loadCsv(mappingPath);
//
//        try (RecordReader rr = new CsvRecordReader(file, schema)) {
//            return pipeline.process(
//                    file.getOriginalFilename(),
//                    schema.getDuplicateCheck(),
//                    rr.iterator(),
//                    schema.getColumns(),
//                    employeePersister::persist,
//                    (record, fields) -> employeeDuplicateDbChecker.exists(record, fields)
//            );
//        } catch (Exception e) {
//            throw new StreamProcessingException("Error reading CSV file: " + e.getMessage(), e);
//        }
//    }
//
//    public int ingestXml(MultipartFile file, String mappingPath) {
//        if (file == null || file.isEmpty()) {
//            throw new InvalidFileFormatException("XML file is empty");
//        }
//
//        XmlSchema schema = mappingRegistry.loadXml(mappingPath);
//
//        try (RecordReader rr = new XmlRecordReader(file, schema)) {
//            return pipeline.process(
//                    file.getOriginalFilename(),
//                    schema.getDuplicateCheck(),
//                    rr.iterator(),
//                    schema.getFields(),
//                    employeePersister::persist,
//                    (record, fields) -> employeeDuplicateDbChecker.exists(record, fields)
//            );
//        } catch (Exception e) {
//            throw new StreamProcessingException("Error reading XML file: " + e.getMessage(), e);
//        }
//    }

    // --------- ingest from file system Path ---------
//
//    public int ingestCsvPath(Path filePath, String mappingPath) {
//        return ingestCsv(new PathMultipartFile(filePath), mappingPath);
//    }
//
//    public int ingestXmlPath(Path filePath, String mappingPath) {
//        return ingestXml(new PathMultipartFile(filePath), mappingPath);
//    }

    public int ingestCsvPathWithProgress(Path filePath, String mappingPath, ProgressReporter progressReporter) {
        CsvSchema schema = mappingRegistry.loadCsv(mappingPath);
        try (RecordReader rr = new CsvRecordReader(new PathMultipartFile(filePath), schema)) {
            return pipeline.process(
                    filePath.getFileName().toString(),
                    schema.getDuplicateCheck(),
                    rr.iterator(),
                    schema.getColumns(),
                    employeePersister::persist,
                    (record, fields) -> employeeDuplicateDbChecker.exists(record, fields),
                    progressReporter
            );
        } catch (Exception e) {
            throw new StreamProcessingException("CSV ingestion failed: " + e.getMessage(), e);
        }
    }

    public int ingestXmlPathWithProgress(Path filePath, String mappingPath, ProgressReporter progressReporter) {
        XmlSchema schema = mappingRegistry.loadXml(mappingPath);
        try (RecordReader rr = new XmlRecordReader(new PathMultipartFile(filePath), schema)) {
            return pipeline.process(
                    filePath.getFileName().toString(),
                    schema.getDuplicateCheck(),
                    rr.iterator(),
                    schema.getFields(),
                    employeePersister::persist,
                    (record, fields) -> employeeDuplicateDbChecker.exists(record, fields),
                    progressReporter
            );
        } catch (Exception e) {
            throw new StreamProcessingException("XML ingestion failed: " + e.getMessage(), e);
        }
    }
}
