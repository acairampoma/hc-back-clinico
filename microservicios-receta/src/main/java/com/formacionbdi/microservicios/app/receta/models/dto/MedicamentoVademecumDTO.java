package com.formacionbdi.microservicios.app.receta.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 💊 DTO para Medicamento del Vademécum - VERIFICADO COMPLETO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicamentoVademecumDTO {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "Código de medicamento es requerido")
    @Size(max = 20, message = "Código no puede exceder 20 caracteres")
    @JsonProperty("codigo_medicamento")
    private String codigoMedicamento;

    @NotBlank(message = "Nombre genérico es requerido")
    @Size(max = 200, message = "Nombre genérico no puede exceder 200 caracteres")
    @JsonProperty("generic_name")
    private String genericName;

    @JsonProperty("brand_names")
    private List<String> brandNames; // ✅ CORREGIDO: Lista de strings, no JSON string

    @Size(max = 100, message = "Concentración no puede exceder 100 caracteres")
    @JsonProperty("concentracion")
    private String concentracion;

    @Size(max = 50, message = "Forma farmacéutica no puede exceder 50 caracteres")
    @JsonProperty("forma_farmaceutica")
    private String formaFarmaceutica;

    @NotBlank(message = "Categoría es requerida")
    @Size(max = 100, message = "Categoría no puede exceder 100 caracteres")
    @JsonProperty("categoria")
    private String categoria;

    @Size(max = 50, message = "Vía de administración no puede exceder 50 caracteres")
    @JsonProperty("via_administracion")
    private String viaAdministracion;

    @JsonProperty("requiere_receta")
    private Boolean requiereReceta;

    @JsonProperty("controlado")
    private Boolean controlado;

    @JsonProperty("disponible")
    private String disponible; // S/N

    @JsonProperty("activo")
    private String activo; // S/N

    // ===== AUDITORÍA =====
    @JsonProperty("creado_por")
    private Long creadoPor;

    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    // ===== MÉTODOS HELPER MEJORADOS =====

    /**
     * Verifica si el medicamento está disponible
     */
    public boolean estaDisponible() {
        return "S".equals(disponible);
    }

    /**
     * Verifica si el medicamento está activo
     */
    public boolean estaActivo() {
        return "S".equals(activo);
    }

    /**
     * Verifica si es medicamento controlado
     */
    public boolean esMedicamentoControlado() {
        return controlado != null && controlado;
    }

    /**
     * Verifica si requiere receta médica
     */
    public boolean requiereRecetaMedica() {
        return requiereReceta != null && requiereReceta;
    }

    /**
     * Obtiene descripción completa para mostrar en interfaz
     */
    public String getDescripcionCompleta() {
        StringBuilder desc = new StringBuilder();

        // Nombre genérico
        desc.append(genericName);

        // Concentración
        if (concentracion != null && !concentracion.trim().isEmpty()) {
            desc.append(" ").append(concentracion);
        }

        // Forma farmacéutica
        if (formaFarmaceutica != null && !formaFarmaceutica.trim().isEmpty()) {
            desc.append(" (").append(formaFarmaceutica).append(")");
        }

        return desc.toString();
    }

    /**
     * Obtiene la primera marca comercial si existe
     */
    public String getPrimeraMarcaComercial() {
        if (brandNames != null && !brandNames.isEmpty()) {
            return brandNames.get(0);
        }
        return genericName; // Fallback al nombre genérico
    }

    /**
     * Obtiene todas las marcas comerciales como string separado por comas
     */
    public String getMarcasComercialesString() {
        if (brandNames != null && !brandNames.isEmpty()) {
            return String.join(", ", brandNames);
        }
        return "Sin marcas comerciales";
    }

    /**
     * Obtiene información para búsqueda (incluye nombre genérico + marcas)
     */
    public String getTextoParaBusqueda() {
        StringBuilder texto = new StringBuilder();

        // Nombre genérico
        texto.append(genericName.toLowerCase());

        // Marcas comerciales
        if (brandNames != null) {
            for (String marca : brandNames) {
                texto.append(" ").append(marca.toLowerCase());
            }
        }

        // Concentración
        if (concentracion != null) {
            texto.append(" ").append(concentracion.toLowerCase());
        }

        return texto.toString();
    }

    /**
     * Verifica si el medicamento contiene el texto de búsqueda
     */
    public boolean coincideConBusqueda(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return true;
        }

        String busqueda = textoBusqueda.toLowerCase().trim();
        return getTextoParaBusqueda().contains(busqueda);
    }

    /**
     * Obtiene el texto para mostrar en dropdown/combo
     */
    public String getTextoParaCombo() {
        StringBuilder texto = new StringBuilder();

        // Código + Nombre
        texto.append(codigoMedicamento).append(" - ").append(genericName);

        // Concentración
        if (concentracion != null) {
            texto.append(" ").append(concentracion);
        }

        return texto.toString();
    }

    /**
     * Verifica si es apto para prescripción
     */
    public boolean esAptoParaPrescripcion() {
        return estaActivo() && estaDisponible();
    }

    /**
     * Obtiene clase CSS para mostrar en interfaz según estado
     */
    public String getClaseCss() {
        if (!estaActivo()) return "medicamento-inactivo";
        if (!estaDisponible()) return "medicamento-no-disponible";
        if (esMedicamentoControlado()) return "medicamento-controlado";
        return "medicamento-normal";
    }
}