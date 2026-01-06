package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;
// Couche domain: concepts metier, exceptions, enums et entites.

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "file_reader_mapping_xml")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FileReaderMappingXML {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMapping;

    @Column(length = 80)
    private String rootElement;

    @Column(length = 80)
    private String recordElement;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "file_reader_mapping_xml_duplicate",
            joinColumns = @JoinColumn(name = "mapping_id")
    )
    @Column(name = "field_name", length = 50)
    @Builder.Default
    private java.util.Set<String> duplicateCheck = new java.util.LinkedHashSet<>();

    @OneToMany(mappedBy = "mapping", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<XmlFieldEntity> fields = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false, unique = true)
    private FileReaderConfig config;

    public void addField(XmlFieldEntity f) {
        fields.add(f);
        f.setMapping(this);
    }

    public void clearFields() {
        fields.clear();
    }
}