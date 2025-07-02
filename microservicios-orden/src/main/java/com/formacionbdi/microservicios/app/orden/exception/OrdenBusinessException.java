package com.formacionbdi.microservicios.app.orden.exception;

import lombok.Getter;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 🛡️ EXCEPCIÓN: Errores de negocio de órdenes - Java 17 Functional Style
 *
 * Features Java 17:
 * - Switch expressions para categorización
 * - Text blocks para mensajes detallados
 * - Suppliers funcionales para reutilización
 * - Records para data inmutable (futuro)
 * - Pattern matching simulado
 *
 * @author Microservicio Órdenes
 * @version 1.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Getter
public class OrdenBusinessException extends RuntimeException {

    private final String codigoError;
    private final TipoError tipoError;
    private final Map<String, Object> contexto;

    /**
     * 🔥 ENUM CON JAVA 17: Categorización inteligente de errores
     */
    public enum TipoError {
        VALIDACION("Validación de datos"),
        ESTADO("Error de estado"),
        DUPLICADO("Registro duplicado"),
        NEGOCIO("Regla de negocio"),
        PERMISOS("Sin permisos"),
        SISTEMA("Error del sistema");

        private final String descripcion;

        TipoError(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() { return descripcion; }
    }

    // =====================================================
    // 🔥 CONSTRUCTORES FUNCIONALES
    // =====================================================

    /**
     * Constructor principal - Más usado
     */
    public OrdenBusinessException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
        this.tipoError = determinarTipoError(codigoError);
        this.contexto = Map.of("timestamp", System.currentTimeMillis());
    }

    /**
     * Constructor con causa raíz
     */
    public OrdenBusinessException(String codigoError, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = codigoError;
        this.tipoError = determinarTipoError(codigoError);
        this.contexto = Map.of(
                "timestamp", System.currentTimeMillis(),
                "causaRaiz", causa.getClass().getSimpleName()
        );
    }

    /**
     * Constructor con contexto adicional
     */
    public OrdenBusinessException(String codigoError, String mensaje, Map<String, Object> contexto) {
        super(mensaje);
        this.codigoError = codigoError;
        this.tipoError = determinarTipoError(codigoError);
        this.contexto = Map.copyOf(contexto); // Inmutable
    }

    // =====================================================
    // 🔥 SUPPLIERS FUNCIONALES PARA REUTILIZACIÓN
    // =====================================================

    /**
     * Supplier funcional para validaciones - Más común
     */
    public static Supplier<OrdenBusinessException> validacion(String codigo, String mensaje) {
        return () -> new OrdenBusinessException(codigo, mensaje);
    }

    /**
     * Supplier funcional para errores de estado
     */
    public static Supplier<OrdenBusinessException> estadoInvalido(String estadoActual, String estadoRequerido) {
        return () -> new OrdenBusinessException(
                "ORDEN_ESTADO_001",
                """
                Estado de orden inválido para la operación.
                Estado actual: %s
                Estado requerido: %s
                """.formatted(estadoActual, estadoRequerido).trim()
        );
    }

    /**
     * Supplier funcional para duplicados
     */
    public static Supplier<OrdenBusinessException> duplicado(String tipo, String identificador) {
        return () -> new OrdenBusinessException(
                "ORDEN_DUPLICADO_001",
                """
                Ya existe una orden de tipo %s con identificador: %s
                No se permiten duplicados en el mismo día.
                """.formatted(tipo, identificador).trim()
        );
    }

    /**
     * Supplier funcional para reglas de negocio
     */
    public static Supplier<OrdenBusinessException> reglaNegocio(String regla, String valor) {
        return () -> new OrdenBusinessException(
                "ORDEN_NEGOCIO_001",
                String.format("Violación de regla de negocio: %s. Valor: %s", regla, valor)
        );
    }

    // =====================================================
    // 🔥 MÉTODOS FUNCIONALES CON JAVA 17
    // =====================================================

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Determina tipo de error por código
     */
    private TipoError determinarTipoError(String codigo) {
        if (codigo == null) return TipoError.SISTEMA;

        return switch (codigo.substring(0, Math.min(6, codigo.length()))) {
            case "ORDEN_" -> {
                // Sub-switch por el número
                String numero = codigo.length() > 6 ? codigo.substring(6, 9) : "999";
                yield switch (numero) {
                    case "001", "002", "003", "004", "005", "006" -> TipoError.VALIDACION;
                    case "007", "008", "009", "010", "011", "012" -> TipoError.ESTADO;
                    case "013", "014", "015" -> TipoError.DUPLICADO;
                    case "016", "017", "018", "019", "020" -> TipoError.NEGOCIO;
                    default -> TipoError.SISTEMA;
                };
            }
            case "VALID_" -> TipoError.VALIDACION;
            case "PERM_" -> TipoError.PERMISOS;
            case "DUP_" -> TipoError.DUPLICADO;
            default -> TipoError.SISTEMA;
        };
    }

