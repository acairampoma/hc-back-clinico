package com.formacionbdi.microservicios.app.orden.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstadoOrdenDTO {

    @NotNull(message = "El estado es obligatorio")
    @Size(max = 2, message = "Estado máximo 2 caracteres")
    private String estado; // 01, 02, 03, 04, 05

    private String observaciones;

    @NotNull(message = "El médico que actualiza es obligatorio")
    private Long medicoId;
}
