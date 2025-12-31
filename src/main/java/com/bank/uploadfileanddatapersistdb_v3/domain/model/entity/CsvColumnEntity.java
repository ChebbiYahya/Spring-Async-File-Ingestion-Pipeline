package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "csv_column")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CsvColumnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer orderIndex;

    @Column(length = 50)
    private String name;

    @Column(length = 80)
    private String header;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FieldType type;

    private boolean required;
    private boolean nullable;

    @Column(length = 500)
    private String pattern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", nullable = false)
    private FileReaderMappingCSV mapping;
}