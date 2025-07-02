package com.formacionbdi.microservicios.app.receta.models.dto;

import java.util.Optional;

/**
 * 🚀 RESUMEN: Medicamento para combo/dropdown/UI - JAVA 17 RECORD
 * ✅ Immutable por defecto
 * ✅ UI helpers incluidos
 * ✅ Búsqueda optimizada
 * ✅ Type safety garantizado
 */
public record MedicamentoResumen(
        Long id,
        String codigo,
        String nombre,
        String categoria,
        String concentracion,
        boolean disponible
) {

    // =====================================================
    // 🔥 CONSTRUCTOR COMPACTO CON VALIDACIONES
    // =====================================================
    public MedicamentoResumen {
        // Validaciones básicas
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID debe ser positivo");
        }

        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("Código requerido");
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre requerido");
        }

        // Normalizar campos
        codigo = codigo.trim().toUpperCase();
        nombre = nombre.trim();
        categoria = categoria != null ? categoria.trim() : "Sin categoría";
        concentracion = concentracion != null ? concentracion.trim() : "";
    }

    // =====================================================
    // 🔥 FACTORY METHODS
    // =====================================================

    /**
     * Factory method desde entity completa
     */
    public static MedicamentoResumen fromEntity(Long id, String codigo, String nombre,
                                                String categoria, String concentracion, String disponible) {
        return new MedicamentoResumen(
                id,
                codigo,
                nombre,
                categoria,
                concentracion,
                "S".equals(disponible)
        );
    }

    /**
     * Factory method mínimo (solo campos esenciales)
     */
    public static MedicamentoResumen minimo(Long id, String codigo, String nombre) {
        return new MedicamentoResumen(id, codigo, nombre, "General", "", true);
    }

    /**
     * Factory method para medicamento no disponible
     */
    public static MedicamentoResumen noDisponible(Long id, String codigo, String nombre) {
        return new MedicamentoResumen(id, codigo, nombre, "No disponible", "", false);
    }

    // =====================================================
    // 🔥 UI HELPERS
    // =====================================================

    /**
     * Texto completo para combo/dropdown
     */
    public String textoCombo() {
        var texto = new StringBuilder();
        texto.append(codigo).append(" - ").append(nombre);

        // Agregar concentración si existe
        if (concentracion != null && !concentracion.trim().isEmpty()) {
            texto.append(" ").append(concentracion);
        }

        return texto.toString();
    }

    /**
     * Texto corto para listas compactas
     */
    public String textoCorto() {
        return codigo + " - " + nombre;
    }

    /**
     * Clase CSS según disponibilidad
     */
    public String claseCss() {
        if (!disponible) return "medicamento-no-disponible";

        return switch (categoria.toLowerCase()) {
            case "controlado" -> "medicamento-controlado";
            case "antibiótico", "antibiotico" -> "medicamento-antibiotico";
            case "analgésico", "analgesico" -> "medicamento-analgesico";
            default -> "medicamento-disponible";
        };
    }

    /**
     * Color para badge/etiqueta
     */
    public String colorBadge() {
        if (!disponible) return "secondary";

        return switch (categoria.toLowerCase()) {
            case "controlado" -> "danger";
            case "antibiótico", "antibiotico" -> "warning";
            case "analgésico", "analgesico" -> "success";
            case "cardiovascular" -> "primary";
            case "gastrointestinal" -> "info";
            default -> "light";
        };
    }

    /**
     * Icono para UI
     */
    public String icono() {
        if (!disponible) return "🚫";

        return switch (categoria.toLowerCase()) {
            case "controlado" -> "⚠️";
            case "antibiótico", "antibiotico" -> "🦠";
            case "analgésico", "analgesico" -> "💊";
            case "cardiovascular" -> "❤️";
            case "gastrointestinal" -> "🥄";
            case "respiratorio" -> "🫁";
            case "neurológico", "neurologico" -> "🧠";
            default -> "💉";
        };
    }

    // =====================================================
    // 🔥 BÚSQUEDA Y FILTROS
    // =====================================================

    /**
     * Texto para búsqueda (sin acentos, minúsculas)
     */
    public String textoBusqueda() {
        var texto = String.join(" ", codigo, nombre, categoria, concentracion);
        return texto.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n");
    }

    /**
     * Verifica si coincide con término de búsqueda
     */
    public boolean coincideConBusqueda(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return true;
        }

        var terminoLimpio = termino.toLowerCase().trim()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n");

        return textoBusqueda().contains(terminoLimpio);
    }

    /**
     * Coincidencia exacta con código
     */
    public boolean coincideCodigoExacto(String codigoBuscado) {
        return codigo.equalsIgnoreCase(codigoBuscado);
    }

    /**
     * Pertenece a categoría específica
     */
    public boolean perteneceACategoria(String categoriaBuscada) {
        return categoria.equalsIgnoreCase(categoriaBuscada);
    }

    // =====================================================
    // 🔥 INFORMATION HELPERS
    // =====================================================

    /**
     * Información completa para tooltip
     */
    public String infoCompleta() {
        return """
            %s
            📋 Código: %s
            🏷️ Categoría: %s
            💊 Concentración: %s
            📊 Estado: %s
            """.formatted(
                nombre,
                codigo,
                categoria,
                concentracion.isEmpty() ? "No especificada" : concentracion,
                disponible ? "Disponible" : "No disponible"
        );
    }

    /**
     * Descripción para accesibilidad (screen readers)
     */
    public String descripcionAccesibilidad() {
        return String.format(
                "Medicamento %s, código %s, categoría %s, %s",
                nombre,
                codigo,
                categoria,
                disponible ? "disponible" : "no disponible"
        );
    }

    /**
     * Datos para JavaScript (frontend)
     */
    public String datosParaJS() {
        return String.format(
                """
                {
                    "id": %d,
                    "codigo": "%s",
                    "nombre": "%s",
                    "categoria": "%s",
                    "concentracion": "%s",
                    "disponible": %s,
                    "textoCombo": "%s",
                    "claseCss": "%s",
                    "icono": "%s"
                }""",
                id, codigo, nombre, categoria, concentracion, disponible,
                textoCombo(), claseCss(), icono()
        );
    }

    // =====================================================
    // 🔥 VALIDATION HELPERS
    // =====================================================

    /**
     * Verifica si es apto para prescripción
     */
    public boolean aptoParaPrescripcion() {
        return disponible && id > 0 && !codigo.trim().isEmpty();
    }

    /**
     * Requiere precaución especial
     */
    public boolean requierePrecaucion() {
        return categoria.toLowerCase().contains("controlado") ||
                categoria.toLowerCase().contains("antibiótico") ||
                !disponible;
    }

    /**
     * Obtiene nivel de precaución
     */
    public String nivelPrecaucion() {
        if (!disponible) return "ALTO";
        if (categoria.toLowerCase().contains("controlado")) return "MUY_ALTO";
        if (categoria.toLowerCase().contains("antibiótico")) return "MEDIO";
        return "BAJO";
    }

    // =====================================================
    // 🔥 CONSTANTES
    // =====================================================

    public static final String CATEGORIA_CONTROLADO = "Controlado";
    public static final String CATEGORIA_ANTIBIOTICO = "Antibiótico";
    public static final String CATEGORIA_ANALGESICO = "Analgésico";
    public static final String CATEGORIA_CARDIOVASCULAR = "Cardiovascular";

    /**
     * Verifica si pertenece a categorías críticas
     */
    public boolean esCategoriaEspecial() {
        var catLower = categoria.toLowerCase();
        return catLower.contains("controlado") ||
                catLower.contains("antibiótico") ||
                catLower.contains("cardiovascular");
    }
}