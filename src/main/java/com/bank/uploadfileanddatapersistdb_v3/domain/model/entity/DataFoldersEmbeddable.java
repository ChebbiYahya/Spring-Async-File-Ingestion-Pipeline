package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFoldersEmbeddable {
    private String baseDir;
    private String inDir;
    private String treatmentDir;
    private String backupDir;
    private String failedDir;
}