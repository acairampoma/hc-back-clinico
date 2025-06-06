package com.formacionbdi.microservicios.app.orden.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO para Orden Detalle - CORREGIDO MAPEO JSON
 * ✅ CamelCase consistente con el JSON de entrada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdenDetDTO {

    // ===== CAMPOS PRINCIPALES =====
    @JsonProperty("id")
    private Long id;

    @NotNull(message = "El examen ID es obligatorio")
    @JsonProperty("examenId")  // ✅ CORREGIDO: CamelCase
    private Long examenId;

    @JsonProperty("cant")  // ✅ CORRECTO
    private Integer cant;

    @JsonProperty("desIndicacion")  // ✅ CORREGIDO: CamelCase
    private String desIndicacion;

    @JsonProperty("desConsideraciones")  // ✅ CORREGIDO: CamelCase
    private String desConsideraciones;

    // ===== CAMPOS AUXILIARES =====
    @JsonProperty("ordenItem")
    private Integer ordenItem;

    @JsonProperty("nomExamen")
    private String nomExamen;

    @JsonProperty("categoria")
    private String categoria;

    // ===== AUDITORÍA =====
    @JsonProperty("creadoPor")
    private Long creadoPor;

    @JsonProperty("creadoEn")
    private LocalDateTime creadoEn;

    /**
     * Obtiene resumen del examen para logs
     */
    public String getResumenExamen() {
        return String.format("ExamenID:%s - Cant:%s", examenId, cant);
    }
}