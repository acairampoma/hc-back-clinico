package com.formacionbdi.microservicios.app.notas.exception;

import lombok.Getter;

/**
 * Excepción específica para reglas de negocio de notas vitales
 */
@Getter
public class NotaBusinessException extends RuntimeException {

    private final String message;

    public NotaBusinessException(String message) {
        super(message);
        this.message = message;
    }

    public NotaBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public NotaBusinessException(String message, String detalles) {
        super(message);
        this.message = message;
    }

    public NotaBusinessException(String message, Throwable cause, String detalles) {
        super(message, cause);
        this.message = message;
    }

    public static NotaBusinessException notaYaFinalizada(Long notaId) {
        return new NotaBusinessException(String.format("La nota %d ya está finalizada y no puede ser modificada", notaId));
    }

    public static NotaBusinessException notaNoEsBorrador(Long notaId) {
        return new NotaBusinessException(String.format("La nota %d no está en estado borrador", notaId));
    }

    public static NotaBusinessException sinPermisosModificacion(Long notaId, Long medicoId) {
        return new NotaBusinessException(
                String.format("El médico %d no tiene permisos para modificar la nota %d", medicoId, notaId));
    }

    public static NotaBusinessException notaPuedeCrear(Long medicoId, Long hospitalizacionId) {
        return new NotaBusinessException(
                String.format("El médico %d ya tiene una nota en borrador para la hospitalización %d",
                        medicoId, hospitalizacionId)
        );
    }

    public static NotaBusinessException audioNoDisponible(Long notaId) {
        return new NotaBusinessException(
                String.format("Nota ID: %d", notaId)
        );
    }

    public static NotaBusinessException firmaRequerida(Long notaId) {
        return new NotaBusinessException(
                String.format("Nota ID: %d", notaId)
        );
    }

    public static NotaBusinessException permisosDenegados(Long medicoId, Long notaId) {
        return new NotaBusinessException(
                String.format("Médico %d intentó modificar nota %d", medicoId, notaId)
        );
    }
}