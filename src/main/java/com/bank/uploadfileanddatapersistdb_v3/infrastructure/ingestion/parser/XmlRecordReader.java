package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser;

import com.bank.uploadfileanddatapersistdb_v3.domain.exception.SchemaValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.ErrorCode;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.RecordValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.XmlFieldRule;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.*;
import java.io.InputStream;
import java.util.*;

/**
 * Streaming XML record reader (StAX) using a YAML mapping.
 * Produces records for each <recordElement> under <rootElement>.
 */
public class XmlRecordReader implements RecordReader {

    private final XMLStreamReader reader;
    private final XmlSchema schema;

    public XmlRecordReader(MultipartFile file, XmlSchema schema) throws Exception {
        this.schema = schema;

        InputStream is = file.getInputStream();
        XMLInputFactory f = XMLInputFactory.newFactory();
        this.reader = f.createXMLStreamReader(is);

        // Move to root start element and validate
        String root = moveToFirstStartElement();
        if (!schema.getRootElement().equals(root)) {
            throw new SchemaValidationException(
                    "Root element <" + root + "> does not match expected <" + schema.getRootElement() + ">"
            );
        }
    }

    private String moveToFirstStartElement() throws XMLStreamException {
        while (reader.hasNext()) {
            int ev = reader.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                return reader.getLocalName();
            }
        }
        throw new SchemaValidationException("Invalid XML: no root element found");
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        return new Iterator<>() {

            int recordIndex = 0;
            Map<String, String> next;
            boolean prepared = false;

            @Override
            public boolean hasNext() {
                if (!prepared) {
                    next = readNextRecord();
                    prepared = true;
                }
                return next != null;
            }

            @Override
            public Map<String, String> next() {
                if (!hasNext()) throw new NoSuchElementException();
                prepared = false;
                return next;
            }

            private Map<String, String> readNextRecord() {
                try {
                    while (reader.hasNext()) {
                        int ev = reader.next();

                        if (ev == XMLStreamConstants.START_ELEMENT
                                && schema.getRecordElement().equals(reader.getLocalName())) {
                            recordIndex++;

                            Map<String, String> out = new HashMap<>();
                            Set<String> expectedTags = new HashSet<>();
                            for (XmlFieldRule r : schema.getFields()) {
                                expectedTags.add(r.getTag());
                            }

                            int depth = 1;
                            String currentTag = null;

                            while (reader.hasNext() && depth > 0) {
                                int e = reader.next();

                                if (e == XMLStreamConstants.START_ELEMENT) {
                                    depth++;
                                    String tag = reader.getLocalName();
                                    if (expectedTags.contains(tag)) currentTag = tag;

                                } else if (e == XMLStreamConstants.CHARACTERS) {
                                    if (currentTag != null) {
                                        String text = reader.getText();
                                        if (text != null) {
                                            text = text.trim();
                                            if (!text.isEmpty()) {
                                                out.putIfAbsent(tagToFieldName(currentTag), text);
                                            }
                                        }
                                    }

                                } else if (e == XMLStreamConstants.END_ELEMENT) {
                                    String tag = reader.getLocalName();
                                    if (tag.equals(currentTag)) currentTag = null;
                                    depth--;
                                }
                            }

                            // Ensure all fields exist as keys (null if missing)
                            for (XmlFieldRule r : schema.getFields()) {
                                out.putIfAbsent(r.getName(), null);
                            }
                            return out;
                        }
                    }
                    return null;

                } catch (Exception ex) {
                    throw new RecordValidationException(
                            ErrorCode.TYPE_MISMATCH,
                            "XML",
                            recordIndex,
                            "XML stream error: " + ex.getMessage()
                    );
                }
            }

            private String tagToFieldName(String tag) {
                for (XmlFieldRule r : schema.getFields()) {
                    if (tag.equals(r.getTag())) return r.getName();
                }
                return tag;
            }
        };
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
