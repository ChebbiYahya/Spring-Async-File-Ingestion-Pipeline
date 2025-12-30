package com.bank.uploadfileanddatapersistdb_v3.api.mapper;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.EmployeeDto;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import org.springframework.stereotype.Component;

/**
 * Maps Employee <-> EmployeeDto.
 * Keeps API layer decoupled from persistence entities.
 */
@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeDto dto) {
        if (dto == null) return null;
        Employee e = new Employee();
        e.setId(dto.getId());
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());
        e.setPosition(dto.getPosition());
        e.setDepartment(dto.getDepartment());
        e.setHireDate(dto.getHireDate());
        e.setSalary(dto.getSalary());
        return e;
    }

    public EmployeeDto toDto(Employee entity) {
        if (entity == null) return null;
        EmployeeDto dto = new EmployeeDto();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setPosition(entity.getPosition());
        dto.setDepartment(entity.getDepartment());
        dto.setHireDate(entity.getHireDate());
        dto.setSalary(entity.getSalary());
        return dto;
    }
}
