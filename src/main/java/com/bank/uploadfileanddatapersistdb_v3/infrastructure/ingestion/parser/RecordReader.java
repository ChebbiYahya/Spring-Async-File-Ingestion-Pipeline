package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.parser;

import java.util.Iterator;
import java.util.Map;

/**
 * Generic record reader (CSV/XML/...) compatible with try-with-resources.
 * Produces an Iterator of maps (fieldName -> raw string value).
 */
public interface RecordReader extends AutoCloseable {

    Iterator<Map<String, String>> iterator();
}
