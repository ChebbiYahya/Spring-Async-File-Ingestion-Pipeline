package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO pour les champs de duplication.

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicateCheckUpdateDto {
    private List<String> fields;
}
