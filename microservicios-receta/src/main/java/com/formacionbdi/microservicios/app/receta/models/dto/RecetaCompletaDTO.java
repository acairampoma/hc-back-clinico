package com.formacionbdi.microservicios.app.receta.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * 📋 DTO para Receta Completa - Vista JSON optimizada
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecetaCompletaDTO {

    @JsonProperty("receta_info")
    private RecetaInfoDTO recetaInfo;

    @JsonProperty("paciente")
    private PacienteBasicoDTO paciente;

    @JsonProperty("medico")
    private MedicoBasicoDTO medico;

    @JsonProperty("medicamentos")
    private List<RecetaDetDTO> medicamentos;

    @JsonProperty("firma_digital")
    private JsonNode firmaDigital;

    // ===== DTOs ANIDADOS =====

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecetaInfoDTO {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("numero_receta")
        private String numeroReceta;

        @JsonProperty("fecha_receta")
        private LocalDateTime fechaReceta;

        @JsonProperty("fecha_vencimiento")
        private LocalDate fechaVencimiento;

        @JsonProperty("estado")
        private String estado;

        @JsonProperty("estado_descripcion")
        private String estadoDescripcion;

        @JsonProperty("diagnostico_principal")
        private String diagnosticoPrincipal;

        @JsonProperty("indicaciones_generales")
        private String indicacionesGenerales;

        @JsonProperty("firmada")
        private String firmada;

        @JsonProperty("tipo_origen")
        private String tipoOrigen;

        @JsonProperty("origen_id")
        private Long origenId;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PacienteBasicoDTO {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("nombre_completo")
        private String nombreCompleto;

        @JsonProperty("numero_documento")
        private String numeroDocumento;

        @JsonProperty("hc_numero")
        private String hcNumero;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MedicoBasicoDTO {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("nombre_completo")
        private String nombreCompleto;

        @JsonProperty("cmp")
        private String cmp;

        @JsonProperty("especialidad")
        private String especialidad;
    }
}