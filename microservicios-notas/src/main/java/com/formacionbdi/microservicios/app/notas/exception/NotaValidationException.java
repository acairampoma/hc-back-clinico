package com.formacionbdi.microservicios.app.notas.exception;

import java.util.List;
import java.util.Map;

/**
 * Excepción para errores de validación específicos de notas
 */
public class NotaValidationException extends RuntimeException {

    private final Map<String, List<String>> erroresValidacion;
    private final String campo;

    public NotaValidationException(String message) {
        super(message);
        this.erroresValidacion = null;
        this.campo = null;
    }

    public NotaValidationException(String campo, String message) {
        super(message);
        this.campo = campo;
        this.erroresValidacion = null;
    }

    public NotaValidationException(Map<String, List<String>> erroresValidacion) {
        super("Errores de validación en los datos de la nota");
        this.erroresValidacion = erroresValidacion;
        this.campo = null;
    }

    public Map<String, List<String>> getErroresValidacion() {
        return erroresValidacion;
    }

    public String getCampo() {
        return campo;
    }

    // ===== MÉTODOS ESTÁTICOS PARA VALIDACIONES ESPECÍFICAS =====

    public static NotaValidationException contenidoVacio() {
        return new NotaValidationException("contenido_nota",
                "El contenido de la nota no puede estar vacío");
    }

    public static NotaValidationException tipoNotaInvalido(String tipoNota) {
        return new NotaValidationException("tipo_nota",
                String.format("Tipo de nota inválido: %s. Valores permitidos: 01=Evolución, 02=Interconsulta", tipoNota));
    }

    public static NotaValidationException estadoInvalido(String estado) {
        return new NotaValidationException("estado",
                String.format("Estado inválido: %s. Valores permitidos: 01=Borrador, 02=Finalizada", estado));
    }

    public static NotaValidationException turnoInvalido(String turno) {
        return new NotaValidationException("turno",
                String.format("Turno inválido: %s. Valores permitidos: 01=Mañana, 02=Tarde, 03=Noche", turno));
    }

    public static NotaValidationException jsonInvalido(String campo, String detalles) {
        return new NotaValidationException(campo,
                String.format("Datos JSON inválidos en campo %s: %s", campo, detalles));
    }
}