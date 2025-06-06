package com.formacionbdi.microservicios.app.catalogos.exception;

/**
 * 🔍 Excepción para recursos no encontrados en catálogos
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}