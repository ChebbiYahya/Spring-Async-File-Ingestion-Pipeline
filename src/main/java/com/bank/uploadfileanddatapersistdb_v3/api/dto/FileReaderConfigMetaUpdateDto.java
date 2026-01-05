package com.bank.uploadfileanddatapersistdb_v3.api.dto;

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
public class FileReaderConfigMetaUpdateDto {
    private String description;
    private String modeChargement;
    private FileReaderConfigDto.PathsDto paths;
}
