package com.formacionbdi.microservicios.app.receta.exception;

import lombok.Getter;

/**
 * 🔍 Excepción para recursos no encontrados en recetas
 */
@Getter
public class RecetaNotFoundException extends RuntimeException {
    private final Long recursoId;

    private RecetaNotFoundException(String mensaje, Long recursoId) {
        super(mensaje);
        this.recursoId = recursoId;
    }

    public static RecetaNotFoundException receta(Long recetaId) {
        return new RecetaNotFoundException(
            String.format("No se encontró la receta con ID %d", recetaId),
            recetaId
        );
    }

    public static RecetaNotFoundException medicamento(Long medicamentoId) {
        return new RecetaNotFoundException(
            String.format("No se encontró el medicamento con ID %d", medicamentoId),
            medicamentoId
        );
    }
}
