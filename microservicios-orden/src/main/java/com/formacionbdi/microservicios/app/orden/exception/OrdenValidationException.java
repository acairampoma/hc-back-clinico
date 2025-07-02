package com.formacionbdi.microservicios.app.orden.exception;

import lombok.Getter;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ✅ EXCEPCIÓN: Errores de validación de órdenes - Java 17 Functional Style
 *
 * Features Java 17:
 * - Switch expressions para categorización
 * - Records para errores de campo
 * - Suppliers funcionales para reutilización
 * - Text blocks para mensajes detallados
 *
 * @author Microservicio Órdenes
 * @version 1.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Getter
public class OrdenValidationException extends RuntimeException {

    private final String codigo;
    private final List<CampoError> erroresCampos;
    private final TipoValidacion tipoValidacion;

    /**
     * 🔥 RECORD JAVA 17: Error de campo inmutable
     */
    public record CampoError(
            String campo,
            Object valorActual,
            String mensaje,
            String valorEsperado
    ) {
        /**
         * Constructor compacto con validación
         */
        public CampoError {
            if (campo == null || campo.trim().isEmpty()) {
                throw new IllegalArgumentException("Campo no puede ser nulo o vacío");
            }
            if (mensaje == null || mensaje.trim().isEmpty()) {
                throw new IllegalArgumentException("Mensaje no puede ser nulo o vacío");
            }
        }

        /**
         * 🔥 MÉTODO FUNCIONAL: Formato para mostrar al usuario
         */
        public String toUserMessage() {
            return valorEsperado != null ?
                    String.format("%s: %s. Se esperaba: %s", campo, mensaje, valorEsperado) :
                    String.format("%s: %s", campo, mensaje);
        }

        /**
         * 🔥 MÉTODO FUNCIONAL: Para logging detallado
         */
        public String toLogMessage() {
            return """
                   Campo: %s
                   Valor actual: %s
                   Mensaje: %s
                   Valor esperado: %s
                   """.formatted(campo, valorActual, mensaje, valorEsperado);
        }
    }

    /**
     * 🔥 ENUM CON JAVA 17: Tipos de validación
     */
    public enum TipoValidacion {
        FORMATO("Formato de datos"),
        RANGO("Valores fuera de rango"),
        REQUERIDO("Campos obligatorios"),
        LOGICA("Validación lógica"),
        CONSISTENCIA("Consistencia de datos"),
        INTEGRIDAD("Integridad referencial");

        private final String descripcion;

        TipoValidacion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() { return descripcion; }
    }

    // =====================================================
    // 🔥 CONSTRUCTORES FUNCIONALES
    // =====================================================

    /**
     * Constructor principal para múltiples errores
     */
    public OrdenValidationException(String codigo, List<CampoError> erroresCampos, TipoValidacion tipo) {
        super(generarMensajeCompuesto(erroresCampos, tipo));
        this.codigo = codigo;
        this.erroresCampos = List.copyOf(erroresCampos); // Inmutable
        this.tipoValidacion = tipo;
    }

    /**
     * Constructor para un solo error de campo
     */
    public OrdenValidationException(String codigo, CampoError error, TipoValidacion tipo) {
        super(error.toUserMessage());
        this.codigo = codigo;
        this.erroresCampos = List.of(error);
        this.tipoValidacion = tipo;
    }

    /**
     * Constructor simple para mensaje personalizado
     */
    public OrdenValidationException(String codigo, String mensaje, TipoValidacion tipo) {
        super(mensaje);
        this.codigo = codigo;
        this.erroresCampos = List.of();
        this.tipoValidacion = tipo;
    }

    // =====================================================
    // 🔥 SUPPLIERS FUNCIONALES PARA REUTILIZACIÓN
    // =====================================================

    /**
     * Supplier funcional para campo requerido
     */
    public static Supplier<OrdenValidationException> campoRequerido(String campo) {
        return () -> new OrdenValidationException(
                "VALIDATION_REQUIRED_001",
                new CampoError(campo, null, "Campo obligatorio", "Valor no nulo"),
                TipoValidacion.REQUERIDO
        );
    }

    /**
     * Supplier funcional para formato inválido
     */
    public static Supplier<OrdenValidationException> formatoInvalido(String campo, Object valor, String formato) {
        return () -> new OrdenValidationException(
                "VALIDATION_FORMAT_001",
                new CampoError(campo, valor, "Formato inválido", formato),
                TipoValidacion.FORMATO
        );
    }

    /**
     * Supplier funcional para rango inválido
     */
    public static Supplier<OrdenValidationException> fueraDeRango(String campo, Object valor, String rango) {
        return () -> new OrdenValidationException(
                "VALIDATION_RANGE_001",
                new CampoError(campo, valor, "Valor fuera de rango", rango),
                TipoValidacion.RANGO
        );
    }

    /**
     * Supplier funcional para inconsistencia
     */
    public static Supplier<OrdenValidationException> inconsistente(String campo1, Object valor1, String campo2, Object valor2) {
        return () -> new OrdenValidationException(
                "VALIDATION_CONSISTENCY_001",
                String.format("Inconsistencia entre %s (%s) y %s (%s)", campo1, valor1, campo2, valor2),
                TipoValidacion.CONSISTENCIA
        );
    }

