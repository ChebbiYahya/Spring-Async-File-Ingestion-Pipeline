package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.EmployeeDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.EmployeeMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.EmployeeService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.EmployeeNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * EmployeeService implementation (DB access + mapping + transactions).
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;

    /**
     * Transactional saveAll: either all entities are saved, or none if an error occurs.
     */
    @Override
    @Transactional
    public void saveAll(List<EmployeeDto> employees) {
        List<Employee> entities = employees.stream()
                .map(mapper::toEntity)
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public List<Employee> getAll() {
        return repository.findAll();
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }
}
