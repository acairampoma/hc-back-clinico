package com.formacionbdi.microservicios.app.receta.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 💊 DTO para Receta Detalle - COMPLETO para interfaz avanzada
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecetaDetDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("receta_id")
    private Long recetaId;

    @JsonProperty("medicamento_id")
    private Long medicamentoId;

    @NotBlank(message = "Código de medicamento es requerido")
    @Size(max = 20, message = "Código no puede exceder 20 caracteres")
    @JsonProperty("codigo_medicamento")
    private String codigoMedicamento;

    @Size(max = 10, message = "Diagnóstico no puede exceder 10 caracteres")
    @JsonProperty("diagnostico_medicamento")
    private String diagnosticoMedicamento; // 🏥 CIE-10 específico para este medicamento

    // ===== POSOLOGÍA BÁSICA =====
    @NotBlank(message = "Dosis es requerida")
    @Size(max = 100, message = "Dosis no puede exceder 100 caracteres")
    @JsonProperty("dosis")
    private String dosis; // "1 tableta", "2 tabletas", "1 cápsula", "5 ml", "10 ml"

    @NotBlank(message = "Frecuencia es requerida")
    @Size(max = 100, message = "Frecuencia no puede exceder 100 caracteres")
    @JsonProperty("frecuencia")
    private String frecuencia; // "Cada 8 horas", "c/4h", "c/6h", "c/8h", "c/12h", "c/24h", "1 vez/día"

    @Size(max = 50, message = "Duración no puede exceder 50 caracteres")
    @JsonProperty("duracion_tratamiento")
    private String duracionTratamiento; // "3 días", "5 días", "7 días", "10 días", "14 días", "Permanente"

    // ===== VÍA DE ADMINISTRACIÓN =====
    @Size(max = 50, message = "Vía de administración no puede exceder 50 caracteres")
    @JsonProperty("via_administracion")
    private String viaAdministracion; // "Oral (VO)", "Intramuscular (IM)", "Endovenoso (EV)"

    // ===== CANTIDAD Y UNIDADES =====
    @NotNull(message = "Cantidad total es requerida")
    @DecimalMin(value = "0.1", message = "Cantidad debe ser mayor a 0")
    @DecimalMax(value = "2.0", message = "Cantidad máxima permitida: 2 unidades")
    @JsonProperty("cantidad_total")
    private BigDecimal cantidadTotal;

    @Size(max = 20, message = "Unidad no puede exceder 20 caracteres")
    @JsonProperty("unidad_cantidad")
    private String unidadCantidad; // "tabletas", "cápsulas", "ml", "frascos"

    // ===== INSTRUCCIONES Y OBSERVACIONES =====
    @JsonProperty("instrucciones_especiales")
    private String instruccionesEspeciales; // Campo libre para instrucciones

    @JsonProperty("con_alimentos")
    private String conAlimentos; // S/N

    @Size(max = 50, message = "Momento de administración no puede exceder 50 caracteres")
    @JsonProperty("momento_administracion")
    private String momentoAdministracion; // "Post comidas", "Estómago vacío", "Con agua", "Sin alcohol", "Antes de dormir"

    @JsonProperty("observaciones_adicionales")
    private String observacionesAdicionales; // Campo libre para observaciones adicionales

    // ===== CONTROL =====
    @JsonProperty("orden_item")
    private Integer ordenItem;

    @JsonProperty("estado")
    private String estado; // 01=Activo, 02=Dispensado, 03=Suspendido

    // ===== CAMPOS DEL VADEMÉCUM (VIA JOIN) =====
    @JsonProperty("nombre_medicamento")
    private String nombreMedicamento; // Del JOIN con vademécum

    @JsonProperty("concentracion")
    private String concentracion; // Del JOIN con vademécum

    @JsonProperty("forma_farmaceutica")
    private String formaFarmaceutica; // Del JOIN con vademécum

    @JsonProperty("categoria")
    private String categoria; // Del JOIN con vademécum

    @JsonProperty("brand_names")
    private String brandNames; // Del JOIN con vademécum (JSON)

    // ===== AUDITORÍA =====
    @NotNull(message = "Creado por es requerido")
    @JsonProperty("creado_por")
    private Long creadoPor;

    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    // ===== MÉTODOS HELPER =====

    /**
     * Verifica si el medicamento está activo
     */
    public boolean estaActivo() {
        return "01".equals(estado);
    }

    /**
     * Verifica si ya fue dispensado
     */
    public boolean fueDispensado() {
        return "02".equals(estado);
    }

    /**
     * Verifica si debe tomarse con alimentos
     */
    public boolean debeTomarseConAlimentos() {
        return "S".equals(conAlimentos);
    }

    /**
     * Verifica si tiene diagnóstico específico
     */
    public boolean tieneDiagnosticoEspecifico() {
        return diagnosticoMedicamento != null && !diagnosticoMedicamento.trim().isEmpty();
    }

    /**
     * Obtiene descripción completa del medicamento
     */
    public String getDescripcionCompleta() {
        StringBuilder desc = new StringBuilder();
        if (nombreMedicamento != null) {
            desc.append(nombreMedicamento);
        }
        if (concentracion != null) {
            desc.append(" ").append(concentracion);
        }
        if (formaFarmaceutica != null) {
            desc.append(" (").append(formaFarmaceutica).append(")");
        }
        return desc.toString();
    }

    /**
     * Obtiene la posología completa para mostrar en interfaz
     */
    public String getPosologiaCompleta() {
        StringBuilder posologia = new StringBuilder();

        // Dosis + Frecuencia
        posologia.append(dosis).append(" ").append(frecuencia);

        // Duración
        if (duracionTratamiento != null) {
            posologia.append(" por ").append(duracionTratamiento);
        }

        // Vía de administración
        if (viaAdministracion != null) {
            posologia.append(" - ").append(viaAdministracion);
        }

        return posologia.toString();
    }

    /**
     * Obtiene todas las instrucciones de administración
     */
    public String getInstruccionesCompletas() {
        StringBuilder instrucciones = new StringBuilder();

        // Instrucciones básicas
        if (instruccionesEspeciales != null) {
            instrucciones.append(instruccionesEspeciales);
        }

        // Momento de administración
        if (momentoAdministracion != null) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append(momentoAdministracion);
        }

        // Con/sin alimentos
        if (debeTomarseConAlimentos()) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append("Tomar con alimentos");
        }

        // Observaciones adicionales
        if (observacionesAdicionales != null) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append(observacionesAdicionales);
        }

        return instrucciones.toString();
    }

    /**
     * Obtiene el resumen del medicamento para la interfaz
     */
    public String getResumenMedicamento() {
        StringBuilder resumen = new StringBuilder();

        // Nombre y dosis principal
        resumen.append(dosis).append(" ").append(frecuencia);

        if (duracionTratamiento != null) {
            resumen.append(" (").append(duracionTratamiento).append(")");
        }

        return resumen.toString();
    }
}
