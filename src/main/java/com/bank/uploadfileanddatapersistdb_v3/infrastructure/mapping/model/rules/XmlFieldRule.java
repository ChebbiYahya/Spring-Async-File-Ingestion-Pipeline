package com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * XML-specific field rule: defines XML tag associated with a field.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class XmlFieldRule extends FieldRule {
    private String tag;
}
