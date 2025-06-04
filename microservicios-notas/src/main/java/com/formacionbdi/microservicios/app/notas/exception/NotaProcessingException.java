package com.formacionbdi.microservicios.app.notas.exception;

/**
 * Excepción para errores de procesamiento de notas (PDF, Audio, etc.)
 */
public class NotaProcessingException extends RuntimeException {

    private final String operacion;
    private final String detallesTecnicos;

    public NotaProcessingException(String operacion, String message) {
        super(message);
        this.operacion = operacion;
        this.detallesTecnicos = null;
    }

    public NotaProcessingException(String operacion, String message, String detallesTecnicos) {
        super(message);
        this.operacion = operacion;
        this.detallesTecnicos = detallesTecnicos;
    }

    public NotaProcessingException(String operacion, String message, Throwable cause) {
        super(message, cause);
        this.operacion = operacion;
        this.detallesTecnicos = cause.getMessage();
    }

    public String getOperacion() {
        return operacion;
    }

    public String getDetallesTecnicos() {
        return detallesTecnicos;
    }

    // ===== MÉTODOS ESTÁTICOS PARA OPERACIONES ESPECÍFICAS =====

    public static NotaProcessingException pdfGeneracion(String detalles) {
        return new NotaProcessingException("PDF_GENERATION",
                "Error al generar PDF de la nota", detalles);
    }

    public static NotaProcessingException audioTranscripcion(String detalles) {
        return new NotaProcessingException("AUDIO_TRANSCRIPTION",
                "Error al transcribir audio de la nota", detalles);
    }

    public static NotaProcessingException firmaDigital(String detalles) {
        return new NotaProcessingException("DIGITAL_SIGNATURE",
                "Error al procesar firma digital", detalles);
    }

    public static NotaProcessingException audioLimpieza(String detalles) {
        return new NotaProcessingException("AUDIO_CLEANUP",
                "Error al limpiar archivos de audio", detalles);
    }

    public static NotaProcessingException jsonParsing(String campo, Throwable cause) {
        return new NotaProcessingException("JSON_PARSING",
                String.format("Error al procesar JSON en campo: %s", campo), cause);
    }
}