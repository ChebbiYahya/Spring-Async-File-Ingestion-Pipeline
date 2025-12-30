package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing an import log:
 * - file name
 * - status
 * - counters
 * - creation time
 * - line-by-line details
 */
@Entity
@Table(name = "log_chargement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogChargement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LogStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "total_lines")
    private Integer totalLines;

    @Column(name = "success_lines")
    private Integer successLines;

    @Column(name = "failed_lines")
    private Integer failedLines;

    @OneToMany(mappedBy = "logChargement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LogChargementDetail> details = new ArrayList<>();

    public void addDetail(LogChargementDetail detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        detail.setLogChargement(this);
        this.details.add(detail);
    }

    public void incrementTotal() {
        if (this.totalLines == null) this.totalLines = 0;
        this.totalLines++;
    }

    public void incrementSuccess() {
        if (this.successLines == null) this.successLines = 0;
        this.successLines++;
    }

    public void incrementFailed() {
        if (this.failedLines == null) this.failedLines = 0;
        this.failedLines++;
    }
}