    /**
     * Supplier funcional para múltiples errores
     */
    public static Supplier<OrdenValidationException> multipleErrores(List<CampoError> errores) {
        return () -> new OrdenValidationException(
                "VALIDATION_MULTIPLE_001",
                errores,
                TipoValidacion.LOGICA
        );
    }

    // =====================================================
    // 🔥 MÉTODOS FUNCIONALES CON JAVA 17
    // =====================================================

    /**
     * 🔥 SWITCH EXPRESSION: Obtiene severidad del error
     */
    public String getSeveridad() {
        return switch (tipoValidacion) {
            case REQUERIDO, INTEGRIDAD -> "HIGH";
            case FORMATO, RANGO -> "MEDIUM";
            case LOGICA, CONSISTENCIA -> "LOW";
        };
    }

    /**
     * Pattern matching estilo - Verifica si tiene múltiples errores
     */
    public boolean tieneMultiplesErrores() {
        return erroresCampos.size() > 1;
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene campos con error
     */
    public List<String> getCamposConError() {
        return erroresCampos.stream()
                .map(CampoError::campo)
                .distinct()
                .toList(); // Java 16+
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Mensaje para el usuario
     */
    public String getMensajeUsuario() {
        return switch (tipoValidacion) {
            case REQUERIDO -> """
                ❌ Campos obligatorios faltantes
                %s
                
                Complete todos los campos marcados como obligatorios.
                """.formatted(getListaErrores()).trim();

            case FORMATO -> """
                📝 Formato de datos incorrecto
                %s
                
                Verifique que los datos tengan el formato correcto.
                """.formatted(getListaErrores()).trim();

            case RANGO -> """
                📊 Valores fuera del rango permitido
                %s
                
                Ajuste los valores dentro de los rangos válidos.
                """.formatted(getListaErrores()).trim();

            case LOGICA -> """
                🧠 Error de validación lógica
                %s
                
                Los datos no cumplen con las reglas del sistema.
                """.formatted(getListaErrores()).trim();

            case CONSISTENCIA -> """
                🔄 Datos inconsistentes
                %s
                
                Revise que todos los datos sean coherentes entre sí.
                """.formatted(getListaErrores()).trim();

            case INTEGRIDAD -> """
                🔗 Error de integridad de datos
                %s
                
                Algunos datos referencian registros que no existen.
                """.formatted(getListaErrores()).trim();
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Lista de errores formateada
     */
    private String getListaErrores() {
        if (erroresCampos.isEmpty()) {
            return getMessage();
        }

        return erroresCampos.stream()
                .map(CampoError::toUserMessage)
                .map(msg -> "• " + msg)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("Sin errores específicos");
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene sugerencias de solución
     */
    public String getSugerenciasSolucion() {
        return switch (tipoValidacion) {
            case REQUERIDO -> """
                • Complete todos los campos obligatorios antes de continuar
                • Verifique que no haya campos vacíos en el formulario
                • Consulte la documentación para campos requeridos
                """;

            case FORMATO -> """
                • Verifique los formatos de fecha (YYYY-MM-DD)
                • Confirme que los números estén en formato correcto
                • Revise que los códigos sigan el patrón establecido
                """;

            case RANGO -> """
                • Ajuste las cantidades dentro de los límites permitidos
                • Verifique que las fechas estén en rangos válidos
                • Confirme que los valores numéricos sean positivos
                """;

            case LOGICA -> """
                • Revise las reglas de negocio del sistema médico
                • Consulte los protocolos de atención establecidos
                • Verifique la consistencia lógica de los datos
                """;

            case CONSISTENCIA -> """
                • Asegúrese de que todos los datos sean coherentes
                • Verifique las relaciones entre campos dependientes
                • Confirme que no hay contradicciones en los datos
                """;

            case INTEGRIDAD -> """
                • Verifique que los IDs referenciados existan
                • Confirme que los pacientes y médicos estén activos
                • Valide que los exámenes estén disponibles
                """;
        };
    }

    // =====================================================
    // 🔥 MÉTODOS HELPER PRIVADOS
    // =====================================================

    /**
     * Genera mensaje compuesto para múltiples errores
     */
    private static String generarMensajeCompuesto(List<CampoError> errores, TipoValidacion tipo) {
        if (errores.isEmpty()) {
            return "Error de validación: " + tipo.getDescripcion();
        }

        if (errores.size() == 1) {
            return errores.get(0).toUserMessage();
        }

        return String.format("Se encontraron %d errores de validación (%s)",
                errores.size(), tipo.getDescripcion());
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para logging estructurado
     */
    public String toLogString() {
        return """
               OrdenValidationException {
                 codigo: %s,
                 tipo: %s,
                 severidad: %s,
                 errores: %d,
                 campos: %s
               }
               """.formatted(
                codigo,
                tipoValidacion,
                getSeveridad(),
                erroresCampos.size(),
                getCamposConError()
        );
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para respuestas de API estructuradas
     */
    public Map<String, Object> toApiResponse() {
        return Map.of(
                "codigo", codigo,
                "tipo", tipoValidacion.name(),
                "severidad", getSeveridad(),
                "mensaje", getMensajeUsuario(),
                "errores", erroresCampos.stream()
                        .map(error -> Map.of(
                                "campo", error.campo(),
                                "valor", error.valorActual(),
                                "mensaje", error.mensaje(),
                                "esperado", error.valorEsperado()
                        ))
                        .toList(),
                "sugerencias", getSugerenciasSolucion()
        );
    }
}