package com.formacionbdi.microservicios.app.notas.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.time.LocalDateTime;

/**
 * Entity que mapea la tabla hospitalizacion_notas
 * Incluye los nuevos campos JSONB: signos_vitales, firma_digital, audio_data
 */
@Entity
@Table(name = "hospitalizacion_notas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class HospitalizacionNota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_nota", unique = true, nullable = false, length = 20)
    @JsonProperty("numero_nota")
    private String numeroNota;

    @Column(name = "hospitalizacion_id", nullable = false)
    @JsonProperty("hospitalizacion_id")
    private Long hospitalizacionId;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    @JsonProperty("numero_cuenta")
    private String numeroCuenta;

    @Column(name = "tipo_nota", nullable = false, length = 2)
    @JsonProperty("tipo_nota")
    private String tipoNota; // 01=Evolución, 02=Interconsulta, etc.

    @Column(name = "titulo_nota", length = 200)
    @JsonProperty("titulo_nota")
    private String tituloNota;

    @Column(name = "contenido_nota", nullable = false, columnDefinition = "TEXT")
    @JsonProperty("contenido_nota")
    private String contenidoNota; // Rich HTML con imágenes, tablas, canvas

    @Column(name = "turno", length = 2)
    @JsonProperty("turno")
    private String turno; // 01=Mañana, 02=Tarde, 03=Noche

    @Column(name = "fecha_nota")
    @JsonProperty("fecha_nota")
    private LocalDateTime fechaNota;

    @Column(name = "estado", length = 2)
    @JsonProperty("estado")
    private String estado; // 01=Borrador, 02=Finalizada

    // ===== NUEVOS CAMPOS JSONB =====

    @Type(type = "jsonb")
    @Column(name = "signos_vitales", columnDefinition = "jsonb")
    @JsonProperty("signos_vitales")
    private JsonNode signosVitales; // Automático/Manual/Mixto + valores

    @Type(type = "jsonb")
    @Column(name = "firma_digital", columnDefinition = "jsonb")
    @JsonProperty("firma_digital")
    private JsonNode firmaDigital; // Firma canvas + metadatos legales

    @Type(type = "jsonb")
    @Column(name = "audio_data", columnDefinition = "jsonb")
    @JsonProperty("audio_data")
    private JsonNode audioData; // Audio + transcripción + limpieza automática

    // ===== AUDITORÍA =====

    @Column(name = "creado_por", nullable = false)
    @JsonProperty("creado_por")
    private Long creadoPor; // ID del médico

    @Column(name = "creado_en")
    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_por")
    @JsonProperty("actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    @JsonProperty("actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        if (fechaNota == null) {
            fechaNota = LocalDateTime.now();
        }
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "01"; // Borrador por defecto
        }
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}