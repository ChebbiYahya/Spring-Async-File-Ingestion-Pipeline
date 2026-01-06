package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CSV-specific field rule: defines either header (when hasHeader=true) or index (when hasHeader=false).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsvColumnRule extends FieldRule {
    private String header;
    private Integer index;
}
