package com.formacionbdi.microservicios.app.receta.exception;

import com.formacionbdi.microservicios.app.receta.models.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🛡️ Manejador global de excepciones para el microservicio de recetas médicas
 * TODA la lógica de manejo de errores centralizada aquí
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ===== 🎯 EXCEPCIONES ESPECÍFICAS DE NEGOCIO =====

    @ExceptionHandler(RecetaBusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecetaBusinessException(
            RecetaBusinessException ex, WebRequest request) {

        log.warn("🚫 Regla de negocio violada - Código: {}, Mensaje: {}", ex.getCodigo(), ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
                "codigo", ex.getCodigo(),
                "detalles", ex.getDetalles() != null ? ex.getDetalles() : "Sin detalles adicionales",
                "timestamp", LocalDateTime.now(),
                "tipo_error", "REGLA_NEGOCIO"
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Regla de negocio no cumplida"
        );
        response.setData(errorDetails);

        // Status específico según el código de error
        HttpStatus status = determinarStatusPorCodigo(ex.getCodigo());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(RecetaNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecetaNotFoundException(
            RecetaNotFoundException ex, WebRequest request) {

        log.warn("🔍 Recurso no encontrado - {}: {}", ex.getRecurso(), ex.getIdentificador());

        Map<String, Object> errorDetails = Map.of(
                "recurso", ex.getRecurso(),
                "identificador", ex.getIdentificador(),
                "timestamp", LocalDateTime.now(),
                "tipo_error", "RECURSO_NO_ENCONTRADO"
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Recurso no encontrado"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RecetaValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecetaValidationException(
            RecetaValidationException ex, WebRequest request) {

        log.warn("✅ Error de validación: {}", ex.getMessage());

        Map<String, Object> errorDetails = new HashMap<>();

        if (ex.getErroresValidacion() != null) {
            errorDetails.put("errores_validacion", ex.getErroresValidacion());
        }

        if (ex.getCampo() != null) {
            errorDetails.put("campo_problematico", ex.getCampo());
        }

        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("tipo_error", "VALIDACION");

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Error de validación de datos"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RecetaProcessingException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecetaProcessingException(
            RecetaProcessingException ex, WebRequest request) {

        log.error("⚙️ Error de procesamiento - Operación: {}, Mensaje: {}",
                ex.getOperacion(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = Map.of(
                "operacion", ex.getOperacion(),
                "detalles_tecnicos", ex.getDetallesTecnicos() != null ? ex.getDetallesTecnicos() : "Sin detalles técnicos",
                "timestamp", LocalDateTime.now(),
                "tipo_error", "PROCESAMIENTO"
        );

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

        log.warn("📝 Errores de validación en campos: {}",
                ex.getBindingResult().getFieldErrors().size());

        Map<String, List<String>> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

        ApiResponse<Map<String, List<String>>> response = ApiResponse.error(
                "Datos inválidos en la solicitud de receta"
        );
        response.setData(errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("🚫 Violación de restricciones: {}", ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
                "violaciones", ex.getConstraintViolations()
                        .stream()
                        .map(violation -> Map.of(
                                "campo", violation.getPropertyPath().toString(),
                                "mensaje", violation.getMessage(),
                                "valor_rechazado", violation.getInvalidValue() != null ?
                                        violation.getInvalidValue().toString() : "null"
                        ))
                        .collect(Collectors.toList()),
                "timestamp", LocalDateTime.now(),
                "tipo_error", "CONSTRAINT_VIOLATION"
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                "Errores de restricciones en los datos de la receta",
                "Restricciones violadas"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===== ⚡ EXCEPCIONES GENÉRICAS =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("📋 Argumento ilegal en receta: {}", ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
                "argumento_problematico", ex.getMessage(),
                "timestamp", LocalDateTime.now(),
                "tipo_error", "ARGUMENTO_ILEGAL"
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                ex.getMessage(),
                "Parámetro inválido en la solicitud"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("💥 Error inesperado en el microservicio de recetas", ex);

        Map<String, Object> errorDetails = Map.of(
                "tipo_error_tecnico", ex.getClass().getSimpleName(),
                "path", request.getDescription(false),
                "timestamp", LocalDateTime.now(),
                "microservicio", "recetas-medicas"
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.error(
                "Error interno del servidor",
                "Ha ocurrido un error inesperado en el procesamiento de recetas"
        );
        response.setData(errorDetails);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ===== 🔧 MÉTODOS HELPER =====

    /**
     * Determina el status HTTP según el código de error de negocio
     */
    private HttpStatus determinarStatusPorCodigo(String codigo) {
        switch (codigo) {
            case "RECETA_001":
                return HttpStatus.CONFLICT;        // Receta duplicada
            case "RECETA_002":
                return HttpStatus.CONFLICT;        // No modificable
            case "RECETA_003":
                return HttpStatus.BAD_REQUEST;     // Cantidad excesiva
            case "RECETA_004":
                return HttpStatus.CONFLICT;        // Ya finalizada
            case "RECETA_005":
                return HttpStatus.PRECONDITION_REQUIRED; // Firma requerida
            case "RECETA_006":
                return HttpStatus.FORBIDDEN;       // Sin permisos
            case "RECETA_007":
                return HttpStatus.GONE;           // Receta vencida
            case "RECETA_008":
                return HttpStatus.NOT_ACCEPTABLE; // Medicamento no disponible
            default:
                return HttpStatus.CONFLICT;
        }
    }

    /**
     * Obtiene mensaje personalizado según el código de error
     */
    private String obtenerMensajePersonalizado(String codigoError) {
        Map<String, String> mensajes = Map.of(
                "RECETA_001", "Solo se permite una receta por origen en el mismo día",
                "RECETA_002", "La receta solo puede modificarse hasta 24 horas antes del vencimiento",
                "RECETA_003", "La cantidad máxima permitida es de 2 unidades por medicamento",
                "RECETA_004", "Las recetas finalizadas no pueden ser modificadas",
                "RECETA_005", "Se requiere firma digital para procesar la receta",
                "RECETA_006", "Solo el médico creador puede modificar la receta",
                "RECETA_007", "La receta está vencida y no puede ser procesada",
                "RECETA_008", "El medicamento no está disponible para prescripción"
        );

        return mensajes.getOrDefault(codigoError, "Error en el procesamiento de la receta");
    }

    /**
     * Registra estadísticas de errores para monitoreo
     */
    private void registrarEstadisticaError(String tipoError, String codigo) {
        // TODO: Implementar métricas de errores para monitoreo
        log.info("📊 Estadística error - Tipo: {}, Código: {}", tipoError, codigo);
    }
}