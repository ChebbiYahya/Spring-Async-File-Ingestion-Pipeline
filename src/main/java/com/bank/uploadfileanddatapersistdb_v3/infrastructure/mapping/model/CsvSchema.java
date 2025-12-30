package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.CsvColumnRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * YAML model for CSV mapping (delimiter, hasHeader, columns, duplicateCheck).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsvSchema extends FileSchema {
    private String delimiter;
    private boolean hasHeader;
    private List<CsvColumnRule> columns;
}
