package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.XmlFieldRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * YAML model for XML mapping (rootElement, recordElement, fields, duplicateCheck).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class XmlSchema extends FileSchema {
    private String rootElement;
    private String recordElement;
    private List<XmlFieldRule> fields;
}
