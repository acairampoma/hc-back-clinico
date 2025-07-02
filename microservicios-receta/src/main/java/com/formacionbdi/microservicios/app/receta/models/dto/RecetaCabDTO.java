package com.formacionbdi.microservicios.app.receta.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 🇵🇪 DTO COMPLETO para RecetaCab - Alan Cairampoma
 * ✅ INCLUYE fechaVencimiento como LocalDate
 * ✅ TODOS los campos necesarios para CREATE y UPDATE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecetaCabDTO {

    private Long id;

    @JsonProperty("numero_receta")
    private String numeroReceta;

    @NotNull(message = "Paciente ID es obligatorio")
    @JsonProperty("paciente_id")
    private Long pacienteId;

    @NotNull(message = "Médico ID es obligatorio")
    @JsonProperty("medico_id")
    private Long medicoId;

    @NotBlank(message = "Tipo origen es obligatorio")
    @Size(max = 3, message = "Tipo origen máximo 3 caracteres")
    @JsonProperty("tipo_origen")
    private String tipoOrigen;

    @NotNull(message = "Origen ID es obligatorio")
    @JsonProperty("origen_id")
    private Long origenId;

    @JsonProperty("fecha_receta")
    private LocalDateTime fechaReceta;

    // 🔥 AGREGADO: fechaVencimiento como LocalDate
    @JsonProperty("fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Size(max = 10, message = "Diagnóstico principal máximo 10 caracteres")
    @JsonProperty("diagnostico_principal")
    private String diagnosticoPrincipal;

    @JsonProperty("indicaciones_generales")
    private String indicacionesGenerales;

    // ❌ QUITADO: observaciones (no existe en BD)
    // @JsonProperty("observaciones")
    // private String observaciones;

    @JsonProperty("estado")
    private String estado;

    @JsonProperty("firmada")
    private String firmada;

    @JsonProperty("fecha_firma")
    private LocalDateTime fechaFirma;

    @JsonProperty("firma_digital")
    private Object firmaDigital; // JsonNode o Map

    @JsonProperty("activo")
    private String activo;

    @JsonProperty("creado_por")
    private Long creadoPor;

    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    @JsonProperty("actualizado_por")
    private Long actualizadoPor;

    @JsonProperty("actualizado_en")
    private LocalDateTime actualizadoEn;

    // ===== MEDICAMENTOS =====
    @Valid
    @JsonProperty("medicamentos")
    private List<RecetaDetDTO> medicamentos;

    // ===== MÉTODOS HELPER =====

    public boolean tieneMedicamentos() {
        return medicamentos != null && !medicamentos.isEmpty();
    }

    public int cantidadMedicamentos() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    public boolean estaFirmada() {
        return "S".equals(firmada);
    }

    public boolean esActiva() {
        return "01".equals(estado);
    }
}