    /**
     * 🔥 PATTERN MATCHING SIMULADO: Verifica si es error crítico
     */
    public boolean esCritico() {
        return switch (tipoError) {
            case SISTEMA, PERMISOS -> true;
            case VALIDACION, ESTADO, DUPLICADO, NEGOCIO -> false;
        };
    }

    /**
     * 🔥 SWITCH EXPRESSION: Obtiene nivel de severidad
     */
    public String getNivelSeveridad() {
        return switch (tipoError) {
            case SISTEMA -> "CRITICAL";
            case PERMISOS -> "HIGH";
            case NEGOCIO, ESTADO -> "MEDIUM";
            case VALIDACION, DUPLICADO -> "LOW";
        };
    }

    /**
     * 🔥 TEXT BLOCKS JAVA 17: Obtiene mensaje para el usuario
     */
    public String getMensajeUsuario() {
        return switch (tipoError) {
            case VALIDACION -> """
                ℹ️ Error de validación
                %s
                
                Por favor, revise los datos ingresados y intente nuevamente.
                """.formatted(getMessage()).trim();

            case ESTADO -> """
                ⚠️ Estado de orden inválido
                %s
                
                La orden debe estar en un estado específico para realizar esta operación.
                """.formatted(getMessage()).trim();

            case DUPLICADO -> """
                🔄 Registro duplicado
                %s
                
                Esta orden ya existe. Revise las órdenes existentes.
                """.formatted(getMessage()).trim();

            case NEGOCIO -> """
                📋 Regla de negocio
                %s
                
                Esta operación viola una regla del sistema médico.
                """.formatted(getMessage()).trim();

            case PERMISOS -> """
                🔒 Sin permisos
                %s
                
                No tiene autorización para realizar esta operación.
                """.formatted(getMessage()).trim();

            case SISTEMA -> """
                ⚙️ Error del sistema
                Ha ocurrido un error interno. Contacte al administrador.
                
                Código de referencia: %s
                """.formatted(codigoError).trim();
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene sugerencias de solución
     */
    public String getSugerenciasSolucion() {
        return switch (tipoError) {
            case VALIDACION -> """
                • Verifique que todos los campos obligatorios estén completos
                • Confirme que los formatos de fecha sean correctos
                • Valide que los IDs de exámenes existan
                """;

            case ESTADO -> """
                • Verifique el estado actual de la orden
                • Confirme los permisos para cambiar estados
                • Revise el flujo de trabajo de órdenes
                """;

            case DUPLICADO -> """
                • Busque la orden existente en el sistema
                • Considere modificar la orden existente
                • Verifique si realmente necesita una nueva orden
                """;

            case NEGOCIO -> """
                • Revise las políticas médicas del hospital
                • Consulte con el supervisor médico
                • Verifique los protocolos de atención
                """;

            case PERMISOS -> """
                • Contacte al administrador del sistema
                • Verifique su rol y permisos asignados
                • Confirme que esté autenticado correctamente
                """;

            case SISTEMA -> """
                • Intente la operación nuevamente en unos minutos
                • Contacte al equipo de soporte técnico
                • Reporte el código de error: %s
                """.formatted(codigoError);
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para logging estructurado
     */
    public String toLogString() {
        return """
               OrdenBusinessException {
                 codigo: %s,
                 tipo: %s,
                 severidad: %s,
                 critico: %s,
                 mensaje: %s,
                 contexto: %s
               }
               """.formatted(
                codigoError,
                tipoError,
                getNivelSeveridad(),
                esCritico(),
                getMessage(),
                contexto
        );
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para respuestas de API estructuradas
     */
    public Map<String, Object> toApiResponse() {
        return Map.of(
                "codigo", codigoError,
                "tipo", tipoError.name(),
                "severidad", getNivelSeveridad(),
                "mensaje", getMensajeUsuario(),
                "sugerencias", getSugerenciasSolucion(),
                "timestamp", contexto.get("timestamp")
        );
    }
}