package com.formacionbdi.microservicios.app.notas.exception;

public class JsonInvalidoException extends RuntimeException {
    public JsonInvalidoException(String message) {
        super(message);
    }

    public JsonInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
