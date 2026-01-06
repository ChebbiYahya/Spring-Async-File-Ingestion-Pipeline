package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository;
// Couche infrastructure: parsing, persistence, mapping, validation et filesystem.

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FileReaderConfigRepository extends JpaRepository<FileReaderConfig, String> {

    /**
     * Charge une configuration FileReaderConfig AVEC tout son graphe AVEC la requête JPQL:
     *  - mapping CSV
     *  - colonnes CSV
     *  - duplicateCheck CSV
     *  - mapping XML
     *  - champs XML
     *  - duplicateCheck XML
     *
     * Cette interface apporte UNE méthode spéciale pour charger tout le graphe nécessaire en une seule requête.
     *
     * Cette méthode évite :
     *  - LazyInitializationException
     *  - le problème N+1
     *
     * Elle est utilisée par :
     *  - FileReaderConfigServiceImpl
     *  - MappingRegistry
     *  - toute la pipeline d’ingestion
     */

    @Query("""
        select distinct c
        from FileReaderConfig c
        left join fetch c.fileMappingCSV csv
        left join fetch csv.duplicateCheck csvDup
        left join fetch c.fileMappingXML xml
        left join fetch xml.duplicateCheck xmlDup
        where c.idConfigFichier = :id
    """)
    Optional<FileReaderConfig> findWithMappingsByIdConfigFichier(@Param("id") String id);
}