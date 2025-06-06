package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para Response de Exámenes - CORREGIDO MAPEO
 * ✅ Consistente con el JSON que espera el frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdenExamenDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("examenId")  // ✅ CORRECTO: CamelCase
    private Long examenId;

    @JsonProperty("nomExamen")
    private String nomExamen;

    @JsonProperty("categoria")
    private String categoria;

    @JsonProperty("CANT")  // ✅ MANTENER: Como en tu response original
    private Integer cant;

    @JsonProperty("des_consideraciones")  // ✅ MANTENER: Como en tu response
    private String desConsideraciones;

    @JsonProperty("des_indicacion")  // ✅ MANTENER: Como en tu response
    private String desIndicacion;

    /**
     * Obtiene descripción completa para UI
     */
    public String getDescripcionCompleta() {
        return String.format("%s (Cant: %s)", nomExamen, cant);
    }
}