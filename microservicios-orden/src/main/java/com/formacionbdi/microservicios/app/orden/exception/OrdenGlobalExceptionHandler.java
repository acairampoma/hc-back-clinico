package com.formacionbdi.microservicios.app.orden.exception;

import com.formacionbdi.microservicios.app.orden.models.dto.OrdenResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 🛡️ GLOBAL EXCEPTION HANDLER PARA ÓRDENES
 * Manejo centralizado de todas las excepciones
 */
@RestControllerAdvice
public class OrdenGlobalExceptionHandler {

    @ExceptionHandler(OrdenBusinessException.class)
    public ResponseEntity<OrdenResponseDTO<String>> handleOrdenBusinessException(OrdenBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(OrdenResponseDTO.error("Error de negocio: " + ex.getMessage()));
    }

    @ExceptionHandler(OrdenNotFoundException.class)
    public ResponseEntity<OrdenResponseDTO<String>> handleOrdenNotFoundException(OrdenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(OrdenResponseDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(ExamenNotFoundException.class)
    public ResponseEntity<OrdenResponseDTO<String>> handleExamenNotFoundException(ExamenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(OrdenResponseDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrdenResponseDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest()
                .body(OrdenResponseDTO.<Map<String, String>>builder()
                        .success(false)
                        .message("Errores de validación")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OrdenResponseDTO<String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OrdenResponseDTO.error("Error interno del servidor: " + ex.getMessage()));
    }
}