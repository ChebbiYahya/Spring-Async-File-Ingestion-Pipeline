package com.bank.uploadfileanddatapersistdb_v3.api.exception;

import com.bank.uploadfileanddatapersistdb_v3.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central exception-to-HTTP mapping for the entire application.
 * Ensures consistent JSON responses for errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<ApiError> handleInvalidFileFormat(InvalidFileFormatException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid file format",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ApiError> handleFileProcessing(FileProcessingException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "File processing error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiError> handleEmployeeNotFound(EmployeeNotFoundException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Employee not found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(MissingRequiredFieldException.class)
    public ResponseEntity<ApiError> handleMissingRequiredField(MissingRequiredFieldException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Missing required field",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(TypeMismatchException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Format / type mismatch",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<ApiError> handleSchemaValidation(SchemaValidationException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Schema validation error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(StreamProcessingException.class)
    public ResponseEntity<ApiError> handleStreamProcessing(StreamProcessingException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Stream processing error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    @ExceptionHandler(LogChargementNotFoundException.class)
    public ResponseEntity<ApiError> handleLogChargementNotFound(LogChargementNotFoundException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Log chargement not found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}
