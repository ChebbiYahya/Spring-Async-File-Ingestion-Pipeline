package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import com.bank.uploadfileanddatapersistdb_v3.domain.exception.SchemaValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.ErrorCode;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.RecordValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.CsvSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.CsvColumnRule;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CsvRecordReader
 *
 * Lit un fichier CSV en suivant un schéma (CsvSchema) issu de ta configuration (DB).
 *
 * Objectif :
 * - Exposer les données sous forme d'un flux de records (Iterator)
 * - Chaque record est transformé en Map<fieldName, rawValue>
 *
 * IMPORTANT :
 * - Ici on ne fait PAS la validation métier complète (type/regex/required/nullable),
 *   on se contente de lire et de vérifier le schéma minimal (header requis).
 * - La validation complète est faite dans IngestionPipeline via FieldValidator.
 */
public class CsvRecordReader implements RecordReader {

    /** Reader bas niveau sur le stream du fichier (UTF-8) */
    private final Reader reader;

    /** Parser Apache Commons CSV (gère delimiter, header, trim, etc.) */
    private final CSVParser parser;

    /** Schéma de lecture (delimiter, hasHeader, columns...) */
    private final CsvSchema schema;

    /**
     * Constructeur :
     * - ouvre le flux du fichier
     * - configure CSVFormat selon le schéma
     * - construit le CSVParser
     * - valide le header si le schéma indique qu'il existe
     *
     * @param file   fichier CSV reçu (upload HTTP ou PathMultipartFile)
     * @param schema schéma CSV chargé depuis la configuration
     */
    public CsvRecordReader(MultipartFile file, CsvSchema schema) throws Exception {
        this.schema = schema;

        validateFileMetadata(file);

        // On force UTF-8 pour éviter les problèmes d'encodage
        this.reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);

        // Construction du format CSV selon la config
        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder()
                // Délimiteur configuré (ex: "," ou ";")
                .setDelimiter(schema.getDelimiter().charAt(0))
                // Trim => supprime espaces avant/après les valeurs
                .setTrim(true)
                // Ignore les lignes vides
                .setIgnoreEmptyLines(true);

        // Si le CSV contient un header :
        // - setHeader() => la première ligne est interprétée comme header
        // - setSkipHeaderRecord(true) => on ne renvoie pas cette ligne comme data record
        if (schema.isHasHeader()) {
            builder.setHeader();
            builder.setSkipHeaderRecord(true);
        }

        // Création du parser (peut streamer le contenu)
        this.parser = new CSVParser(this.reader, builder.build());

