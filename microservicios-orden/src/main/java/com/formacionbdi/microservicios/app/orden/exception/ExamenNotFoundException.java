package com.formacionbdi.microservicios.app.orden.exception;

/**
 * Excepción específica para errores con exámenes
 */
public class ExamenNotFoundException extends RuntimeException {

    public ExamenNotFoundException(String mensaje) {
        super(mensaje);
    }

    public ExamenNotFoundException(Long examenId) {
        super("Examen no encontrado con ID: " + examenId);
    }

    public ExamenNotFoundException(String codigoExamen, boolean byCodigo) {
        super("Examen no encontrado con código: " + codigoExamen);
    }
}