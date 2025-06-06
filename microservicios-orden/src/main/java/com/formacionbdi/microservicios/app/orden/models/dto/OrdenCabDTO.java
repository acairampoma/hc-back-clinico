package com.formacionbdi.microservicios.app.orden.models.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear nueva orden médica
 * POST /ordenes/crear
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCabDTO {

    // CAMPOS OBLIGATORIOS CON VALIDACIONES
    @NotNull(message = "El paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "El tipo de origen es obligatorio")
    @Size(max = 3, message = "Tipo origen máximo 3 caracteres")
    private String tipoOrigen; // HOS, AMB, EMR

    @NotNull(message = "El origen ID es obligatorio")
    private Long origenId;

    @NotNull(message = "El tipo de orden es obligatorio")
    @Size(max = 20, message = "Tipo orden máximo 20 caracteres")
    private String tipoOrden; // LAB, IMG, PROC, FUNC

    @NotNull(message = "La justificación clínica es obligatoria")
    @NotEmpty(message = "La justificación clínica no puede estar vacía")
    private String justificacionClinica;

    @NotNull(message = "Los exámenes son obligatorios")
    @NotEmpty(message = "Debe incluir al menos un examen")
    private List<OrdenDetDTO> examenes;

    // CAMPOS OPCIONALES
    private String diagnosticoPrincipal; // CIE-10
    private String prioridad; // E, U, N

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaProgramada;
}