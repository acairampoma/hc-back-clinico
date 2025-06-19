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
 * 🔐 VERSIÓN CORREGIDA: Procesamiento correcto de firma digital del frontend
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

    @JsonProperty("observaciones")
    private String observaciones;

    @JsonProperty("estado")
    private String estado; // 01=Activa, 02=Despachada, 03=Vencida, 04=Anulada

    @JsonProperty("activo")
    private String activo; // S/N

    // ===== 🔐 CAMPOS DE FIRMA DIGITAL =====

    @JsonProperty("firmada")
    private String firmada; // S/N

    @JsonProperty("fecha_firma")
    private LocalDateTime fechaFirma;

    /**
     * 🎯 CAMPO CRÍTICO: Firma Digital como JsonNode
     * ✅ Recibe el JSON exacto del frontend con estructura:
     * {
     *   "imagen_base64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA...",
     *   "fecha_firma": "2025-06-15T05:23:19.518Z",
     *   "metodo": "canvas_signaturePad_directo",
     *   "version": "corregida_v1"
     * }
     */
    @JsonProperty("firma_digital")  // ← Snake_case como envía el frontend
    private JsonNode firmaDigital;

    // ===== 💊 LISTA DE MEDICAMENTOS (CAB+DET) =====

    @Valid
    @JsonProperty("medicamentos")
    private List<RecetaDetDTO> medicamentos;

    // ===== 📊 AUDITORÍA =====

    @NotNull(message = "Creado por es requerido")
    @JsonProperty("creado_por")
    private Long creadoPor;

    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    @JsonProperty("actualizado_por")
    private Long actualizadoPor;

    @JsonProperty("actualizado_en")
    private LocalDateTime actualizadoEn;

    // ===== 🛠️ MÉTODOS HELPER =====

    /**
     * ✅ Verifica si la receta está firmada
     */
    public boolean estaFirmada() {
        return "S".equals(firmada);
    }

    /**
     * ✅ Verifica si es una receta activa
     */
    public boolean esActiva() {
        return "01".equals(estado);
    }

    /**
     * ✅ Verifica si es de hospitalización
     */
    public boolean esDeHospitalizacion() {
        return "HOS".equals(tipoOrigen);
    }

    /**
     * ✅ Verifica si es de acto médico
     */
    public boolean esDeActoMedico() {
        return "ACT".equals(tipoOrigen);
    }

    /**
     * ✅ Obtiene el total de medicamentos
     */
    public int getTotalMedicamentos() {
        return medicamentos != null ? medicamentos.size() : 0;
    }

    /**
     * 🔐 MÉTODO CORREGIDO: Detecta la firma como la envía el frontend
     * ✅ ANTES: Buscaba "tiene_firma" (campo de notas médicas)
     * ✅ AHORA: Busca "imagen_base64" (como envía el frontend de recetas)
     */
    public boolean tieneFirmaDigitalValida() {
        return firmaDigital != null &&
                firmaDigital.has("imagen_base64") &&
                !firmaDigital.get("imagen_base64").asText().trim().isEmpty() &&
                firmaDigital.get("imagen_base64").asText().length() > 100;
    }

    /**
     * ✅ Verifica si puede ser modificada (hasta 24h antes de vencimiento)
     */
    public boolean puedeSerModificada() {
        if (fechaVencimiento == null) return false;

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = fechaVencimiento.atStartOfDay().minusHours(24);

        return ahora.isBefore(limite) && esActiva();
    }

    /**
     * 🆕 MÉTODO PARA DEPURACIÓN: Información detallada de la firma
     */
    public String getInfoFirmaDigital() {
        if (firmaDigital == null) {
            return "Sin firma digital";
        }

        StringBuilder info = new StringBuilder();

        // Información de la imagen base64
        if (firmaDigital.has("imagen_base64")) {
            int tamaño = firmaDigital.get("imagen_base64").asText().length();
            info.append("Imagen: ").append(tamaño).append(" caracteres");
        }

        // Método usado para capturar la firma
        if (firmaDigital.has("metodo")) {
            info.append(" | Método: ").append(firmaDigital.get("metodo").asText());
        }

        // Fecha de la firma
        if (firmaDigital.has("fecha_firma")) {
            info.append(" | Fecha: ").append(firmaDigital.get("fecha_firma").asText());
        }

        // Versión del procesamiento
        if (firmaDigital.has("version")) {
            info.append(" | Versión: ").append(firmaDigital.get("version").asText());
        }

        return info.toString();
    }

    /**
     * 🔍 MÉTODO PARA LOGS: Obtiene los primeros caracteres de la imagen base64
     */
    public String getPrimeros50CharsImagenBase64() {
        if (firmaDigital != null && firmaDigital.has("imagen_base64")) {
            String imagen = firmaDigital.get("imagen_base64").asText();
            return imagen.length() > 50 ? imagen.substring(0, 50) + "..." : imagen;
        }
        return "No disponible";
    }

    /**
     * 📏 MÉTODO HELPER: Obtiene el tamaño de la imagen base64
     */
    public int getTamanoImagenBase64() {
        if (firmaDigital != null && firmaDigital.has("imagen_base64")) {
            return firmaDigital.get("imagen_base64").asText().length();
        }
        return 0;
    }

    /**
     * 🏥 MÉTODO HELPER: Descripción del tipo de origen
     */
    public String getDescripcionTipoOrigen() {
        if (tipoOrigen == null) return "No definido";

        switch (tipoOrigen) {
            case "HOS": return "Hospitalización";
            case "ACT": return "Acto Médico";
            case "EME": return "Emergencia";
            default: return "Tipo desconocido: " + tipoOrigen;
        }
    }

    /**
     * 📊 MÉTODO HELPER: Descripción del estado
     */
    public String getDescripcionEstado() {
        if (estado == null) return "No definido";

        switch (estado) {
            case "01": return "Activa";
            case "02": return "Despachada";
            case "03": return "Vencida";
            case "04": return "Anulada";
            default: return "Estado desconocido: " + estado;
        }
    }
}