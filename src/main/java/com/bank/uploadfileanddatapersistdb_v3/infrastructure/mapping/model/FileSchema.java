package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Common base for file schemas (mainly carries duplicateCheck configuration).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileSchema {
    private List<String> duplicateCheck;
}
