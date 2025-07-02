package com.formacionbdi.microservicios.app.receta.models.dto;

import java.time.LocalDateTime;

/**
 * 🚀 RESULTADO: Operaciones detalladas de medicamentos (PUT específicamente) - JAVA 17 RECORD
 * ✅ Immutable por defecto
 * ✅ Cálculos automáticos
 * ✅ Helper methods para análisis
 * ✅ Type safety garantizado
 */
public record RecetaOperationResult(
        boolean success,
        String operation,
        Long recetaId,
        int eliminados,
        int modificados,
        int agregados,
        int medicamentosActivos,
        String message,
        LocalDateTime timestamp
) {

    // =====================================================
    // 🔥 CONSTRUCTOR COMPACTO CON VALIDACIONES
    // =====================================================
    public RecetaOperationResult {
        // Validaciones básicas
        if (operation == null || operation.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation requerida");
        }

        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        // Validar que los contadores no sean negativos
        if (eliminados < 0 || modificados < 0 || agregados < 0 || medicamentosActivos < 0) {
            throw new IllegalArgumentException("Los contadores no pueden ser negativos");
        }
    }

    // =====================================================
    // 🔥 FACTORY METHODS
    // =====================================================

    /**
     * Factory method para operación exitosa completa
     */
    public static RecetaOperationResult success(
            String operation,
            Long recetaId,
            int eliminados,
            int modificados,
            int agregados,
            int medicamentosActivos) {

        var message = String.format(
                "Operación completada: %d eliminados, %d modificados, %d agregados. Total activos: %d",
                eliminados, modificados, agregados, medicamentosActivos
        );

        return new RecetaOperationResult(
                true,
                operation,
                recetaId,
                eliminados,
                modificados,
                agregados,
                medicamentosActivos,
                message,
                LocalDateTime.now()
        );
    }

    /**
     * Factory method para operación sin cambios
     */
    public static RecetaOperationResult sinCambios(String operation, Long recetaId, int medicamentosActivos) {
        return new RecetaOperationResult(
                true,
                operation,
                recetaId,
                0, 0, 0,
                medicamentosActivos,
                "Operación completada sin cambios en medicamentos",
                LocalDateTime.now()
        );
    }

    /**
     * Factory method para error
     */
    public static RecetaOperationResult error(String operation, Long recetaId, String errorMessage) {
        return new RecetaOperationResult(
                false,
                operation,
                recetaId,
                0, 0, 0, 0,
                "Error: " + errorMessage,
                LocalDateTime.now()
        );
    }

    // =====================================================
    // 🔥 CÁLCULOS AUTOMÁTICOS
    // =====================================================

    /**
     * Total de operaciones realizadas
     */
    public int totalOperaciones() {
        return eliminados + modificados + agregados;
    }

    /**
     * Verifica si hubo operaciones
     */
    public boolean tuvoOperaciones() {
        return totalOperaciones() > 0;
    }

    /**
     * Verifica si solo fueron eliminaciones
     */
    public boolean soloEliminaciones() {
        return eliminados > 0 && modificados == 0 && agregados == 0;
    }

    /**
     * Verifica si solo fueron modificaciones
     */
    public boolean soloModificaciones() {
        return eliminados == 0 && modificados > 0 && agregados == 0;
    }

    /**
     * Verifica si solo fueron agregados
     */
    public boolean soloAgregados() {
        return eliminados == 0 && modificados == 0 && agregados > 0;
    }

    /**
     * Verifica si fue operación mixta
     */
    public boolean operacionMixta() {
        var tiposOperacion = 0;
        if (eliminados > 0) tiposOperacion++;
        if (modificados > 0) tiposOperacion++;
        if (agregados > 0) tiposOperacion++;
        return tiposOperacion > 1;
    }

    // =====================================================
    // 🔥 HELPER METHODS PARA ANÁLISIS
    // =====================================================

    /**
     * Resumen de operaciones para UI
     */
    public String resumenOperaciones() {
        if (!tuvoOperaciones()) {
            return "Sin cambios en medicamentos";
        }

        var partes = new java.util.ArrayList<String>();

        if (eliminados > 0) {
            partes.add(eliminados + " eliminado" + (eliminados > 1 ? "s" : ""));
        }
        if (modificados > 0) {
            partes.add(modificados + " modificado" + (modificados > 1 ? "s" : ""));
        }
        if (agregados > 0) {
            partes.add(agregados + " agregado" + (agregados > 1 ? "s" : ""));
        }

        return String.join(", ", partes);
    }

    /**
     * Descripción detallada para logs
     */
    public String descripcionDetallada() {
        return """
            RecetaOperationResult {
                success: %s,
                operation: %s,
                recetaId: %d,
                eliminados: %d,
                modificados: %d,
                agregados: %d,
                totalOperaciones: %d,
                medicamentosActivos: %d,
                tipoOperacion: %s,
                timestamp: %s
            }""".formatted(
                success,
                operation,
                recetaId != null ? recetaId : 0,
                eliminados,
                modificados,
                agregados,
                totalOperaciones(),
                medicamentosActivos,
                obtenerTipoOperacion(),
                timestamp
        );
    }

    /**
     * Obtiene el tipo de operación realizada
     */
    public String obtenerTipoOperacion() {
        if (!tuvoOperaciones()) return "SIN_CAMBIOS";
        if (soloEliminaciones()) return "SOLO_ELIMINACIONES";
        if (soloModificaciones()) return "SOLO_MODIFICACIONES";
        if (soloAgregados()) return "SOLO_AGREGADOS";
        if (operacionMixta()) return "OPERACION_MIXTA";
        return "DESCONOCIDO";
    }

    /**
     * Emoji para representar el resultado en UI
     */
    public String emojiResultado() {
        if (!success) return "❌";
        if (!tuvoOperaciones()) return "⚪";
        if (soloAgregados()) return "➕";
        if (soloEliminaciones()) return "➖";
        if (soloModificaciones()) return "✏️";
        if (operacionMixta()) return "🔄";
        return "✅";
    }

    /**
     * Color para badge en UI
     */
    public String colorBadge() {
        if (!success) return "danger";
        if (!tuvoOperaciones()) return "secondary";
        if (soloAgregados()) return "success";
        if (soloEliminaciones()) return "warning";
        if (soloModificaciones()) return "info";
        if (operacionMixta()) return "primary";
        return "light";
    }

    /**
     * Mensaje corto para notificaciones
     */
    public String mensajeCorto() {
        if (!success) return "Error en operación";
        if (!tuvoOperaciones()) return "Sin cambios";

        return switch (obtenerTipoOperacion()) {
            case "SOLO_ELIMINACIONES" -> eliminados + " medicamento" + (eliminados > 1 ? "s eliminados" : " eliminado");
            case "SOLO_MODIFICACIONES" -> modificados + " medicamento" + (modificados > 1 ? "s modificados" : " modificado");
            case "SOLO_AGREGADOS" -> agregados + " medicamento" + (agregados > 1 ? "s agregados" : " agregado");
            case "OPERACION_MIXTA" -> "Múltiples cambios realizados";
            default -> "Operación completada";
        };
    }

    // =====================================================
    // 🔥 CONSTANTES
    // =====================================================

    public static final String OPERATION_PUT = "PUT";
    public static final String OPERATION_PATCH = "PATCH";

    public static final int MAX_MEDICAMENTOS_POR_RECETA = 10;
    public static final int MIN_MEDICAMENTOS_POR_RECETA = 1;

    /**
     * Valida si el número de medicamentos activos está en rango válido
     */
    public boolean medicamentosEnRangoValido() {
        return medicamentosActivos >= MIN_MEDICAMENTOS_POR_RECETA &&
                medicamentosActivos <= MAX_MEDICAMENTOS_POR_RECETA;
    }
}