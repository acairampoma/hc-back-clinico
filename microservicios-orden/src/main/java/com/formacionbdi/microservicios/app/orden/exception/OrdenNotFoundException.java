package com.formacionbdi.microservicios.app.orden.exception;

import lombok.Getter;
import java.util.function.Supplier;

/**
 * 🔍 EXCEPCIÓN: Orden no encontrada - Java 17 Functional Style
 *
 * Features Java 17:
 * - Pattern matching en getMessage()
 * - Switch expressions para contexto
 * - Suppliers funcionales para reutilización
 * - Records para data inmutable
 *
 * @author Microservicio Órdenes
 * @version 1.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Getter
public class OrdenNotFoundException extends RuntimeException {

    private final String codigo;
    private final Long ordenId;
    private final String numeroOrden;
    private final TipoBusqueda tipoBusqueda;

    /**
     * 🔥 ENUM CON JAVA 17: Sealed-like behavior para tipos de búsqueda
     */
    public enum TipoBusqueda {
        POR_ID("ORDEN_NOT_FOUND_001", "Búsqueda por ID"),
        POR_NUMERO("ORDEN_NOT_FOUND_002", "Búsqueda por número"),
        POR_PACIENTE("ORDEN_NOT_FOUND_003", "Búsqueda por paciente"),
        POR_MEDICO("ORDEN_NOT_FOUND_004", "Búsqueda por médico"),
        PERSONALIZADA("ORDEN_NOT_FOUND_999", "Búsqueda personalizada");

        private final String codigo;
        private final String descripcion;

        TipoBusqueda(String codigo, String descripcion) {
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
    public OrdenNotFoundException(Long ordenId) {
        super(generarMensajePorId(ordenId));
        this.codigo = TipoBusqueda.POR_ID.getCodigo();
        this.ordenId = ordenId;
        this.numeroOrden = null;
        this.tipoBusqueda = TipoBusqueda.POR_ID;
    }

    /**
     * Constructor funcional por número de orden
     */
    public OrdenNotFoundException(String numeroOrden) {
        super(generarMensajePorNumero(numeroOrden));
        this.codigo = TipoBusqueda.POR_NUMERO.getCodigo();
        this.ordenId = null;
        this.numeroOrden = numeroOrden;
        this.tipoBusqueda = TipoBusqueda.POR_NUMERO;
    }

    /**
     * Constructor funcional personalizado con tipo
     */
    public OrdenNotFoundException(String message, TipoBusqueda tipo) {
        super(message);
        this.codigo = tipo.getCodigo();
        this.ordenId = null;
        this.numeroOrden = null;
        this.tipoBusqueda = tipo;
    }

    /**
     * Constructor funcional con causa raíz
     */
    public OrdenNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.codigo = TipoBusqueda.PERSONALIZADA.getCodigo();
        this.ordenId = null;
        this.numeroOrden = null;
        this.tipoBusqueda = TipoBusqueda.PERSONALIZADA;
    }

    // =====================================================
    // 🔥 SUPPLIERS FUNCIONALES PARA REUTILIZACIÓN
    // =====================================================

    /**
     * Supplier funcional - Más elegante en Optional.orElseThrow()
     */
    public static Supplier<OrdenNotFoundException> porId(Long ordenId) {
        return () -> new OrdenNotFoundException(ordenId);
    }

    /**
     * Supplier funcional para búsqueda por número
     */
    public static Supplier<OrdenNotFoundException> porNumero(String numeroOrden) {
        return () -> new OrdenNotFoundException(numeroOrden);
    }

    /**
     * Supplier funcional genérico
     */
    public static Supplier<OrdenNotFoundException> conMensaje(String mensaje) {
        return () -> new OrdenNotFoundException(mensaje, TipoBusqueda.PERSONALIZADA);
    }

    // =====================================================
    // 🔥 MÉTODOS FUNCIONALES CON JAVA 17
    // =====================================================

    /**
     * Pattern matching estilo - Verifica tipo de búsqueda
     */
    public boolean esPorId() {
        return tipoBusqueda == TipoBusqueda.POR_ID && ordenId != null;
    }

    /**
     * Pattern matching estilo - Verifica tipo de búsqueda
     */
    public boolean esPorNumero() {
        return tipoBusqueda == TipoBusqueda.POR_NUMERO &&
                numeroOrden != null && !numeroOrden.trim().isEmpty();
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Obtiene identificador
     */
    public String getIdentificador() {
        return switch (tipoBusqueda) {
            case POR_ID -> ordenId != null ? String.valueOf(ordenId) : "ID_NULO";
            case POR_NUMERO -> numeroOrden != null ? numeroOrden : "NUMERO_NULO";
            case POR_PACIENTE, POR_MEDICO, PERSONALIZADA -> "OTRO_CRITERIO";
        };
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Obtiene contexto detallado
     */
    public String getContextoDetallado() {
        return switch (tipoBusqueda) {
            case POR_ID -> """
                Contexto: Búsqueda por ID de orden
                ID buscado: %s
                Posibles causas: Orden eliminada, ID incorrecto, sin permisos
                """.formatted(getIdentificador());

            case POR_NUMERO -> """
                Contexto: Búsqueda por número de orden
                Número buscado: %s
                Posibles causas: Número incorrecto, orden anulada
                """.formatted(getIdentificador());

            case POR_PACIENTE -> """
                Contexto: Búsqueda por paciente
                Posibles causas: Paciente sin órdenes, filtros incorrectos
                """;

            case POR_MEDICO -> """
                Contexto: Búsqueda por médico
                Posibles causas: Médico sin órdenes, filtros incorrectos
                """;

            case PERSONALIZADA -> """
                Contexto: Búsqueda personalizada
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
                • Verifique que el ID de la orden sea correcto
                • Confirme que la orden no haya sido eliminada
                • Valide permisos de acceso a la orden
                """;

            case POR_NUMERO -> """
                • Verifique el formato del número de orden
                • Confirme que la orden no haya sido anulada
                • Busque en órdenes históricas si es necesario
                """;

            case POR_PACIENTE, POR_MEDICO -> """
                • Ajuste los filtros de búsqueda
                • Verifique el rango de fechas
                • Confirme los permisos de acceso
                """;

            case PERSONALIZADA -> """
                • Revise los criterios de búsqueda
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
    private static String generarMensajePorId(Long ordenId) {
        return """
               Orden médica no encontrada.
               ID buscado: %d
               La orden puede haber sido eliminada o no tener permisos de acceso.
               """.formatted(ordenId).trim();
    }

    /**
     * Text block Java 17 para mensaje por número
     */
    private static String generarMensajePorNumero(String numeroOrden) {
        return """
               Orden médica no encontrada.
               Número buscado: %s
               Verifique que el número sea correcto y la orden esté activa.
               """.formatted(numeroOrden).trim();
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
            case POR_ID -> generarMensajePorId(ordenId);
            case POR_NUMERO -> generarMensajePorNumero(numeroOrden);
            case POR_PACIENTE -> "No se encontraron órdenes para el paciente especificado";
            case POR_MEDICO -> "No se encontraron órdenes para el médico especificado";
            case PERSONALIZADA -> "Orden no encontrada con los criterios especificados";
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para logging estructurado
     */
    public String toLogString() {
        return """
               OrdenNotFoundException {
                 codigo: %s,
                 tipoBusqueda: %s,
                 identificador: %s,
                 mensaje: %s
               }
               """.formatted(codigo, tipoBusqueda, getIdentificador(), getMessage());
    }
}