package com.formacionbdi.microservicios.app.listas.exception;

/**
 * Excepción específica para errores relacionados con la estructura hospitalaria
 */
public class EstructuraHospitalException extends RuntimeException {

    public EstructuraHospitalException(String message) {
        super(message);
    }

    public EstructuraHospitalException(String message, Throwable cause) {
        super(message, cause);
    }
}