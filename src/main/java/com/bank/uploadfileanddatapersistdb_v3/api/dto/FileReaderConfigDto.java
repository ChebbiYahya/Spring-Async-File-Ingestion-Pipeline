package com.bank.uploadfileanddatapersistdb_v3.api.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FileReaderConfigDto {
    private String idConfigFichier;
    private String description;
    private String modeChargement;
    private PathsDto paths;
    private FileReaderMappingCsvDto fileMappingCSV;
    private FileReaderMappingXmlDto fileMappingXML;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PathsDto {
        private String baseDir;
        private String inDir;
        private String treatmentDir;
        private String backupDir;
        private String failedDir;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FileReaderMappingCsvDto {
        private String delimiter;
        private boolean hasHeader;
        private List<String> duplicateCheck;
        private List<CsvColumnDto> columns;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CsvColumnDto {
        private Long id;
        private Integer orderIndex;
        private String name;
        private String header;
        private String type; // LONG/STRING/LOCAL_DATE/DECIMAL
        private boolean required;
        private boolean nullable;
        private String pattern;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FileReaderMappingXmlDto {
        private String rootElement;
        private String recordElement;
        private List<String> duplicateCheck;
        private List<XmlFieldDto> fields;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class XmlFieldDto {
        private Long id;
        private Integer orderIndex;
        private String name;
        private String tag;
        private String type;
        private boolean required;
        private boolean nullable;
        private String pattern;
    }
}
