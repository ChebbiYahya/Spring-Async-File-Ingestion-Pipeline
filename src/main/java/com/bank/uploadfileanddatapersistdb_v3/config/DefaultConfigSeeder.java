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

    // Repository utilisé uniquement pour vérifier
    // si la config existe déjà en base
    private final FileReaderConfigRepository repo;

    /**
     * CommandLineRunner :
     * - s'exécute automatiquement au démarrage de Spring Boot
     * - idéal pour initialiser des données par défaut
     */
    @Bean
    CommandLineRunner seed(FileReaderConfigService service) {

        return args -> {

            // ⚠️ Sécurité : ne rien faire si la config existe déjà
            // Cela évite d'écraser une config modifiée via API
            if (repo.existsById("EMPLOYEES")) return;

            // Construction du DTO de configuration
            FileReaderConfigDto dto = FileReaderConfigDto.builder()

                    // Identifiant fonctionnel de la config
                    .idConfigFichier("EMPLOYEES")

                    // Description libre
                    .description("Employees mapping (DB managed)")

                    // Mode de chargement (informatif)
                    .modeChargement("WEB")

                    // -----------------------------
                    // Configuration des dossiers
                    // -----------------------------
                    .paths(FileReaderConfigDto.PathsDto.builder()
                            .baseDir("D:/DATA")              // dossier racine
                            .inDir("DATA_IN")               // dépôt initial
                            .treatmentDir("DATA_TREATMENT") // en cours de traitement
                            .backupDir("DATA_BACKUP")       // succès
                            .failedDir("DATA_FAILED")       // échec
                            .build())

                    // -----------------------------
                    // Mapping CSV
                    // -----------------------------
                    .fileMappingCSV(FileReaderConfigDto.FileReaderMappingCsvDto.builder()
                            .delimiter(",")        // séparateur CSV
                            .hasHeader(true)       // première ligne = header

                            // Champs utilisés pour détecter les doublons
                            .duplicateCheck(List.of("id", "firstName", "lastName"))

                            // Définition des colonnes CSV
                            .columns(List.of(

                                    // Colonne ID
                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(1)
                                            .name("id")
                                            .header("id")
                                            .type("LONG")
                                            .required(true)
                                            .nullable(false)
                                            .pattern("^[0-9]+$")
                                            .build(),

                                    // Prénom
                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(2)
                                            .name("firstName")
                                            .header("firstName")
                                            .type("STRING")
                                            .required(true)
                                            .nullable(false)
                                            .pattern("^[A-Za-zÀ-ÿ' -]{2,100}$")
                                            .build(),

                                    // Nom
                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(3)
                                            .name("lastName")
                                            .header("lastName")
                                            .type("STRING")
                                            .required(true)
                                            .nullable(false)
                                            .pattern("^[A-Za-zÀ-ÿ' -]{2,100}$")
                                            .build(),

                                    // Champs optionnels
                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(4)
                                            .name("position")
                                            .header("position")
                                            .type("STRING")
                                            .required(false)
                                            .nullable(true)
                                            .build(),

                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(5)
                                            .name("department")
                                            .header("department")
                                            .type("STRING")
                                            .required(false)
                                            .nullable(true)
                                            .build(),

                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(6)
                                            .name("hireDate")
                                            .header("hireDate")
                                            .type("LOCAL_DATE")
                                            .required(false)
                                            .nullable(true)
                                            .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                            .build(),

                                    FileReaderConfigDto.CsvColumnDto.builder()
                                            .orderIndex(7)
                                            .name("salary")
                                            .header("salary")
                                            .type("DECIMAL")
                                            .required(false)
                                            .nullable(true)
                                            .pattern("^-?\\d+(\\.\\d+)?$")
                                            .build()
                            ))
                            .build())

                    // -----------------------------
                    // Mapping XML
                    // -----------------------------
                    .fileMappingXML(FileReaderConfigDto.FileReaderMappingXmlDto.builder()
                            .rootElement("employees")     // racine XML
                            .recordElement("employee")   // un enregistrement
                            .duplicateCheck(List.of("id", "firstName", "lastName"))

                            .fields(List.of(
                                    FileReaderConfigDto.XmlFieldDto.builder()
                                            .orderIndex(1)
                                            .name("id")
                                            .tag("id")
                                            .type("LONG")
                                            .required(true)
                                            .nullable(false)
                                            .build(),

                                    FileReaderConfigDto.XmlFieldDto.builder()
                                            .orderIndex(2)
                                            .name("firstName")
                                            .tag("firstName")
                                            .type("STRING")
                                            .required(true)
                                            .nullable(false)
                                            .pattern("^[A-Za-zÀ-ÿ' -]{2,100}$")
                                            .build(),

                                    FileReaderConfigDto.XmlFieldDto.builder()
                                            .orderIndex(3)
                                            .name("lastName")
                                            .tag("lastName")
                                            .type("STRING")
                                            .required(true)
                                            .nullable(false)
                                            .pattern("^[A-Za-zÀ-ÿ' -]{2,100}$")
                                            .build()
                            ))
                            .build())

                    .build();

            // Persistance via la couche métier
            service.upsert(dto);
        };
    }
}
