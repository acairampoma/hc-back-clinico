package com.formacionbdi.microservicios.app.orden.exception;

import lombok.Getter;
import java.util.function.Supplier;

/**
 * 🔬 EXCEPCIÓN: Examen no encontrado - Java 17 Functional Style
 *
 * Features Java 17:
 * - Switch expressions para contexto
 * - Suppliers funcionales para reutilización
 * - Text blocks para mensajes detallados
 * - Pattern matching simulado
 *
 * @author Microservicio Órdenes
 * @version 1.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Getter
public class ExamenNotFoundException extends RuntimeException {

    private final String codigo;
    private final Long examenId;
    private final String codigoExamen;
    private final TipoBusquedaExamen tipoBusqueda;

    /**
     * 🔥 ENUM CON JAVA 17: Tipos de búsqueda de exámenes
     */
    public enum TipoBusquedaExamen {
        POR_ID("EXAMEN_NOT_FOUND_001", "Búsqueda por ID"),
        POR_CODIGO("EXAMEN_NOT_FOUND_002", "Búsqueda por código"),
        POR_CATEGORIA("EXAMEN_NOT_FOUND_003", "Búsqueda por categoría"),
        EN_ORDEN("EXAMEN_NOT_FOUND_004", "Examen en orden"),
        PERSONALIZADA("EXAMEN_NOT_FOUND_999", "Búsqueda personalizada");

        private final String codigo;
        private final String descripcion;

        TipoBusquedaExamen(String codigo, String descripcion) {
            this.codigo = codigo;
            this.descripcion = descripcion;
        }

        public String getCodigo() { return codigo; }
        public String getDescripcion() { return descripcion; }
    }

    // =====================================================
    // 🔥 CONSTRUCTORES FUNCIONALES
    // =====================================================

    /**
     * Constructor funcional por ID - Más usado
     */
    public ExamenNotFoundException(Long examenId) {
        super(generarMensajePorId(examenId));
        this.codigo = TipoBusquedaExamen.POR_ID.getCodigo();
        this.examenId = examenId;
        this.codigoExamen = null;
        this.tipoBusqueda = TipoBusquedaExamen.POR_ID;
    }

    /**
     * Constructor funcional por código de examen
     */
    public ExamenNotFoundException(String codigoExamen, boolean esPorCodigo) {
        super(generarMensajePorCodigo(codigoExamen));
        this.codigo = TipoBusquedaExamen.POR_CODIGO.getCodigo();
        this.examenId = null;
        this.codigoExamen = codigoExamen;
        this.tipoBusqueda = TipoBusquedaExamen.POR_CODIGO;
    }

    /**
     * Constructor funcional personalizado
     */
    public ExamenNotFoundException(String mensaje, TipoBusquedaExamen tipo) {
        super(mensaje);
        this.codigo = tipo.getCodigo();
        this.examenId = null;
        this.codigoExamen = null;
        this.tipoBusqueda = tipo;
    }

    /**
     * Constructor funcional con causa raíz
     */
    public ExamenNotFoundException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = TipoBusquedaExamen.PERSONALIZADA.getCodigo();
        this.examenId = null;
        this.codigoExamen = null;
        this.tipoBusqueda = TipoBusquedaExamen.PERSONALIZADA;
    }

    // =====================================================
    // 🔥 SUPPLIERS FUNCIONALES PARA REUTILIZACIÓN
    // =====================================================

    /**
     * Supplier funcional - Más elegante en Optional.orElseThrow()
     */
    public static Supplier<ExamenNotFoundException> porId(Long examenId) {
        return () -> new ExamenNotFoundException(examenId);
    }

    /**
     * Supplier funcional para búsqueda por código
     */
    public static Supplier<ExamenNotFoundException> porCodigo(String codigoExamen) {
        return () -> new ExamenNotFoundException(codigoExamen, true);
    }

    /**
     * Supplier funcional para examen en orden específica
     */
    public static Supplier<ExamenNotFoundException> enOrden(Long ordenId, Long examenId) {
        return () -> new ExamenNotFoundException(
                String.format("Examen ID %d no encontrado en la orden %d", examenId, ordenId),
                TipoBusquedaExamen.EN_ORDEN
        );
    }

    /**
     * Supplier funcional para categoría
     */
    public static Supplier<ExamenNotFoundException> porCategoria(String categoria) {
        return () -> new ExamenNotFoundException(
                String.format("No se encontraron exámenes en la categoría: %s", categoria),
                TipoBusquedaExamen.POR_CATEGORIA
        );
    }

    /**
     * Supplier funcional genérico
     */
    public static Supplier<ExamenNotFoundException> conMensaje(String mensaje) {
        return () -> new ExamenNotFoundException(mensaje, TipoBusquedaExamen.PERSONALIZADA);
    }

    // =====================================================
    // 🔥 MÉTODOS FUNCIONALES CON JAVA 17
    // =====================================================

    /**
     * Pattern matching estilo - Verifica tipo de búsqueda
     */
    public boolean esPorId() {
        return tipoBusqueda == TipoBusquedaExamen.POR_ID && examenId != null;
    }

    /**
     * Pattern matching estilo - Verifica tipo de búsqueda
     */
    public boolean esPorCodigo() {
        return tipoBusqueda == TipoBusquedaExamen.POR_CODIGO &&
                codigoExamen != null && !codigoExamen.trim().isEmpty();
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Obtiene identificador
     */
    public String getIdentificador() {
        return switch (tipoBusqueda) {
            case POR_ID -> examenId != null ? String.valueOf(examenId) : "ID_NULO";
            case POR_CODIGO -> codigoExamen != null ? codigoExamen : "CODIGO_NULO";
            case POR_CATEGORIA, EN_ORDEN, PERSONALIZADA -> "OTRO_CRITERIO";
        };
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Obtiene contexto detallado
     */
    public String getContextoDetallado() {
        return switch (tipoBusqueda) {
            case POR_ID -> """
                Contexto: Búsqueda de examen por ID
                ID buscado: %s
                Posibles causas: Examen inexistente, inactivo, sin permisos
                """.formatted(getIdentificador());

            case POR_CODIGO -> """
                Contexto: Búsqueda de examen por código
                Código buscado: %s
                Posibles causas: Código incorrecto, examen descontinuado
                """.formatted(getIdentificador());

            case POR_CATEGORIA -> """
                Contexto: Búsqueda de exámenes por categoría
                Posibles causas: Categoría vacía, filtros incorrectos
                """;

            case EN_ORDEN -> """
                Contexto: Examen específico en una orden
                Posibles causas: Examen no incluido en la orden, orden incorrecta
                """;

            case PERSONALIZADA -> """
                Contexto: Búsqueda personalizada de examen
                Mensaje: %s
                """.formatted(getMessage());
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene sugerencias de solución
     */
    public String getSugerencias() {
        return switch (tipoBusqueda) {
            case POR_ID -> """
                • Verifique que el ID del examen sea correcto
                • Confirme que el examen esté activo en el sistema
                • Valide permisos de acceso al catálogo de exámenes
                • Revise si el examen fue descontinuado
                """;

            case POR_CODIGO -> """
                • Verifique el formato del código de examen
                • Confirme que el código esté actualizado
                • Busque en el catálogo completo de exámenes
                • Consulte la lista de exámenes activos
                """;

            case POR_CATEGORIA -> """
                • Verifique que la categoría exista en el sistema
                • Confirme que haya exámenes activos en esa categoría
                • Revise los filtros de búsqueda aplicados
                • Consulte el catálogo completo por categorías
                """;

            case EN_ORDEN -> """
                • Verifique que el examen esté incluido en la orden
                • Confirme que la orden sea la correcta
                • Revise si el examen fue eliminado de la orden
                • Valide permisos de acceso a la orden
                """;

            case PERSONALIZADA -> """
                • Revise los criterios de búsqueda utilizados
                • Consulte la documentación del sistema
                • Contacte al administrador si persiste el error
                """;
        };
    }

    // =====================================================
    // 🔥 MÉTODOS HELPER PRIVADOS CON TEXT BLOCKS
    // =====================================================

    /**
     * Text block Java 17 para mensaje por ID
     */
    private static String generarMensajePorId(Long examenId) {
        return """
               Examen médico no encontrado.
               ID buscado: %d
               El examen puede estar inactivo o no tener permisos de acceso.
               """.formatted(examenId).trim();
    }

    /**
     * Text block Java 17 para mensaje por código
     */
    private static String generarMensajePorCodigo(String codigoExamen) {
        return """
               Examen médico no encontrado.
               Código buscado: %s
               Verifique que el código sea correcto y el examen esté activo.
               """.formatted(codigoExamen).trim();
    }

    /**
     * 🔥 OVERRIDE CON PATTERN MATCHING: Mensaje inteligente
     */
    @Override
    public String getMessage() {
        if (super.getMessage() != null && !super.getMessage().isEmpty()) {
            return super.getMessage();
        }

        // Pattern matching simulado con switch
        return switch (tipoBusqueda) {
            case POR_ID -> generarMensajePorId(examenId);
            case POR_CODIGO -> generarMensajePorCodigo(codigoExamen);
            case POR_CATEGORIA -> "No se encontraron exámenes en la categoría especificada";
            case EN_ORDEN -> "Examen no encontrado en la orden especificada";
            case PERSONALIZADA -> "Examen no encontrado con los criterios especificados";
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para logging estructurado
     */
    public String toLogString() {
        return """
               ExamenNotFoundException {
                 codigo: %s,
                 tipoBusqueda: %s,
                 identificador: %s,
                 mensaje: %s
               }
               """.formatted(codigo, tipoBusqueda, getIdentificador(), getMessage());
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene tipo de examen esperado
     */
    public String getTipoExamenEsperado() {
        return switch (tipoBusqueda) {
            case POR_ID, EN_ORDEN -> "Examen específico";
            case POR_CODIGO -> "Examen por código";
            case POR_CATEGORIA -> "Exámenes de categoría";
            case PERSONALIZADA -> "Examen según criterios";
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Verifica si es error crítico
     */
    public boolean esCritico() {
        return tipoBusqueda == TipoBusquedaExamen.EN_ORDEN;
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para respuestas de API estructuradas
     */
    public java.util.Map<String, Object> toApiResponse() {
        return java.util.Map.of(
                "codigo", codigo,
                "tipo", tipoBusqueda.name(),
                "identificador", getIdentificador(),
                "tipoExamen", getTipoExamenEsperado(),
                "critico", esCritico(),
                "mensaje", getMessage(),
                "sugerencias", getSugerencias()
        );
    }
}