        // Validation stricte des colonnes requises (si header présent)
        if (schema.isHasHeader()) {
            validateHeaderStrict(parser.getHeaderMap());
        }
    }

    /**
     * Valide que le header du fichier contient bien toutes les colonnes REQUIRED.
     *
     * @param headerMap Map<headerName, index> fournie par Apache CSV
     */
    private void validateHeaderStrict(Map<String, Integer> headerMap) {

        // Pour chaque colonne définie dans le mapping
        for (CsvColumnRule c : schema.getColumns()) {

            // On ne vérifie strictement que les colonnes "required"
            if (c.isRequired()) {
                String h = c.getHeader();

                // Si required=true mais header manquant dans la config => mapping incorrect
                if (h == null || h.isBlank()) {
                    throw new SchemaValidationException(
                            "CSV mapping error: required column has no 'header' value: " + c.getName()
                    );
                }

                // Si le header défini n'existe pas dans le fichier => schema mismatch
                if (!headerMap.containsKey(h)) {
                    throw new SchemaValidationException(
                            "Required column missing in CSV header: '" + h + "'"
                    );
                }
            }
        }
    }

    private void validateFileMetadata(MultipartFile file) throws Exception {
        String firstLine = readFirstNonEmptyLine(file);
        if (firstLine == null) {
            throw new SchemaValidationException("CSV file is empty.");
        }

        char expectedDelimiter = schema.getDelimiter().charAt(0);
        char detectedDelimiter = detectDelimiter(firstLine);
        if (detectedDelimiter != 0 && detectedDelimiter != expectedDelimiter) {
            throw new SchemaValidationException(
                    "CSV delimiter mismatch: expected '" + expectedDelimiter + "' but detected '" + detectedDelimiter + "'"
            );
        }
        if (detectedDelimiter == 0 && expectedDelimiterMissing(firstLine, expectedDelimiter) && schema.getColumns().size() > 1) {
            throw new SchemaValidationException(
                    "CSV delimiter mismatch: expected '" + expectedDelimiter + "' but none detected in file"
            );
        }

        if (schema.isHasHeader() && headerMissing(firstLine, expectedDelimiter)) {
            throw new SchemaValidationException(
                    "CSV header expected (hasHeader=true) but not found in file"
            );
        }
    }

    private String readFirstNonEmptyLine(MultipartFile file) throws Exception {
        try (var in = file.getInputStream();
             var r = new InputStreamReader(in, StandardCharsets.UTF_8);
             var br = new java.io.BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) {
                    return line;
                }
            }
            return null;
        }
    }

    private char detectDelimiter(String line) {
        char[] candidates = new char[]{',', ';', '\t', '|'};
        int bestCount = 0;
        char best = 0;
        for (char c : candidates) {
            int count = countOccurrences(line, c);
            if (count > bestCount) {
                bestCount = count;
                best = c;
            }
        }
        return bestCount > 0 ? best : 0;
    }

    private int countOccurrences(String line, char c) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == c) count++;
        }
        return count;
    }

    private boolean expectedDelimiterMissing(String line, char expectedDelimiter) {
        return line.indexOf(expectedDelimiter) < 0;
    }

    private boolean headerMissing(String firstLine, char expectedDelimiter) throws Exception {
        List<String> expectedHeaders = new ArrayList<>();
        for (CsvColumnRule c : schema.getColumns()) {
            String h = c.getHeader();
            if (h != null && !h.isBlank()) expectedHeaders.add(h);
        }
        if (expectedHeaders.isEmpty()) {
            return false;
        }

        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setDelimiter(expectedDelimiter)
                .setTrim(true)
                .build();
        try (CSVParser headerParser = CSVParser.parse(firstLine, fmt)) {
            List<CSVRecord> records = headerParser.getRecords();
            if (records.isEmpty()) {
                return true;
            }

            CSVRecord header = records.get(0);
            for (String expected : expectedHeaders) {
                for (String actual : header) {
                    if (expected.equals(actual)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Retourne un Iterator<Map<String,String>> (record streaming).
     *
     * Chaque appel à next() :
     * - lit la ligne courante du CSV
     * - construit une Map où :
     *      key   = nom logique du champ (c.getName())
     *      value = valeur brute lue (String) depuis header ou index
     */
    @Override
    public Iterator<Map<String, String>> iterator() {

        // Iterator natif Apache CSV
        Iterator<CSVRecord> it = parser.iterator();

        // On wrappe l'iterator pour produire Map<String,String>
        return new Iterator<>() {

            // Numéro de ligne logique pour les records (hors header)
            int dataLineNumber = 0;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Map<String, String> next() {
                CSVRecord record = it.next();
                dataLineNumber++;

                Map<String, String> out = new HashMap<>();

                // Pour chaque champ défini dans le mapping
                for (CsvColumnRule c : schema.getColumns()) {
                    String raw;

                    if (schema.isHasHeader()) {
                        // Lecture par header
                        // record.isMapped(header) => vérifie que la colonne existe dans le CSV
                        raw = record.isMapped(c.getHeader()) ? record.get(c.getHeader()) : null;

                    } else {
                        // Lecture par index (si le fichier n'a pas de header)
                        // Dans ce cas, le mapping DOIT définir index pour chaque champ.
                        Integer idx = (c.getIndex() != null) ? c.getIndex() : null;

                        // Si index manquant => impossible de lire correctement
                        if (idx == null) {
                            throw new RecordValidationException(
                                    ErrorCode.MISSING_COLUMN,
                                    c.getName(),
                                    dataLineNumber,
                                    "CSV mapping needs 'index' when hasHeader=false for field: " + c.getName()
                            );
                        }

                        // Lecture sécurisée : si idx > taille du record => null
                        raw = idx < record.size() ? record.get(idx) : null;
                    }

                    // On stocke toujours par nom logique du champ (pas header)
                    out.put(c.getName(), raw);
                }

                return out;
            }
        };
    }

    /**
     * Libère les ressources.
     * Utilisé automatiquement via try-with-resources.
     */
    @Override
    public void close() throws Exception {
        parser.close();
        reader.close();
    }
}
