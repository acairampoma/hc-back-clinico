package com.formacionbdi.microservicios.app.receta.exception;

import lombok.Getter;

@Getter
public class RecetaBusinessException extends RuntimeException {
    private final String codigo;
    private final Object detalles;

    public RecetaBusinessException(String mensaje) {
        this(mensaje, "REC-001", null);
    }

    public RecetaBusinessException(String mensaje, String codigo) {
        this(mensaje, codigo, null);
    }

    public RecetaBusinessException(String mensaje, String codigo, Object detalles) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = detalles;
    }

    // Constructores estáticos para casos comunes
    public static RecetaBusinessException datosInvalidos(String mensaje) {
        return new RecetaBusinessException(mensaje, "REC-001");
    }

    public static RecetaBusinessException recursoNoEncontrado(String mensaje) {
        return new RecetaBusinessException(mensaje, "REC-002");
    }

    public static RecetaBusinessException conflictoEstado(String mensaje) {
        return new RecetaBusinessException(mensaje, "REC-003");
    }

    public static RecetaBusinessException permisosDenegados(Long medicoId, Long recetaId) {
        return new RecetaBusinessException(
            String.format("El médico %d no tiene permisos para modificar la receta %d", medicoId, recetaId),
            "REC-004",
            java.util.Map.of("medico_id", medicoId, "receta_id", recetaId)
        );
    }

    public static RecetaBusinessException noProcesable(String mensaje) {
        return new RecetaBusinessException(mensaje, "REC-005");
    }
}