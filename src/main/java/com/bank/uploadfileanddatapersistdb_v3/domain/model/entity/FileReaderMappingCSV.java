package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;
// Couche domain: concepts metier, exceptions, enums et entites.

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "file_reader_mapping_csv")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FileReaderMappingCSV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMapping;

    @Column(length = 10)
    private String delimiter;

    @Column(name = "has_header")
    private boolean hasHeader;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "file_reader_mapping_csv_duplicate",
            joinColumns = @JoinColumn(name = "mapping_id")
    )
    @Column(name = "field_name", length = 50)
    @Builder.Default
    private java.util.Set<String> duplicateCheck = new java.util.LinkedHashSet<>();

    @OneToMany(mappedBy = "mapping", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<CsvColumnEntity> columns = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false, unique = true)
    private FileReaderConfig config;

    public void addColumn(CsvColumnEntity c) {
        columns.add(c);
        c.setMapping(this);
    }

    public void clearColumns() {
        columns.clear();
    }
}