package com.formacionbdi.microservicios.app.listas.exception;

/**
 * Excepción para recursos no encontrados
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}