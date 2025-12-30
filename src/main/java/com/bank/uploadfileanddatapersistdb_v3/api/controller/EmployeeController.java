package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.EmployeeDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.EmployeeMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.EmployeeService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints to read employees from the database.
 */
@Tag(name = "Employees", description = "Employee read endpoints")
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    @Operation(summary = "Get all employees")
    @GetMapping
    public List<EmployeeDto> getAllEmployees() {
        List<Employee> employees = employeeService.getAll();
        return employees.stream()
                .map(employeeMapper::toDto)
                .toList();
    }

    @Operation(summary = "Get an employee by id")
    @GetMapping("/{id}")
    public EmployeeDto getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        return employeeMapper.toDto(employee);
    }
}
