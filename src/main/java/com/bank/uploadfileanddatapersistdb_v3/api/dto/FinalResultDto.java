package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO de resultat final du job.

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Final result for a processing job.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalResultDto {

    @JsonProperty("FilesTreated")
    private List<String> filesTreated;

    @JsonProperty("FilesFailed")
    private List<FileFailedDto> filesFailed;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileFailedDto {

        @JsonProperty("Filename")
        private String filename;

        @JsonProperty("DetailProblem")
        private String detailProblem;
    }
}
