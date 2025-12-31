package com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.FileReaderConfig;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FileReaderConfigRepository extends JpaRepository<FileReaderConfig, String> {

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