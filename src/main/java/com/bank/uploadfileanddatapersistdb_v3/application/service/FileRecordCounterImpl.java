package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileRecordCounter;
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
 * FileRecordCounterImpl
 *
 * Implémentation concrète du comptage des enregistrements dans les fichiers.
 *
 * Supporte :
 * - CSV (avec ou sans header)
 * - XML (basé sur recordElement défini dans la config)
 *
 * Le comptage est fait en streaming :
 * - pas de chargement complet en mémoire
 * - compatible avec de gros fichiers
 */
@Component
@RequiredArgsConstructor
class FileRecordCounterImpl implements FileRecordCounter {

    /**
     * MappingRegistry permet de charger dynamiquement :
     * - CsvSchema
     * - XmlSchema
     * depuis la configuration stockée en base (FileReaderConfig).
     */
    private final MappingRegistry mappingRegistry;

    /**
     * Point d’entrée unique pour le comptage.
     * Délègue vers CSV ou XML selon l’extension du fichier.
     */
    @Override
    public int countRecords(Path filePath, String configId) {
        String name = filePath.getFileName().toString().toLowerCase(Locale.ROOT);

        if (name.endsWith(".csv")) {
            return countCsvRecords(filePath, configId);
        }

        if (name.endsWith(".xml")) {
            return countXmlRecords(filePath, configId);
        }

        // Fichier non supporté
        return 0;
    }

    /**
     * Compte les enregistrements dans un fichier CSV.
     *
     * Règles :
     * - ignore les lignes vides
     * - si le CSV a un header, il est soustrait du total
     */
    private int countCsvRecords(Path filePath, String configId) {
        CsvSchema schema = mappingRegistry.loadCsv(configId);

        try (Stream<String> lines = Files.lines(filePath)) {

            // Compte uniquement les lignes non vides
            long nonEmpty = lines
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .count();

            // Retirer la ligne d’en-tête si elle existe
            if (schema.isHasHeader()) {
                return (int) Math.max(0, nonEmpty - 1);
            }

            return (int) nonEmpty;

        } catch (Exception e) {
            throw new FileProcessingException(
                    "Cannot count CSV records for " + filePath.getFileName() + ": " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Compte les enregistrements dans un fichier XML.
     *
     * Règle :
     * - chaque balise <recordElement> correspond à 1 enregistrement
     *
     * Le parsing est fait avec StAX (streaming XML).
     */
    private int countXmlRecords(Path filePath, String configId) {
        XmlSchema schema = mappingRegistry.loadXml(configId);
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
            throw new FileProcessingException(
                    "Cannot count XML records for " + filePath.getFileName() + ": " + e.getMessage(),
                    e
            );
        }
    }
}
