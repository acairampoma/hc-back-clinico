package com.formacionbdi.microservicios.app.notas.models.dto;

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
import java.time.LocalDateTime;

/**
 * DTO para HospitalizacionNota - Requests y Responses
 * Incluye validaciones y estructura completa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HospitalizacionNotaDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("numero_nota")
    private String numeroNota;

    @NotNull(message = "Hospitalizacion ID es requerido")
    @JsonProperty("hospitalizacion_id")
    private Long hospitalizacionId;

    @NotBlank(message = "Numero cuenta es requerido")
    @Size(max = 20, message = "Numero cuenta no puede exceder 20 caracteres")
    @JsonProperty("numero_cuenta")
    private String numeroCuenta;

    @NotBlank(message = "Tipo de nota es requerido")
    @Size(max = 2, message = "Tipo nota debe ser de 2 caracteres")
    @JsonProperty("tipo_nota")
    private String tipoNota; // 01=Evolución, 02=Interconsulta

    @Size(max = 200, message = "Titulo no puede exceder 200 caracteres")
    @JsonProperty("titulo_nota")
    private String tituloNota;

    @NotBlank(message = "Contenido de nota es requerido")
    @JsonProperty("contenido_nota")
    private String contenidoNota; // Rich HTML con imágenes, tablas, canvas

    @Size(max = 2, message = "Turno debe ser de 2 caracteres")
    @JsonProperty("turno")
    private String turno; // 01=Mañana, 02=Tarde, 03=Noche

    @JsonProperty("fecha_nota")
    private LocalDateTime fechaNota;

    @JsonProperty("estado")
    private String estado; // 01=Borrador, 02=Finalizada

    // ===== CAMPOS JSONB =====

    @JsonProperty("signos_vitales")
    private JsonNode signosVitales; // Flexible: automático/manual/mixto

    @JsonProperty("firma_digital")
    private JsonNode firmaDigital; // Firma canvas + metadatos

    @JsonProperty("audio_data")
    private JsonNode audioData; // Audio + transcripción + limpieza

    // ===== AUDITORÍA =====

    @NotNull(message = "Creado por es requerido")
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