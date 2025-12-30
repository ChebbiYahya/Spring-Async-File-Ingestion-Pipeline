package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.EmployeeDto;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;

import java.util.List;

/**
 * Use-case contract for Employee operations.
 */
public interface EmployeeService {

    void saveAll(List<EmployeeDto> employees);

    List<Employee> getAll();

    Employee getEmployeeById(Long id);

    boolean existsById(Long id);
}
