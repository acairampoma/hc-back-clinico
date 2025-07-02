package com.formacionbdi.microservicios.app.orden.exception;

import com.formacionbdi.microservicios.commons.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 🛡️ GLOBAL EXCEPTION HANDLER PARA ÓRDENES - Java 17 Functional Style
 *
 * Features Java 17:
 * - Switch expressions para status codes
 * - Text blocks para mensajes estructurados
 * - Functions para transformaciones
 * - Records para respuestas estructuradas
 * - Pattern matching simulado
 *
 * @author Microservicio Órdenes
 * @version 1.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class OrdenGlobalExceptionHandler {

    // =====================================================
    // 🔥 FUNCTIONS FUNCIONALES PARA TRANSFORMACIONES
    // =====================================================

    /**
     * Function para extraer errores de validación
     */
    private final Function<FieldError, Map<String, String>> extractFieldError = fieldError ->
            Map.of(
                    "campo", fieldError.getField(),
                    "valor", fieldError.getRejectedValue() != null ?
                            fieldError.getRejectedValue().toString() : "null",
                    "mensaje", fieldError.getDefaultMessage() != null ?
                            fieldError.getDefaultMessage() : "Error de validación"
            );

    /**
     * Function para obtener path de request
     */
    private final Function<HttpServletRequest, String> extractPath = request ->
            request.getRequestURI() != null ? request.getRequestURI() : "unknown";

    // =====================================================
    // 🔥 EXCEPTION HANDLERS CON JAVA 17
    // =====================================================

    /**
     * Handler para OrdenNotFoundException
     */
    @ExceptionHandler(OrdenNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleOrdenNotFoundException(
            OrdenNotFoundException ex, HttpServletRequest request) {

        log.warn("🔍 Orden no encontrada: {}", ex.toLogString());

        // 🔥 SWITCH EXPRESSION para contexto adicional
        String contextoAdicional = switch (ex.getTipoBusqueda()) {
            case POR_ID -> "Verifique el ID de la orden";
            case POR_NUMERO -> "Confirme el número de orden";
            case POR_PACIENTE -> "Revise los filtros de paciente";
            case POR_MEDICO -> "Revise los filtros de médico";
            case PERSONALIZADA -> "Revise los criterios de búsqueda";
        };

        Map<String, Object> errorDetails = Map.of(
                "codigo", ex.getCodigo(),
                "tipo", ex.getTipoBusqueda().name(),
                "identificador", ex.getIdentificador(),
                "contexto", ex.getContextoDetallado(),
                "sugerencias", ex.getSugerencias(),
                "contextoAdicional", contextoAdicional,
                "timestamp", LocalDateTime.now(),
                "path", extractPath.apply(request)
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), extractPath.apply(request)));
    }

    /**
     * Handler para OrdenBusinessException
     */
    @ExceptionHandler(OrdenBusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleOrdenBusinessException(
            OrdenBusinessException ex, HttpServletRequest request) {

        log.error("🛡️ Error de negocio en órdenes: {}", ex.toLogString());

        // 🔥 SWITCH EXPRESSION para HTTP Status
        HttpStatus status = switch (ex.getTipoError()) {
            case VALIDACION, DUPLICADO -> HttpStatus.BAD_REQUEST;
            case ESTADO, NEGOCIO -> HttpStatus.CONFLICT;
            case PERMISOS -> HttpStatus.FORBIDDEN;
            case SISTEMA -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        // Usar el método del exception que ya retorna Map estructurado
        Map<String, Object> errorDetails = ex.toApiResponse();
        errorDetails.put("path", extractPath.apply(request));

        return ResponseEntity
                .status(status)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(ex.getMensajeUsuario())
                        .data(errorDetails)
                        .error(ex.getMessage())
                        .path(extractPath.apply(request))
                        .build());
    }

    /**
     * Handler para ExamenNotFoundException
     */
    @ExceptionHandler(ExamenNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleExamenNotFoundException(
            ExamenNotFoundException ex, HttpServletRequest request) {

        log.warn("🔬 Examen no encontrado: {}", ex.toLogString());

        // Usar el método del exception que ya retorna Map estructurado
        Map<String, Object> errorDetails = ex.toApiResponse();
        errorDetails.put("path", extractPath.apply(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(errorDetails)
                        .error("Examen no encontrado")
                        .path(extractPath.apply(request))
                        .build());
    }

    /**
     * Handler para OrdenValidationException
     */
    @ExceptionHandler(OrdenValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleOrdenValidationException(
            OrdenValidationException ex, HttpServletRequest request) {

        log.warn("✅ Error de validación en órdenes: {}", ex.toLogString());

        // 🔥 SWITCH EXPRESSION para HTTP Status según severidad
        HttpStatus status = switch (ex.getSeveridad()) {
            case "HIGH" -> HttpStatus.BAD_REQUEST;
            case "MEDIUM" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "LOW" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };

        // Usar el método del exception que ya retorna Map estructurado
        Map<String, Object> errorDetails = ex.toApiResponse();
        errorDetails.put("path", extractPath.apply(request));

        return ResponseEntity
                .status(status)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(ex.getMensajeUsuario())
                        .data(errorDetails)
                        .error("Error de validación")
                        .path(extractPath.apply(request))
                        .build());
    }

    /**
     * Handler para errores de validación de Spring
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("📝 Errores de validación Spring: {} errores encontrados",
                ex.getBindingResult().getErrorCount());

        // 🔥 PROGRAMACIÓN FUNCIONAL: Stream + Function para transformar errores
        var erroresValidacion = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(extractFieldError)
                .collect(Collectors.toList());

        // Text block para mensaje detallado
        String mensajeDetallado = """
            Se encontraron %d errores de validación en la solicitud.
            Revise los campos marcados y corrija los valores antes de continuar.
            """.formatted(erroresValidacion.size()).trim();

        Map<String, Object> errorDetails = Map.of(
                "codigo", "VALIDATION_SPRING_001",
                "tipo", "VALIDATION_ERROR",
                "totalErrores", erroresValidacion.size(),
                "errores", erroresValidacion,
                "timestamp", LocalDateTime.now(),
                "path", extractPath.apply(request),
                "sugerencias", """
                • Verifique que todos los campos obligatorios estén completos
                • Confirme que los formatos de fecha sean correctos (YYYY-MM-DD)
                • Valide que los números estén en el rango permitido
                • Asegúrese de que los IDs sean válidos
                """
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(mensajeDetallado)
                        .data(errorDetails)
                        .error("Errores de validación")
                        .path(extractPath.apply(request))
                        .build());
    }

    /**
     * Handler para IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("⚠️ Argumento ilegal: {}", ex.getMessage());

        Map<String, Object> errorDetails = Map.of(
                "codigo", "ARGUMENT_INVALID_001",
                "tipo", "ILLEGAL_ARGUMENT",
                "mensaje", ex.getMessage(),
                "timestamp", LocalDateTime.now(),
                "path", extractPath.apply(request),
                "sugerencias", """
                • Verifique que todos los parámetros sean válidos
                • Confirme que los valores no sean nulos
                • Revise la documentación de la API
                """
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Parámetros inválidos en la solicitud")
                        .data(errorDetails)
                        .error(ex.getMessage())
                        .path(extractPath.apply(request))
                        .build());
    }

    /**
     * Handler genérico para todas las demás excepciones
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("💥 Error interno del servidor: {}", ex.getMessage(), ex);

        // 🔥 SWITCH EXPRESSION para determinar si mostrar detalles
        boolean mostrarDetalles = switch (ex.getClass().getSimpleName()) {
            case "DataIntegrityViolationException", "ConstraintViolationException" -> true;
            case "OptimisticLockingFailureException", "PessimisticLockingFailureException" -> true;
            default -> false;
        };

        // Text block para mensaje genérico
        String mensajeUsuario = """
            Ha ocurrido un error interno en el servidor.
            Por favor, intente nuevamente en unos minutos.
            Si el problema persiste, contacte al administrador del sistema.
            """.trim();

        Map<String, Object> errorDetails = Map.of(
                "codigo", "INTERNAL_SERVER_ERROR_001",
                "tipo", "SYSTEM_ERROR",
                "excepcion", ex.getClass().getSimpleName(),
                "mensaje", mostrarDetalles ? ex.getMessage() : "Error interno del sistema",
                "timestamp", LocalDateTime.now(),
                "path", extractPath.apply(request),
                "sugerencias", """
                • Intente la operación nuevamente en unos minutos
                • Verifique que los datos sean correctos
                • Contacte al equipo de soporte si persiste el error
                • Reporte el timestamp para seguimiento técnico
                """
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(mensajeUsuario)
                        .data(errorDetails)
                        .error("Error interno del servidor")
                        .path(extractPath.apply(request))
                        .build());
    }

    // =====================================================
    // 🔥 MÉTODOS HELPER FUNCIONALES
    // =====================================================

    /**
     * 🔥 MÉTODO FUNCIONAL: Determina si es error de base de datos
     */
    private boolean esErrorBaseDatos(Exception ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "DataIntegrityViolationException",
                    "ConstraintViolationException",
                    "SQLIntegrityConstraintViolationException",
                    "DataAccessException" -> true;
            default -> false;
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL JAVA 17: Obtiene mensaje amigable para errores de BD
     */
    private String getMensajeAmigableBaseDatos(Exception ex) {
        String mensaje = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Java 17 compatible - usar if-else en lugar de pattern matching
        if (mensaje.contains("duplicate") || mensaje.contains("unique")) {
            return "El registro que intenta crear ya existe en el sistema";
        }

        if (mensaje.contains("foreign key") || mensaje.contains("referenced")) {
            return "El registro está relacionado con otros datos y no puede ser modificado";
        }

        if (mensaje.contains("not null") || mensaje.contains("required")) {
            return "Faltan campos obligatorios para completar la operación";
        }

        if (mensaje.contains("timeout") || mensaje.contains("connection")) {
            return "Problema temporal de conexión. Intente nuevamente";
        }

        return "Error en la base de datos. Contacte al administrador";
    }

    /**
     * 🔥 RECORD JAVA 17: Para respuestas de error estructuradas
     */
    public record ErrorResponse(
            String codigo,
            String mensaje,
            String tipo,
            LocalDateTime timestamp,
            String path,
            Map<String, Object> detalles
    ) {
        /**
         * Constructor compacto con validación
         */
        public ErrorResponse {
            if (codigo == null || codigo.trim().isEmpty()) {
                codigo = "UNKNOWN_ERROR";
            }
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }
        }

        /**
         * Factory method funcional para crear respuesta rápida
         */
        public static ErrorResponse of(String codigo, String mensaje, String path) {
            return new ErrorResponse(
                    codigo,
                    mensaje,
                    "GENERIC_ERROR",
                    LocalDateTime.now(),
                    path,
                    Map.of()
            );
        }

        /**
         * Factory method funcional con detalles
         */
        public static ErrorResponse withDetails(String codigo, String mensaje, String path, Map<String, Object> detalles) {
            return new ErrorResponse(codigo, mensaje, "DETAILED_ERROR", LocalDateTime.now(), path, detalles);
        }
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Log estructurado para todas las excepciones
     */
    private void logExceptionDetails(Exception ex, HttpServletRequest request) {
        String logMessage = """
                === EXCEPCIÓN CAPTURADA ===
                Tipo: %s
                Mensaje: %s
                Path: %s
                Method: %s
                User-Agent: %s
                Timestamp: %s
                ===========================
                """.formatted(
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                extractPath.apply(request),
                request.getMethod(),
                request.getHeader("User-Agent"),
                LocalDateTime.now()
        );

        log.error(logMessage, ex);
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Sanitizar mensaje de error para usuario
     */
    private String sanitizarMensajeParaUsuario(String mensajeOriginal) {
        if (mensajeOriginal == null) return "Error desconocido";

        // Remover información técnica sensible
        return mensajeOriginal
                .replaceAll("(?i)password", "***")
                .replaceAll("(?i)token", "***")
                .replaceAll("(?i)secret", "***")
                .replaceAll("(?i)key", "***")
                .replaceAll("java\\.\\w+\\.\\w+Exception", "Error del sistema")
                .replaceAll("org\\.\\w+\\.\\w+", "Sistema")
                .substring(0, Math.min(500, mensajeOriginal.length())); // Limitar longitud
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Determinar código de error automático
     */
    private String determinarCodigoError(Exception ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "OrdenNotFoundException" -> "ORDEN_NOT_FOUND";
            case "ExamenNotFoundException" -> "EXAMEN_NOT_FOUND";
            case "OrdenBusinessException" -> "ORDEN_BUSINESS_ERROR";
            case "OrdenValidationException" -> "ORDEN_VALIDATION_ERROR";
            case "MethodArgumentNotValidException" -> "VALIDATION_ERROR";
            case "IllegalArgumentException" -> "INVALID_ARGUMENT";
            case "DataIntegrityViolationException" -> "DATA_INTEGRITY_ERROR";
            case "OptimisticLockingFailureException" -> "CONCURRENT_MODIFICATION";
            case "AccessDeniedException" -> "ACCESS_DENIED";
            default -> "UNKNOWN_ERROR";
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Builder pattern funcional para ApiResponse
     */
    private ResponseEntity<ApiResponse<Map<String, Object>>> buildErrorResponse(
            Exception ex,
            HttpStatus status,
            String mensajeUsuario,
            HttpServletRequest request) {

        // Log automático
        logExceptionDetails(ex, request);

        Map<String, Object> errorDetails = Map.of(
                "codigo", determinarCodigoError(ex),
                "tipo", ex.getClass().getSimpleName(),
                "mensaje", sanitizarMensajeParaUsuario(ex.getMessage()),
                "timestamp", LocalDateTime.now(),
                "path", extractPath.apply(request),
                "method", request.getMethod(),
                "esErrorBaseDatos", esErrorBaseDatos(ex)
        );

        return ResponseEntity
                .status(status)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message(mensajeUsuario)
                        .data(errorDetails)
                        .error(sanitizarMensajeParaUsuario(ex.getMessage()))
                        .path(extractPath.apply(request))
                        .build());
    }
}