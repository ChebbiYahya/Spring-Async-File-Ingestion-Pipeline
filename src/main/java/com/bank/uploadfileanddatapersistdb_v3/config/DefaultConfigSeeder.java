package com.bank.uploadfileanddatapersistdb_v3.config;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FileReaderConfigDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileReaderConfigService;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.FileReaderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DefaultConfigSeeder {

    private final FileReaderConfigRepository repo;

    @Bean
    CommandLineRunner seed(FileReaderConfigService service) {
        return args -> {
            if (repo.existsById("EMPLOYEES")) return;

            FileReaderConfigDto dto = FileReaderConfigDto.builder()
                    .idConfigFichier("EMPLOYEES")
                    .description("Employees mapping (DB managed)")
                    .modeChargement("WEB")
                    .paths(FileReaderConfigDto.PathsDto.builder()
                            .baseDir("D:/DATA")
                            .inDir("DATA_IN")
                            .treatmentDir("DATA_TREATMENT")
                            .backupDir("DATA_BACKUP")
                            .failedDir("DATA_FAILED")
                            .build())
                    .fileMappingCSV(FileReaderConfigDto.FileReaderMappingCsvDto.builder()
                            .delimiter(",")
                            .hasHeader(true)
                            .duplicateCheck(List.of("id", "firstName", "lastName"))
                            .columns(List.of(
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(1).name("id").header("id").type("LONG").required(true).nullable(false).pattern("^[0-9]+$").build(),
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(2).name("firstName").header("firstName").type("STRING").required(true).nullable(false).pattern("^[A-Za-zÀ-ÿ' -]{2,100}$").build(),
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(3).name("lastName").header("lastName").type("STRING").required(true).nullable(false).pattern("^[A-Za-zÀ-ÿ' -]{2,100}$").build(),
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(4).name("position").header("position").type("STRING").required(false).nullable(true).pattern(null).build(),
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(5).name("department").header("department").type("STRING").required(false).nullable(true).pattern(null).build(),
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(6).name("hireDate").header("hireDate").type("LOCAL_DATE").required(false).nullable(true).pattern("^\\d{4}-\\d{2}-\\d{2}$").build(),
                                    FileReaderConfigDto.CsvColumnDto.builder().orderIndex(7).name("salary").header("salary").type("DECIMAL").required(false).nullable(true).pattern("^-?\\d+(\\.\\d+)?$").build()
                            ))
                            .build())
                    .fileMappingXML(FileReaderConfigDto.FileReaderMappingXmlDto.builder()
                            .rootElement("employees")
                            .recordElement("employee")
                            .duplicateCheck(List.of("id", "firstName", "lastName"))
                            .fields(List.of(
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(1).name("id").tag("id").type("LONG").required(true).nullable(false).pattern(null).build(),
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(2).name("firstName").tag("firstName").type("STRING").required(true).nullable(false).pattern("^[A-Za-zÀ-ÿ' -]{2,100}$").build(),
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(3).name("lastName").tag("lastName").type("STRING").required(true).nullable(false).pattern("^[A-Za-zÀ-ÿ' -]{2,100}$").build(),
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(4).name("position").tag("position").type("STRING").required(false).nullable(true).pattern(null).build(),
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(5).name("department").tag("department").type("STRING").required(false).nullable(true).pattern(null).build(),
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(6).name("hireDate").tag("hireDate").type("LOCAL_DATE").required(false).nullable(true).pattern("^\\d{4}-\\d{2}-\\d{2}$").build(),
                                    FileReaderConfigDto.XmlFieldDto.builder().orderIndex(7).name("salary").tag("salary").type("DECIMAL").required(false).nullable(true).pattern("^-?\\d+(\\.\\d+)?$").build()
                            ))
                            .build())
                    .build();

            service.upsert(dto);
        };
    }
}
