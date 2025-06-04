package com.formacionbdi.microservicios.app.receta.exception;

/**
 * ⚙️ Excepción para errores de procesamiento de recetas
 */
public class RecetaProcessingException extends RuntimeException {

    private final String operacion;
    private final String detallesTecnicos;

    public RecetaProcessingException(String operacion, String message) {
        super(message);
        this.operacion = operacion;
        this.detallesTecnicos = null;
    }

    public RecetaProcessingException(String operacion, String message, String detallesTecnicos) {
        super(message);
        this.operacion = operacion;
        this.detallesTecnicos = detallesTecnicos;
    }

    public RecetaProcessingException(String operacion, String message, Throwable cause) {
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

    public static RecetaProcessingException pdfGeneracion(String detalles) {
        return new RecetaProcessingException("PDF_GENERATION",
                "Error al generar PDF de la receta", detalles);
    }

    public static RecetaProcessingException firmaDigital(String detalles) {
        return new RecetaProcessingException("DIGITAL_SIGNATURE",
                "Error al procesar firma digital", detalles);
    }

    public static RecetaProcessingException numeroRecetaGeneracion(String detalles) {
        return new RecetaProcessingException("NUMERO_RECETA_GENERATION",
                "Error al generar número de receta", detalles);
    }

    public static RecetaProcessingException vademecumConsulta(String detalles) {
        return new RecetaProcessingException("VADEMECUM_QUERY",
                "Error al consultar información del vademécum", detalles);
    }

    public static RecetaProcessingException transaccionFallida(String operacion, Throwable cause) {
        return new RecetaProcessingException("TRANSACTION_FAILED",
                String.format("Error en transacción durante: %s", operacion), cause);
    }

    public static RecetaProcessingException busquedaMedicamentos(String detalles) {
        return new RecetaProcessingException("MEDICAMENTO_SEARCH",
                "Error al buscar medicamentos en el vademécum", detalles);
    }
}