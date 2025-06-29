package com.formacionbdi.microservicios.app.notas.models.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospitalizacion_notas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String tipoNota;

    @Column(name = "titulo_nota", length = 200)
    @JsonProperty("titulo_nota")
    private String tituloNota;

    @Column(name = "contenido_nota", columnDefinition = "text")
    @JsonProperty("contenido_nota")
    private String contenidoNota;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty("signos_vitales")
    private String signosVitales;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty("firma_digital")
    private String firmaDigital;

    @Column(name = "audio_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty("audio_data")
    private String audioData;

    @Column(name = "estado", nullable = false, length = 20)
    @JsonProperty("estado")
    private String estado; // BORRADOR, FINALIZADA, ELIMINADA

    @Column(name = "creado_por", nullable = false)
    @JsonProperty("creado_por")
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false)
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
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}