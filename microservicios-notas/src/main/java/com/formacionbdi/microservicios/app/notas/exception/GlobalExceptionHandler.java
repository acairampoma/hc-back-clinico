package com.formacionbdi.microservicios.app.notas.exception;

import com.formacionbdi.microservicios.app.notas.models.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el microservicio de notas vitales
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ===== 🎯 EXCEPCIONES ESPECÍFICAS DE NEGOCIO =====

    @ExceptionHandler(NotaBusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotaBusinessException(
            NotaBusinessException ex, WebRequest request) {

        log.warn("Regla de negocio violada - Código: {}, Mensaje: {}", ex.getCodigo(), ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("codigo", ex.getCodigo());
        errorDetails.put("detalles", ex.getDetalles());
        errorDetails.put("timestamp", java.time.LocalDateTime.now());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Regla de negocio no cumplida"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(NotaNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotaNotFoundException(
            NotaNotFoundException ex, WebRequest request) {

        log.warn("Recurso no encontrado - {}: {}", ex.getRecurso(), ex.getIdentificador());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("recurso", ex.getRecurso());
        errorDetails.put("identificador", ex.getIdentificador());
        errorDetails.put("timestamp", java.time.LocalDateTime.now());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Recurso no encontrado"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NotaValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotaValidationException(
            NotaValidationException ex, WebRequest request) {

        log.warn("Error de validación: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();

        if (ex.getErroresValidacion() != null) {
            errorDetails.put("errores_validacion", ex.getErroresValidacion());
        }

        if (ex.getCampo() != null) {
            errorDetails.put("campo", ex.getCampo());
        }

        errorDetails.put("timestamp", java.time.LocalDateTime.now());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Error de validación de datos"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NotaProcessingException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotaProcessingException(
            NotaProcessingException ex, WebRequest request) {

        log.error("Error de procesamiento - Operación: {}, Mensaje: {}",
                ex.getOperacion(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("operacion", ex.getOperacion());
        errorDetails.put("detalles_tecnicos", ex.getDetallesTecnicos());
        errorDetails.put("timestamp", java.time.LocalDateTime.now());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Error de procesamiento interno"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ===== 🔧 VALIDACIONES ESTÁNDAR DE SPRING =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        log.warn("Errores de validación en campos: {}",
                ex.getBindingResult().getFieldErrors().size());

        Map<String, List<String>> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

        ApiResponse<Map<String, List<String>>> response = ApiResponse.error(
                "Datos inválidos en la solicitud"
        );
        response.setData(errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("Violación de restricciones: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("violaciones", ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    Map<String, String> violationMap = new HashMap<>();
                    violationMap.put("campo", violation.getPropertyPath().toString());
                    violationMap.put("mensaje", violation.getMessage());
                    violationMap.put("valor_rechazado",
                            violation.getInvalidValue() != null ?
                                    violation.getInvalidValue().toString() : "null");
                    return violationMap;
                })
                .collect(Collectors.toList()));
        errorDetails.put("timestamp", java.time.LocalDateTime.now());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                "Errores de validación en los datos",
                "Restricciones violadas"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===== ⚡ EXCEPCIONES GENÉRICAS =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Argumento ilegal: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                "Parámetro inválido en la solicitud"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("Error inesperado en el microservicio de notas", ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("tipo_error", ex.getClass().getSimpleName());
        errorDetails.put("path", request.getDescription(false));
        errorDetails.put("timestamp", java.time.LocalDateTime.now());

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                "Error interno del servidor",
                "Ha ocurrido un error inesperado"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ===== 📋 MÉTODOS HELPER =====

    private String obtenerMensajePersonalizado(String codigoError) {
        Map<String, String> mensajes = Map.of(
                "NOTA_001", "Solo puede tener una nota en borrador por hospitalización",
                "NOTA_002", "No se pueden modificar notas finalizadas",
                "NOTA_003", "El audio no está disponible o fue eliminado",
                "NOTA_004", "Se requiere firma digital para finalizar la nota",
                "NOTA_005", "Sin permisos para modificar esta nota"
        );

        return mensajes.getOrDefault(codigoError, "Error en el procesamiento de la nota");
    }
}