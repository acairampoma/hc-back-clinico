package com.formacionbdi.microservicios.app.orden.exception;

/**
 * Excepción para cuando no se encuentra una orden
 */
public class OrdenNotFoundException extends RuntimeException {

    public OrdenNotFoundException(String mensaje) {
        super(mensaje);
    }

    public OrdenNotFoundException(Long ordenId) {
        super("Orden no encontrada con ID: " + ordenId);
    }

    public OrdenNotFoundException(String numeroOrden, boolean byNumero) {
        super("Orden no encontrada con número: " + numeroOrden);
    }
}