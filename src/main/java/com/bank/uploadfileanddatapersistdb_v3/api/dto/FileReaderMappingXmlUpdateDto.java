package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO pour options XML.

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileReaderMappingXmlUpdateDto {
    private String rootElement;
    private String recordElement;
}
