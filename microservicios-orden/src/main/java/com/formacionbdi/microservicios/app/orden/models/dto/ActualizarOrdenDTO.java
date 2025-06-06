package com.formacionbdi.microservicios.app.orden.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para ACTUALIZAR Orden - CORREGIDO MAPEO JSON
 * ✅ CamelCase consistente con JSON de entrada
 * PUT /ordenes/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActualizarOrdenDTO {

    @Size(max = 10, message = "Diagnóstico no puede exceder 10 caracteres")
    @JsonProperty("diagnosticoPrincipal")  // ✅ CORREGIDO: CamelCase
    private String diagnosticoPrincipal;

    @JsonProperty("justificacionClinica")  // ✅ CORREGIDO: CamelCase
    private String justificacionClinica;

    @JsonProperty("fechaProgramada")       // ✅ CORREGIDO: CamelCase
    private LocalDate fechaProgramada;

    @Size(max = 1, message = "Prioridad debe ser E, U o N")
    @JsonProperty("prioridad")             // ✅ CORRECTO: Ya funcionaba
    private String prioridad; // E, U, N

    @JsonProperty("observaciones")         // ✅ CORRECTO
    private String observaciones;

    // ===== OPERACIONES CON EXÁMENES =====

    @JsonProperty("agregarExamenes")       // ✅ CORREGIDO: CamelCase
    private List<NuevoExamenDTO> agregarExamenes;

    @JsonProperty("modificarExamenes")     // ✅ CORREGIDO: CamelCase
    private List<ModificarExamenDTO> modificarExamenes;

    @JsonProperty("eliminarExamenes")      // ✅ CORREGIDO: CamelCase
    private List<Long> eliminarExamenes; // IDs de exámenes a eliminar

    // ===== DTOs ANIDADOS PARA EXÁMENES =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NuevoExamenDTO {
        @JsonProperty("examenId")          // ✅ CORREGIDO: CamelCase
        private Long examenId;

        @JsonProperty("codigoExamen")      // ✅ CORREGIDO: CamelCase
        private String codigoExamen;

        @JsonProperty("preparacionEspecial") // ✅ CORREGIDO: CamelCase
        private String preparacionEspecial;

        @JsonProperty("indicaciones")      // ✅ CORRECTO
        private String indicaciones;

        @JsonProperty("ordenItem")         // ✅ CORREGIDO: CamelCase
        private Integer ordenItem;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ModificarExamenDTO {
        @JsonProperty("id")                    // ← AGREGAR ESTE CAMPO
        private Long id;                       // ← ID del examen_det a modificar

        @JsonProperty("cant")              // ✅ CORRECTO
        private Integer cant;

        @JsonProperty("desIndicacion")     // ✅ CORREGIDO: CamelCase
        private String desIndicacion;

        @JsonProperty("desConsideraciones") // ✅ CORREGIDO: CamelCase
        private String desConsideraciones;
    }

    // ===== MÉTODOS HELPER =====

    public boolean tieneOperacionesExamenes() {
        return (agregarExamenes != null && !agregarExamenes.isEmpty()) ||
                (modificarExamenes != null && !modificarExamenes.isEmpty()) ||
                (eliminarExamenes != null && !eliminarExamenes.isEmpty());
    }

    public boolean soloActualizaInfo() {
        return !tieneOperacionesExamenes() &&
                (diagnosticoPrincipal != null ||
                        justificacionClinica != null ||
                        fechaProgramada != null ||
                        prioridad != null ||
                        observaciones != null);
    }
}