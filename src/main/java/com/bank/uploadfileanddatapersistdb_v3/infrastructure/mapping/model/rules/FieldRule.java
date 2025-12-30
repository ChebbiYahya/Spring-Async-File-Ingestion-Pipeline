package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic field rule used by validation:
 * - name/type/required/nullable/pattern
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldRule {
    private String name;
    private String type;
    private boolean required;
    private boolean nullable;
    private String pattern;
}
