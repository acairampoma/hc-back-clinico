package com.formacionbdi.microservicios.app.receta.models.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 🚀 INFO: Receta básica para listas/UI - JAVA 17 RECORD
 * ✅ Immutable por defecto
 * ✅ UI helpers para listas
 * ✅ Formateo de fechas
 * ✅ Type safety garantizado
 */
public record RecetaBasicaInfo(
        Long id,
        String numeroReceta,
        String pacienteNombre,
        String medicoNombre,
        String estado,
        String estadoDescripcion,
        LocalDateTime fechaReceta,
        boolean firmada,
        int totalMedicamentos
) {

    // =====================================================
    // 🔥 CONSTRUCTOR COMPACTO CON VALIDACIONES
    // =====================================================
    public RecetaBasicaInfo {
        // Validaciones básicas
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID debe ser positivo");
        }

        if (numeroReceta == null || numeroReceta.trim().isEmpty()) {
            throw new IllegalArgumentException("Número de receta requerido");
        }

        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("Estado requerido");
        }

        if (totalMedicamentos < 0) {
            throw new IllegalArgumentException("Total medicamentos no puede ser negativo");
        }

        // Normalizar campos
        numeroReceta = numeroReceta.trim();
        pacienteNombre = pacienteNombre != null ? pacienteNombre.trim() : "Paciente no especificado";
        medicoNombre = medicoNombre != null ? medicoNombre.trim() : "Médico no especificado";
        estadoDescripcion = estadoDescripcion != null ? estadoDescripcion.trim() : obtenerDescripcionEstado(estado);
    }

    // =====================================================
    // 🔥 FACTORY METHODS
    // =====================================================

    /**
     * Factory method completo
     */
    public static RecetaBasicaInfo crear(Long id, String numeroReceta, String pacienteNombre,
                                         String medicoNombre, String estado, LocalDateTime fechaReceta,
                                         boolean firmada, int totalMedicamentos) {
        return new RecetaBasicaInfo(
                id, numeroReceta, pacienteNombre, medicoNombre,
                estado, null, fechaReceta, firmada, totalMedicamentos
        );
    }

    /**
     * Factory method mínimo (para listas rápidas)
     */
    public static RecetaBasicaInfo minimo(Long id, String numeroReceta, String estado, LocalDateTime fechaReceta) {
        return new RecetaBasicaInfo(
                id, numeroReceta, null, null, estado, null, fechaReceta, false, 0
        );
    }

    // =====================================================
    // 🔥 HELPER METHODS DE ESTADO
    // =====================================================

    /**
     * Verifica si es una receta activa
     */
    public boolean esActiva() {
        return "01".equals(estado);
    }

    /**
     * Verifica si está despachada
     */
    public boolean estaDespachada() {
        return "02".equals(estado);
    }

    /**
     * Verifica si está vencida
     */
    public boolean estaVencida() {
        return "03".equals(estado);
    }

    /**
     * Verifica si está anulada
     */
    public boolean estaAnulada() {
        return "04".equals(estado);
    }

    /**
     * Verifica si está firmada
     */
    public boolean estaFirmada() {
        return firmada;
    }

    /**
     * Verifica si tiene medicamentos
     */
    public boolean tieneMedicamentos() {
        return totalMedicamentos > 0;
    }

    // =====================================================
    // 🔥 UI HELPERS
    // =====================================================

    /**
     * Color de badge según estado
     */
    public String badgeColor() {
        return switch (estado) {
            case "01" -> "success";   // Verde - Activa
            case "02" -> "primary";   // Azul - Despachada
            case "03" -> "warning";   // Amarillo - Vencida
            case "04" -> "danger";    // Rojo - Anulada
            default -> "secondary";   // Gris - Desconocido
        };
    }

    /**
     * Icono según estado
     */
    public String iconoEstado() {
        return switch (estado) {
            case "01" -> firmada ? "✅" : "⏳";  // Activa firmada/sin firmar
            case "02" -> "📦";                  // Despachada
            case "03" -> "⏰";                  // Vencida
            case "04" -> "❌";                  // Anulada
            default -> "❓";                    // Desconocido
        };
    }

    /**
     * Clase CSS para la fila
     */
    public String classCssRow() {
        var base = "receta-row";

        if (!esActiva()) base += " receta-inactiva";
        if (!firmada && esActiva()) base += " receta-sin-firmar";
        if (estaVencida()) base += " receta-vencida";
        if (estaAnulada()) base += " receta-anulada";

        return base;
    }

    /**
     * Prioridad visual (para ordenamiento UI)
     */
    public int prioridadVisual() {
        // Activas sin firmar = prioridad 1 (más alta)
        if (esActiva() && !firmada) return 1;
        // Activas firmadas = prioridad 2
        if (esActiva() && firmada) return 2;
        // Vencidas = prioridad 3
        if (estaVencida()) return 3;
        // Despachadas = prioridad 4
        if (estaDespachada()) return 4;
        // Anuladas = prioridad 5 (más baja)
        if (estaAnulada()) return 5;
        return 6;
    }

    // =====================================================
    // 🔥 FORMATEO DE FECHAS
    // =====================================================

    /**
     * Fecha formateada para UI
     */
    public String fechaFormateada() {
        if (fechaReceta == null) return "Fecha no disponible";
        return fechaReceta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Fecha corta (solo fecha)
     */
    public String fechaCorta() {
        if (fechaReceta == null) return "N/A";
        return fechaReceta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Tiempo relativo (hace cuánto)
     */
    public String tiempoRelativo() {
        if (fechaReceta == null) return "Fecha desconocida";

        var ahora = LocalDateTime.now();
        var diferencia = ChronoUnit.HOURS.between(fechaReceta, ahora);

        if (diferencia < 1) return "Hace menos de 1 hora";
        if (diferencia < 24) return "Hace " + diferencia + " hora" + (diferencia > 1 ? "s" : "");

        var dias = ChronoUnit.DAYS.between(fechaReceta, ahora);
        if (dias < 7) return "Hace " + dias + " día" + (dias > 1 ? "s" : "");
        if (dias < 30) {
            var semanas = dias / 7;
            return "Hace " + semanas + " semana" + (semanas > 1 ? "s" : "");
        }

        var meses = ChronoUnit.MONTHS.between(fechaReceta, ahora);
        return "Hace " + meses + " mes" + (meses > 1 ? "es" : "");
    }

    // =====================================================
    // 🔥 INFORMACIÓN COMPLETA
    // =====================================================

    /**
     * Título para card/lista
     */
    public String titulo() {
        return numeroReceta + " - " + pacienteNombre;
    }

    /**
     * Subtítulo con información clave
     */
    public String subtitulo() {
        return String.format("%s | %s | %d medicamento%s",
                medicoNombre,
                estadoDescripcion,
                totalMedicamentos,
                totalMedicamentos != 1 ? "s" : "");
    }

    /**
     * Resumen completo para tooltip
     */
    public String resumenCompleto() {
        return """
            📋 Receta: %s
            👤 Paciente: %s
            👨‍⚕️ Médico: %s
            📊 Estado: %s %s
            📅 Fecha: %s
            💊 Medicamentos: %d
            ✍️ Firmada: %s
            """.formatted(
                numeroReceta,
                pacienteNombre,
                medicoNombre,
                iconoEstado(), estadoDescripcion,
                fechaFormateada(),
                totalMedicamentos,
                firmada ? "Sí" : "No"
        );
    }

    /**
     * Información para búsqueda
     */
    public String textoBusqueda() {
        return String.join(" ",
                        numeroReceta,
                        pacienteNombre,
                        medicoNombre,
                        estadoDescripcion)
                .toLowerCase();
    }

    /**
     * Coincide con término de búsqueda
     */
    public boolean coincideConBusqueda(String termino) {
        if (termino == null || termino.trim().isEmpty()) return true;
        return textoBusqueda().contains(termino.toLowerCase().trim());
    }

    // =====================================================
    // 🔥 MÉTODOS PRIVADOS HELPER
    // =====================================================

    /**
     * Obtiene descripción del estado por código
     */
    private static String obtenerDescripcionEstado(String estado) {
        return switch (estado) {
            case "01" -> "Activa";
            case "02" -> "Despachada";
            case "03" -> "Vencida";
            case "04" -> "Anulada";
            default -> "Estado desconocido";
        };
    }

    // =====================================================
    // 🔥 VALIDATION HELPERS
    // =====================================================

    /**
     * Verifica si la receta es válida para mostrar
     */
    public boolean esValidaParaMostrar() {
        return id > 0 &&
                numeroReceta != null &&
                !numeroReceta.trim().isEmpty() &&
                estado != null;
    }

    /**
     * Requiere atención especial
     */
    public boolean requiereAtencion() {
        return (esActiva() && !firmada) || estaVencida();
    }

    /**
     * Mensaje de atención
     */
    public String mensajeAtencion() {
        if (esActiva() && !firmada) return "Receta activa pendiente de firma";
        if (estaVencida()) return "Receta vencida";
        return "";
    }

    // =====================================================
    // 🔥 CONSTANTES
    // =====================================================

    public static final String ESTADO_ACTIVA = "01";
    public static final String ESTADO_DESPACHADA = "02";
    public static final String ESTADO_VENCIDA = "03";
    public static final String ESTADO_ANULADA = "04";
}