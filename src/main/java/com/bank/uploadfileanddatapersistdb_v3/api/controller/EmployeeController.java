package com.bank.uploadfileanddatapersistdb_v3.api.controller;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.EmployeeDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.EmployeeMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.EmployeeService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EmployeeController
 *
 * Expose des endpoints REST (lecture seule) pour consulter les employés en base.
 *
 * Responsabilités :
 * - Recevoir la requête HTTP
 * - Appeler le service (couche métier)
 * - Convertir Entity -> DTO via le mapper
 * - Retourner une réponse JSON
 *
 * Ne doit pas :
 * - accéder directement au repository
 * - contenir de la logique métier (validation complexe, règles, etc.)
 */
@Tag(name = "Employees", description = "Employee read endpoints")
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    /**
     * Couche métier : récupère les employés depuis la base
     * (souvent via un repository dans l’implémentation du service).
     */
    private final EmployeeService employeeService;

    /**
     * Mapper : convertit Employee (entity) -> EmployeeDto (API).
     */
    private final EmployeeMapper employeeMapper;

    /**
     * GET /employee
     *
     * Retourne la liste complète des employés.
     */
    @Operation(
            summary = "Get all employees",
            description = "Returns all employees stored in the database."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employees returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EmployeeDto.class)
                    )
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EmployeeDto> getAllEmployees() {

        // 1) Appel service -> retourne une liste d'entités JPA
        List<Employee> employees = employeeService.getAll();

        // 2) Transformation Entity -> DTO pour exposer uniquement les champs API
        return employees.stream()
                .map(employeeMapper::toDto)
                .toList();
    }

    /**
     * GET /employee/{id}
     *
     * Retourne un employé par son identifiant.
     */
    @Operation(
            summary = "Get an employee by id",
            description = "Returns one employee by its id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EmployeeDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Employee not found",
                    content = @Content // pas de body, ou bien tu peux mettre un ErrorDto si tu en as un
            )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public EmployeeDto getEmployeeById(
            @Parameter(description = "Employee id", example = "1", required = true)
            @PathVariable Long id
    ) {

        // 1) Le service doit lancer une exception (ex: NotFound) si l’employé n’existe pas
        Employee employee = employeeService.getEmployeeById(id);

        // 2) Conversion en DTO
        return employeeMapper.toDto(employee);
    }
}
