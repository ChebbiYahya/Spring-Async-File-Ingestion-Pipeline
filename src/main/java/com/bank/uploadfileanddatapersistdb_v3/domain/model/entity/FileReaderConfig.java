package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_reader_config")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FileReaderConfig {

    @Id
    @Column(name = "id_config_fichier", length = 50)
    private String idConfigFichier; // ex: "EMPLOYEES"

    @Column(length = 255)
    private String description;

    @Column(name = "mode_chargement", length = 30)
    private String modeChargement; // ex: "WEB"


    private DataFoldersEmbeddable paths;

    @OneToOne(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private FileReaderMappingCSV fileMappingCSV;

    @OneToOne(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private FileReaderMappingXML fileMappingXML;

    public void attachCsv(FileReaderMappingCSV csv) {
        this.fileMappingCSV = csv;
        if (csv != null) csv.setConfig(this);
    }

    public void attachXml(FileReaderMappingXML xml) {
        this.fileMappingXML = xml;
        if (xml != null) xml.setConfig(this);
    }
}