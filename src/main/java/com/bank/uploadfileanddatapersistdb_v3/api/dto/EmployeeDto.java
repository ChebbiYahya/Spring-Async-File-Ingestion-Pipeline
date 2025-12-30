package com.bank.uploadfileanddatapersistdb_v3.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API DTO representing an Employee without exposing JPA entity directly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String position;
    private String department;
    private LocalDate hireDate;
    private BigDecimal salary;
}
