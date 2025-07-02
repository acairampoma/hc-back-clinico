package com.formacionbdi.microservicios.app.receta.exception;

public class RecetaValidationException extends RuntimeException {
    
    public RecetaValidationException(String message) {
        super(message);
    }

    public RecetaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
