package com.formacionbdi.microservicios.app.notas.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para HospitalizacionNota - Requests y Responses
 * Incluye validaciones y estructura completa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalizacionNotaDTO {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "El número de nota no puede estar vacío")
    @Size(max = 20, message = "El número de nota no puede exceder los 20 caracteres")
    @JsonProperty("numero_nota")
    private String numeroNota;

    @NotNull(message = "El ID de hospitalización no puede estar vacío")
    @JsonProperty("hospitalizacion_id")
    private Long hospitalizacionId;

    @NotBlank(message = "El número de cuenta no puede estar vacío")
    @Size(max = 20, message = "El número de cuenta no puede exceder los 20 caracteres")
    @JsonProperty("numero_cuenta")
    private String numeroCuenta;

    @NotBlank(message = "El tipo de nota no puede estar vacío")
    @Size(max = 2, message = "El tipo de nota no puede exceder los 2 caracteres")
    @JsonProperty("tipo_nota")
    private String tipoNota; // 01=Evolución, 02=Interconsulta

    @Size(max = 200, message = "El título de la nota no puede exceder los 200 caracteres")
    @JsonProperty("titulo_nota")
    private String tituloNota;

    @NotBlank(message = "Contenido de nota es requerido")
    @JsonProperty("contenido_nota")
    private String contenidoNota; // Rich HTML con imágenes, tablas, canvas

    @JsonProperty("signos_vitales")
    private JsonNode signosVitales; // Flexible: automático/manual/mixto

    @JsonProperty("firma_digital")
    private JsonNode firmaDigital; // Firma canvas + metadatos

    @JsonProperty("audio_data")
    private JsonNode audioData; // Audio + transcripción + limpieza

    @NotBlank(message = "El estado no puede estar vacío")
    @Size(max = 20, message = "El estado no puede exceder los 20 caracteres")
    @JsonProperty("estado")
    private String estado; // 01=Borrador, 02=Finalizada

    @NotNull(message = "El ID del creador no puede estar vacío")
    @JsonProperty("creado_por")
    private Long creadoPor; // ID del médico

    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    @JsonProperty("actualizado_por")
    private Long actualizadoPor;

    @JsonProperty("actualizado_en")
    private LocalDateTime actualizadoEn;

    // ===== MÉTODOS HELPER =====

    /**
     * Verifica si la nota tiene firma digital válida
     */
    public boolean tieneFirmaDigital() {
        return firmaDigital != null &&
                firmaDigital.has("tiene_firma") &&
                firmaDigital.get("tiene_firma").asBoolean();
    }

    /**
     * Verifica si la nota tiene audio grabado
     */
    public boolean tieneAudio() {
        return audioData != null &&
                audioData.has("tiene_audio") &&
                audioData.get("tiene_audio").asBoolean() &&
                (!audioData.has("audio_eliminado") || !audioData.get("audio_eliminado").asBoolean());
    }

    /**
     * Verifica si el audio está transcrito
     */
    public boolean audioTranscrito() {
        return audioData != null &&
                audioData.has("transcrito") &&
                audioData.get("transcrito").asBoolean();
    }

    /**
     * Obtiene el origen de los signos vitales
     */
    public String getOrigenSignosVitales() {
        if (signosVitales != null && signosVitales.has("origen")) {
            return signosVitales.get("origen").asText();
        }
        return "no_definido";
    }

    /**
     * Obtiene la duración del audio en segundos
     */
    public Integer getDuracionAudioSegundos() {
        if (audioData != null && audioData.has("duracion_segundos")) {
            return audioData.get("duracion_segundos").asInt();
        }
        return 0;
    }

    /**
     * Verifica si es una nota finalizada
     */
    public boolean esFinalizada() {
        return "02".equals(estado);
    }

    /**
     * Obtiene el nombre del médico desde la firma digital
     */
    public String getNombreMedicoFirma() {
        if (firmaDigital != null &&
                firmaDigital.has("medico") &&
                firmaDigital.get("medico").has("nombre")) {
            return firmaDigital.get("medico").get("nombre").asText();
        }
        return "No disponible";
    }
}