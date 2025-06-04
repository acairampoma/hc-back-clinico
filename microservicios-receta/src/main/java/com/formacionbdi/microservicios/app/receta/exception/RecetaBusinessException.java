package com.formacionbdi.microservicios.app.receta.exception;

/**
 * 📋 Excepción específica para reglas de negocio de recetas médicas
 */
public class RecetaBusinessException extends RuntimeException {

    private final String codigo;
    private final String detalles;

    public RecetaBusinessException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
        this.detalles = null;
    }

    public RecetaBusinessException(String codigo, String message, String detalles) {
        super(message);
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public RecetaBusinessException(String codigo, String message, Throwable cause) {
        super(message, cause);
        this.codigo = codigo;
        this.detalles = null;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDetalles() {
        return detalles;
    }

    // ===== MÉTODOS ESTÁTICOS PARA ERRORES COMUNES =====

    public static RecetaBusinessException recetaDuplicadaMismoDia(String tipoOrigen, Long origenId) {
        return new RecetaBusinessException(
                "RECETA_001",
                "No se puede crear una nueva receta",
                String.format("Ya existe una receta para %s ID %d en el día de hoy", tipoOrigen, origenId)
        );
    }


    public static RecetaBusinessException medicamentosDuplicadosMismoDia(String tipoOrigen, Long origenId) {
        return new RecetaBusinessException(
                "RECETA_002",
                "Medicamentos duplicados detectados",
                String.format("Ya existe una receta con algunos de estos medicamentos para %s ID %d en el día de hoy. " +
                        "Puede crear una nueva receta con medicamentos diferentes.", tipoOrigen, origenId)
        );
    }

    public static RecetaBusinessException recetaNoModificable(Long recetaId) {
        return new RecetaBusinessException(
                "RECETA_002",
                "La receta no puede ser modificada",
                String.format("Receta ID %d: Solo se puede modificar hasta 24 horas antes del vencimiento", recetaId)
        );
    }

    public static RecetaBusinessException cantidadExcesiva(String medicamento, double cantidad) {
        return new RecetaBusinessException(
                "RECETA_003",
                "Cantidad de medicamento excede el límite permitido",
                String.format("Medicamento %s: Cantidad %.1f excede el máximo de 2 unidades", medicamento, cantidad)
        );
    }

    public static RecetaBusinessException recetaYaFinalizada(Long recetaId) {
        return new RecetaBusinessException(
                "RECETA_004",
                "La receta ya está finalizada y no puede ser modificada",
                String.format("Receta ID: %d", recetaId)
        );
    }

    public static RecetaBusinessException firmaRequerida(Long recetaId) {
        return new RecetaBusinessException(
                "RECETA_005",
                "La receta requiere firma digital para ser procesada",
                String.format("Receta ID: %d", recetaId)
        );
    }

    public static RecetaBusinessException permisosDenegados(Long medicoId, Long recetaId) {
        return new RecetaBusinessException(
                "RECETA_006",
                "El médico no tiene permisos para modificar esta receta",
                String.format("Médico %d intentó modificar receta %d", medicoId, recetaId)
        );
    }

    public static RecetaBusinessException recetaVencida(Long recetaId) {
        return new RecetaBusinessException(
                "RECETA_007",
                "La receta está vencida y no puede ser procesada",
                String.format("Receta ID: %d", recetaId)
        );
    }

    public static RecetaBusinessException medicamentoNoDisponible(String codigoMedicamento) {
        return new RecetaBusinessException(
                "RECETA_008",
                "El medicamento no está disponible para prescripción",
                String.format("Código medicamento: %s", codigoMedicamento)
        );
    }
}