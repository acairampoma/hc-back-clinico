package com.formacionbdi.microservicios.app.receta.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Future;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * 📋 DTO para Receta Cabecera - Incluye validaciones y reglas de negocio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecetaCabDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("numero_receta")
    private String numeroReceta;

    @NotNull(message = "Paciente ID es requerido")
    @JsonProperty("paciente_id")
    private Long pacienteId;

    @NotNull(message = "Médico ID es requerido")
    @JsonProperty("medico_id")
    private Long medicoId;

    @NotBlank(message = "Tipo de origen es requerido")
    @Size(max = 3, message = "Tipo origen debe ser de 3 caracteres")
    @JsonProperty("tipo_origen")
    private String tipoOrigen; // ACT=Acto Médico, HOS=Hospitalización

    @NotNull(message = "Origen ID es requerido")
    @JsonProperty("origen_id")
    private Long origenId;

    @JsonProperty("fecha_receta")
    private LocalDateTime fechaReceta;

    @Future(message = "Fecha de vencimiento debe ser futura")
    @JsonProperty("fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Size(max = 10, message = "Diagnóstico principal no puede exceder 10 caracteres")
    @JsonProperty("diagnostico_principal")
    private String diagnosticoPrincipal; // CIE-10

    @JsonProperty("indicaciones_generales")
    private String indicacionesGenerales;

    @JsonProperty("estado")
    private String estado; // 01=Activa, 02=Despachada, 03=Vencida, 04=Anulada

    @JsonProperty("firmada")
    private String firmada; // S/N

    @JsonProperty("fecha_firma")
    private LocalDateTime fechaFirma;

    @JsonProperty("firma_digital")
    private JsonNode firmaDigital;

    // ===== LISTA DE MEDICAMENTOS (CAB+DET) =====
    @Valid
    @JsonProperty("medicamentos")
    private List<RecetaDetDTO> medicamentos;

    // ===== AUDITORÍA =====
    @NotNull(message = "Creado por es requerido")
    @JsonProperty("creado_por")
    private Long creadoPor;

    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    @JsonProperty("actualizado_por")
    private Long actualizadoPor;

    @JsonProperty("actualizado_en")
    private LocalDateTime actualizadoEn;

    // ===== MÉTODOS HELPER =====

    /**
     * Verifica si la receta está firmada
     */
    public boolean estaFirmada() {
        return "S".equals(firmada);
    }

    /**
     * Verifica si es una receta activa
     */
    public boolean esActiva() {
        return "01".equals(estado);
    }

    /**
     * Verifica si es de hospitalización
     */
    public boolean esDeHospitalizacion() {
        return "HOS".equals(tipoOrigen);
    }

    /**
     * Verifica si es de acto médico
     */
    public boolean esDeActoMedico() {
        return "ACT".equals(tipoOrigen);
    }

    /**
     * Obtiene el total de medicamentos
     */
    public int getTotalMedicamentos() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    /**
     * Verifica si tiene firma digital válida
     */
    public boolean tieneFirmaDigitalValida() {
        return firmaDigital != null &&
                firmaDigital.has("tiene_firma") &&
                firmaDigital.get("tiene_firma").asBoolean();
    }

    /**
     * Verifica si puede ser modificada (hasta 24h antes de vencimiento)
     */
    public boolean puedeSerModificada() {
        if (fechaVencimiento == null) return false;

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = fechaVencimiento.atStartOfDay().minusHours(24);

        return ahora.isBefore(limite) && esActiva();
    }
}