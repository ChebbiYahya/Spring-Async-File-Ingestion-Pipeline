package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.CsvSchema;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.XmlSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Loads YAML mapping files from the classpath and deserializes them into schema objects.
 */
@Component
public class MappingRegistry {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public CsvSchema loadCsv(String resourcePath) {
        return read(resourcePath, CsvSchema.class);
    }

    public XmlSchema loadXml(String resourcePath) {
        return read(resourcePath, XmlSchema.class);
    }

    private <T> T read(String resourcePath, Class<T> clazz) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Mapping not found in resources: " + resourcePath);
            }
            return mapper.readValue(is, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load mapping " + resourcePath + ": " + e.getMessage(), e);
        }
    }
}
