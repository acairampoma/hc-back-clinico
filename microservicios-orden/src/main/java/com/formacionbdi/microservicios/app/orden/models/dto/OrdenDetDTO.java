package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * DTO Record para detalle de examen en orden - Java 17
 * Componente de OrdenCabDTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdenDetDTO(

        @JsonProperty("id")
        Long id,

        @NotNull(message = "El examen ID es obligatorio")
        @JsonProperty("examenId")
        Long examenId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @JsonProperty("cant")
        Integer cant,

        @Size(max = 200, message = "Indicación no puede exceder 200 caracteres")
        @JsonProperty("desIndicacion")
        String desIndicacion,

        @Size(max = 200, message = "Consideraciones no puede exceder 200 caracteres")
        @JsonProperty("desConsideraciones")
        String desConsideraciones,

        @JsonProperty("ordenItem")
        Integer ordenItem,

        @JsonProperty("nomExamen")
        String nomExamen,

        @JsonProperty("categoria")
        String categoria
) {

    /**
     * Compact constructor con validaciones y valores por defecto
     */
    public OrdenDetDTO {
        // Cantidad por defecto
        if (cant == null) {
            cant = 1;
        }

        // Validar cantidad según categoría si está disponible
        if (categoria != null && !cantidadEsValidaParaCategoria(cant, categoria)) {
            throw new IllegalArgumentException(
                    String.format("Cantidad %d no válida para categoría %s", cant, categoria)
            );
        }

        // Validar cantidad máxima
        if (cant != null && cant > 10) {
            throw new IllegalArgumentException("Cantidad máxima: 10");
        }
    }

    /**
     * Factory method simple
     */
    public static OrdenDetDTO crear(Long examenId, Integer cantidad) {
        return new OrdenDetDTO(null, examenId, cantidad, null, null, null, null, null);
    }

    /**
     * Factory method con indicaciones
     */
    public static OrdenDetDTO crear(Long examenId, Integer cantidad, String indicacion, String consideraciones) {
        return new OrdenDetDTO(null, examenId, cantidad, indicacion, consideraciones, null, null, null);
    }

    /**
     * Factory method completo
     */
    public static OrdenDetDTO completo(Long examenId, Integer cantidad, String indicacion,
                                       String consideraciones, String nombreExamen, String categoria) {
        return new OrdenDetDTO(null, examenId, cantidad, indicacion, consideraciones,
                null, nombreExamen, categoria);
    }

    /**
     * Validación de cantidad según categoría
     */
    private static boolean cantidadEsValidaParaCategoria(Integer cantidad, String categoria) {
        if (cantidad == null || cantidad < 1) return false;

        return switch (categoria) {
            case "LAB" -> cantidad <= 5;   // Laboratorio: máximo 5
            case "IMG" -> cantidad <= 2;   // Imagen: máximo 2
            case "PROC" -> cantidad == 1;  // Procedimiento: solo 1
            case "FUNC" -> cantidad <= 3;  // Funcional: máximo 3
            default -> cantidad <= 3;      // Otros: máximo 3
        };
    }

    /**
     * Métodos de utilidad
     */
    public boolean tieneIndicaciones() {
        return desIndicacion != null && !desIndicacion.trim().isEmpty();
    }

    public boolean tieneConsideraciones() {
        return desConsideraciones != null && !desConsideraciones.trim().isEmpty();
    }

    public boolean esNuevo() {
        return id == null;
    }

    public int getCantidadSegura() {
        return cant != null ? cant : 1;
    }

    public String getResumenExamen() {
        var nombre = nomExamen != null ? nomExamen : "Examen ID: " + examenId;
        var cantidad = cant != null && cant > 1 ? " (x" + cant + ")" : "";
        return nombre + cantidad;
    }

    public String getCategoriaDescripcion() {
        if (categoria == null) return "Sin categoría";

        return switch (categoria) {
            case "LAB" -> "Laboratorio";
            case "IMG" -> "Imagenología";
            case "PROC" -> "Procedimiento";
            case "FUNC" -> "Prueba Funcional";
            default -> categoria;
        };
    }

    /**
     * Copia con nuevo ID (útil para updates)
     */
    public OrdenDetDTO conId(Long nuevoId) {
        return new OrdenDetDTO(nuevoId, examenId, cant, desIndicacion,
                desConsideraciones, ordenItem, nomExamen, categoria);
    }

    /**
     * Copia con nueva cantidad
     */
    public OrdenDetDTO conCantidad(Integer nuevaCantidad) {
        return new OrdenDetDTO(id, examenId, nuevaCantidad, desIndicacion,
                desConsideraciones, ordenItem, nomExamen, categoria);
    }

    /**
     * Copia con nuevas indicaciones
     */
    public OrdenDetDTO conIndicaciones(String nuevaIndicacion, String nuevasConsideraciones) {
        return new OrdenDetDTO(id, examenId, cant, nuevaIndicacion,
                nuevasConsideraciones, ordenItem, nomExamen, categoria);
    }

    /**
     * Validación completa del examen
     */
    public boolean esValido() {
        return examenId != null &&
                getCantidadSegura() >= 1 &&
                getCantidadSegura() <= 10 &&
                (categoria == null || cantidadEsValidaParaCategoria(cant, categoria));
    }
}