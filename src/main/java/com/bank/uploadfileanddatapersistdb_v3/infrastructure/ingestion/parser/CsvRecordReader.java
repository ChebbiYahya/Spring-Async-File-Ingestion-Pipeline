package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser;

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
 * Reads a CSV file according to a YAML schema and exposes records as Map<fieldName, rawValue>.
 * Header validation is strict for required columns.
 */
public class CsvRecordReader implements RecordReader {

    private final Reader reader;
    private final CSVParser parser;
    private final CsvSchema schema;

    public CsvRecordReader(MultipartFile file, CsvSchema schema) throws Exception {
        this.schema = schema;
        this.reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);

        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder()
                .setDelimiter(schema.getDelimiter().charAt(0))
                .setTrim(true)
                .setIgnoreEmptyLines(true);

        if (schema.isHasHeader()) {
            builder.setHeader();           // first record as header
            builder.setSkipHeaderRecord(true);
        }

        this.parser = new CSVParser(this.reader, builder.build());

        if (schema.isHasHeader()) {
            validateHeaderStrict(parser.getHeaderMap());
        }
    }

    private void validateHeaderStrict(Map<String, Integer> headerMap) {
        // Ensure required headers exist
        for (CsvColumnRule c : schema.getColumns()) {
            if (c.isRequired()) {
                String h = c.getHeader();
                if (h == null || h.isBlank()) {
                    throw new SchemaValidationException("CSV mapping error: required column has no 'header' value: " + c.getName());
                }
                if (!headerMap.containsKey(h)) {
                    throw new SchemaValidationException("Required column missing in CSV header: '" + h + "'");
                }
            }
        }
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        Iterator<CSVRecord> it = parser.iterator();
        return new Iterator<>() {
            int dataLineNumber = 0; // number of data records (not counting header)

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Map<String, String> next() {
                CSVRecord record = it.next();
                dataLineNumber++;

                Map<String, String> out = new HashMap<>();
                for (CsvColumnRule c : schema.getColumns()) {
                    String raw;
                    if (schema.isHasHeader()) {
                        raw = record.isMapped(c.getHeader()) ? record.get(c.getHeader()) : null;
                    } else {
                        Integer idx = (c.getIndex() != null) ? c.getIndex() : null;
                        if (idx == null) {
                            throw new RecordValidationException(
                                    ErrorCode.MISSING_COLUMN,
                                    c.getName(),
                                    dataLineNumber,
                                    "CSV mapping needs 'index' when hasHeader=false for field: " + c.getName()
                            );
                        }
                        raw = idx < record.size() ? record.get(idx) : null;
                    }
                    out.put(c.getName(), raw);
                }
                return out;
            }
        };
    }

    @Override
    public void close() throws Exception {
        parser.close();
        reader.close();
    }
}
