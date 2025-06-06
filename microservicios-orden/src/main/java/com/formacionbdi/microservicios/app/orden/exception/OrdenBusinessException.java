package com.formacionbdi.microservicios.app.orden.exception;

/**
 * Excepción específica para errores de negocio de órdenes médicas
 */
public class OrdenBusinessException extends RuntimeException {

    private final String codigoError;

    public OrdenBusinessException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public OrdenBusinessException(String codigoError, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = codigoError;
    }

    public String getCodigoError() {
        return codigoError;
    }
}