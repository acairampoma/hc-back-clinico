package com.formacionbdi.microservicios.app.catalogos.exception;

/**
 * 🔍 Excepción específica para errores de catálogos
 * Puerto: 8009 - Microservicio Catálogos
 */
public class CatalogosException extends RuntimeException {

    public CatalogosException(String message) {
        super(message);
    }

    public CatalogosException(String message, Throwable cause) {
        super(message, cause);
    }
}