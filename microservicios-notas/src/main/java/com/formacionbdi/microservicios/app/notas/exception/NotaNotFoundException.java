package com.formacionbdi.microservicios.app.notas.exception;

/**
 * Excepción para recursos no encontrados
 */
public class NotaNotFoundException extends RuntimeException {

    private final String recurso;
    private final String identificador;

    public NotaNotFoundException(String recurso, String identificador) {
        super(String.format("%s no encontrado con identificador: %s", recurso, identificador));
        this.recurso = recurso;
        this.identificador = identificador;
    }

    public NotaNotFoundException(String message) {
        super(message);
        this.recurso = "Recurso";
        this.identificador = "desconocido";
    }

    public String getRecurso() {
        return recurso;
    }

    public String getIdentificador() {
        return identificador;
    }

    // ===== MÉTODOS ESTÁTICOS PARA RECURSOS ESPECÍFICOS =====

    public static NotaNotFoundException nota(Long notaId) {
        return new NotaNotFoundException("Nota", notaId.toString());
    }

    public static NotaNotFoundException hospitalizacion(Long hospitalizacionId) {
        return new NotaNotFoundException("Hospitalización", hospitalizacionId.toString());
    }

    public static NotaNotFoundException medico(Long medicoId) {
        return new NotaNotFoundException("Médico", medicoId.toString());
    }

    public static NotaNotFoundException numeroCuenta(String numeroCuenta) {
        return new NotaNotFoundException("Cuenta", numeroCuenta);
    }
}
