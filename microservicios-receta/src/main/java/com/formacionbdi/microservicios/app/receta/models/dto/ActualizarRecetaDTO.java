package com.formacionbdi.microservicios.app.receta.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Size;
import java.time.LocalDate; // ✅ CORRECTO - Solo fecha, no hora
import java.util.List;

/**
 * 🇵🇪 DTO para ACTUALIZAR Receta - CORREGIDO por Alan Cairampoma
 * ✅ fechaVencimiento es LocalDate (coherente con BD - solo fecha)
 * ✅ Sin validaciones obligatorias - Solo campos a modificar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActualizarRecetaDTO {

    @Size(max = 10, message = "Diagnóstico no puede exceder 10 caracteres")
    @JsonProperty("diagnostico_principal")
    private String diagnosticoPrincipal;

    @JsonProperty("indicaciones_generales")
    private String indicacionesGenerales;

    // 🔥 CORRECTO: LocalDate para fecha_vencimiento (solo fecha)
    @JsonProperty("fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // ❌ QUITADO: observaciones (no existe en BD)
    // @JsonProperty("observaciones")
    // private String observaciones;

    // ===== OPERACIONES CON MEDICAMENTOS =====

    @JsonProperty("agregar_medicamentos")
    private List<NuevoMedicamentoDTO> agregarMedicamentos;

    @JsonProperty("modificar_medicamentos")
    private List<ModificarMedicamentoDTO> modificarMedicamentos;

    @JsonProperty("eliminar_medicamentos")
    private List<Long> eliminarMedicamentos;

    // ===== DTOs ANIDADOS =====

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NuevoMedicamentoDTO {
        @JsonProperty("medicamento_id")
        private Long medicamentoId;

        @JsonProperty("codigo_medicamento")
        private String codigoMedicamento;

        @JsonProperty("diagnostico_medicamento")
        private String diagnosticoMedicamento;

        @JsonProperty("dosis")
        private String dosis;

        @JsonProperty("frecuencia")
        private String frecuencia;

        @JsonProperty("duracion_tratamiento")
        private String duracionTratamiento;

        @JsonProperty("cantidad_total")
        private Double cantidadTotal;

        @JsonProperty("unidad_cantidad")
        private String unidadCantidad;

        @JsonProperty("via_administracion")
        private String viaAdministracion;

        @JsonProperty("instrucciones_especiales")
        private String instruccionesEspeciales;

        @JsonProperty("con_alimentos")
        private String conAlimentos;

        @JsonProperty("momento_administracion")
        private String momentoAdministracion;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ModificarMedicamentoDTO {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("dosis")
        private String dosis;

        @JsonProperty("frecuencia")
        private String frecuencia;

        @JsonProperty("duracion_tratamiento")
        private String duracionTratamiento;

        @JsonProperty("cantidad_total")
        private Double cantidadTotal;

        @JsonProperty("instrucciones_especiales")
        private String instruccionesEspeciales;

        @JsonProperty("con_alimentos")
        private String conAlimentos;

        @JsonProperty("momento_administracion")
        private String momentoAdministracion;
    }

    // ===== MÉTODOS HELPER =====

    public boolean tieneOperacionesMedicamentos() {
        return (agregarMedicamentos != null && !agregarMedicamentos.isEmpty()) ||
                (modificarMedicamentos != null && !modificarMedicamentos.isEmpty()) ||
                (eliminarMedicamentos != null && !eliminarMedicamentos.isEmpty());
    }

    public boolean soloActualizaInfo() {
        return !tieneOperacionesMedicamentos() &&
                (diagnosticoPrincipal != null ||
                        indicacionesGenerales != null ||
                        fechaVencimiento != null);
    }
}