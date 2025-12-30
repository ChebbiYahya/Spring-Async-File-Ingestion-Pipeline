package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.domain.exception.FileProcessingException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.MappingRegistry;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.CsvSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Counts the number of records in CSV/XML files so we can compute accurate percent and ETA.
 * This is a streaming count (no full load in memory).
 */
@Component
@RequiredArgsConstructor
public class FileRecordCounter {

    private final MappingRegistry mappingRegistry;

    public int countRecords(Path filePath, String mappingCsv, String mappingXml) {
        String name = filePath.getFileName().toString().toLowerCase(Locale.ROOT);

        if (name.endsWith(".csv")) {
            String mp = (mappingCsv == null || mappingCsv.isBlank()) ? "mapping/employees-csv.yml" : mappingCsv;
            return countCsvRecords(filePath, mp);
        }
        if (name.endsWith(".xml")) {
            String mp = (mappingXml == null || mappingXml.isBlank()) ? "mapping/employees-xml.yml" : mappingXml;
            return countXmlRecords(filePath, mp);
        }
        return 0;
    }

    private int countCsvRecords(Path filePath, String mappingPath) {
        CsvSchema schema = mappingRegistry.loadCsv(mappingPath);
        try (Stream<String> lines = Files.lines(filePath)) {
            // Mimic "ignore empty lines".
            long nonEmpty = lines.filter(s -> s != null && !s.trim().isEmpty()).count();
            if (schema.isHasHeader()) {
                return (int) Math.max(0, nonEmpty - 1);
            }
            return (int) nonEmpty;
        } catch (Exception e) {
            throw new FileProcessingException("Cannot count CSV records for " + filePath.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private int countXmlRecords(Path filePath, String mappingPath) {
        XmlSchema schema = mappingRegistry.loadXml(mappingPath);
        XMLInputFactory factory = XMLInputFactory.newFactory();
        int count = 0;

        try (InputStream is = Files.newInputStream(filePath)) {
            XMLStreamReader r = factory.createXMLStreamReader(is);
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    if (schema.getRecordElement().equals(r.getLocalName())) {
                        count++;
                    }
                }
            }
            r.close();
            return count;

        } catch (Exception e) {
            throw new FileProcessingException("Cannot count XML records for " + filePath.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
