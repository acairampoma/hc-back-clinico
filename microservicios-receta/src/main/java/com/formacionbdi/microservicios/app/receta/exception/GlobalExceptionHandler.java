package com.formacionbdi.microservicios.app.receta.exception;

import com.formacionbdi.microservicios.commons.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el microservicio de recetas médicas
 * TODA la lógica de manejo de errores centralizada aquí
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ===== EXCEPCIONES ESPECÍFICAS DE NEGOCIO =====

    @ExceptionHandler(RecetaBusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecetaBusinessException(
            RecetaBusinessException ex, WebRequest request) {

        log.warn("Regla de negocio violada - Código: {}, Mensaje: {}", ex.getCodigo(), ex.getMessage());

        return ResponseEntity
                .status(determinarStatusPorCodigo(ex.getCodigo()))
                .body(ApiResponse.error(
                    String.format("[%s] %s", ex.getCodigo(), ex.getMessage()),
                    request.getDescription(false)
                ));
    }

    // ===== EXCEPCIONES DE VALIDACIÓN =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        String mensajeError = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    String.format("Error de validación - %s", mensajeError),
                    request.getDescription(false)
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        String mensajeError = ex.getConstraintViolations().stream()
                .map(violation -> String.format("%s: %s", 
                    violation.getPropertyPath(), 
                    violation.getMessage()))
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    String.format("Error de validación - %s", mensajeError),
                    request.getDescription(false)
                ));
    }

    // ===== EXCEPCIONES DE NO ENCONTRADO =====

    @ExceptionHandler(RecetaNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecetaNotFoundException(
            RecetaNotFoundException ex, WebRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                    String.format("Recurso no encontrado - ID %d: %s", 
                        ex.getRecursoId(), 
                        ex.getMessage()),
                    request.getDescription(false)
                ));
    }

    // ===== EXCEPCIONES GENERALES =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Error no manejado: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    String.format("Error interno del servidor: %s", ex.getMessage()),
                    request.getDescription(false)
                ));
    }

    // ===== MÉTODOS HELPER =====

    private HttpStatus determinarStatusPorCodigo(String codigo) {
        return switch (codigo) {
            case "REC-001" -> HttpStatus.BAD_REQUEST;        // Datos inválidos
            case "REC-002" -> HttpStatus.NOT_FOUND;          // Recurso no encontrado
            case "REC-003" -> HttpStatus.CONFLICT;           // Conflicto de estado
            case "REC-004" -> HttpStatus.FORBIDDEN;          // Sin permisos
            case "REC-005" -> HttpStatus.UNPROCESSABLE_ENTITY; // No se puede procesar
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}