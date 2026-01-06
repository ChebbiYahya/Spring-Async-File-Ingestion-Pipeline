package com.bank.uploadfileanddatapersistdb_v3.domain.model.entity;
// Couche domain: concepts metier, exceptions, enums et entites.

import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing the result of a single line/record:
 * - line number / record index
 * - status (SUCCESS/FAILED)
 * - optional problem detail
 */
@Entity
@Table(name = "log_chargement_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogChargementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Line number in the source file (or record index for XML). */
    @Column(name = "line_number")
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LineStatus status;

    @Column(name = "detail_problem", length = 2000)
    private String detailProblem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_chargement_id", nullable = false)
    private LogChargement logChargement;
}
