package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdenCompletaDTO {

    // INFO CABECERA
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("numeroOrden")
    private String numeroOrden;
    
    @JsonProperty("pacienteId")
    private Long pacienteId;
    
    @JsonProperty("medicoId")
    private Long medicoId;

    // ORIGEN
    @JsonProperty("tipoOrigen")
    private String tipoOrigen;
    
    @JsonProperty("tipoOrigenDescripcion")
    private String tipoOrigenDescripcion;
    
    @JsonProperty("origenId")
    private Long origenId;

    // CLASIFICACIÓN
    @JsonProperty("tipoOrden")
    private String tipoOrden;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("fechaOrden")
    private LocalDateTime fechaOrden;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fechaProgramada")
    private LocalDate fechaProgramada;

    // DIAGNÓSTICO
    @JsonProperty("diagnosticoPrincipal")
    private String diagnosticoPrincipal;

    @JsonProperty("justificacionClinica")
    private String justificacionClinica;

    // ESTADO Y PRIORIDAD
    @JsonProperty("prioridad")
    private String prioridad;
    
    @JsonProperty("prioridadDescripcion")
    private String prioridadDescripcion;
    
    @JsonProperty("estado")
    private String estado;
    
    @JsonProperty("estadoDescripcion")
    private String estadoDescripcion;

    // FIRMA
    @JsonProperty("firmada")
    private String firmada;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("fechaFirma")
    private LocalDateTime fechaFirma;

    @JsonProperty("firmaDigital")
    private String firmaDigital; // Cambiado a String para Base64

    // EXAMENES CON JOIN
    @JsonProperty("examenes")
    private List<OrdenExamenDTO> examenes;

    // AUDITORÍA
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("creadoEn")
    private LocalDateTime creadoEn;

    @JsonProperty("creadoPor")
    private Long creadoPor;

    // CONTADORES
    @JsonProperty("totalExamenes")
    private Integer totalExamenes;
}