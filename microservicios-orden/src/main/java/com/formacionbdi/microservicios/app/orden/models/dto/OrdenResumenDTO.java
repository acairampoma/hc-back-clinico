package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenResumenDTO {

    private Long id;
    private String numeroOrden;
    private Long pacienteId;

    // ORIGEN
    private String tipoOrigen;
    private String tipoOrigenDescripcion;

    // TIPO Y ESTADO
    private String tipoOrden;
    private String estado;
    private String estadoDescripcion;
    private String prioridad;
    private String prioridadDescripcion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaOrden;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaProgramada;

    // CONTADORES
    private Integer totalExamenes;
    private String firmada;

    // DATOS BÁSICOS PACIENTE (si se incluye JOIN)
    private String nombrePaciente;
    private String numeroDocumento;
}
