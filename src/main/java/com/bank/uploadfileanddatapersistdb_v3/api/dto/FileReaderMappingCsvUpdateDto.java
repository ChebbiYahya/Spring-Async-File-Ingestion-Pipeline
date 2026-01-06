package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO pour options CSV.

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
public class FileReaderMappingCsvUpdateDto {
    private String delimiter;
    private Boolean hasHeader;
}
