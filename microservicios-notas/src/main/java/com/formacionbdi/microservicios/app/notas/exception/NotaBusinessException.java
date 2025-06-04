package com.formacionbdi.microservicios.app.notas.exception;

/**
 * Excepción específica para reglas de negocio de notas vitales
 */
public class NotaBusinessException extends RuntimeException {

    private final String codigo;
    private final String detalles;

    public NotaBusinessException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
        this.detalles = null;
    }

    public NotaBusinessException(String codigo, String message, String detalles) {
        super(message);
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public NotaBusinessException(String codigo, String message, Throwable cause) {
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

    public static NotaBusinessException notaPuedeCrear(Long medicoId, Long hospitalizacionId) {
        return new NotaBusinessException(
                "NOTA_001",
                "No se puede crear una nueva nota",
                String.format("El médico %d ya tiene una nota en borrador para la hospitalización %d",
                        medicoId, hospitalizacionId)
        );
    }

    public static NotaBusinessException notaYaFinalizada(Long notaId) {
        return new NotaBusinessException(
                "NOTA_002",
                "La nota ya está finalizada y no puede ser modificada",
                String.format("Nota ID: %d", notaId)
        );
    }

    public static NotaBusinessException audioNoDisponible(Long notaId) {
        return new NotaBusinessException(
                "NOTA_003",
                "El audio de la nota no está disponible o fue eliminado",
                String.format("Nota ID: %d", notaId)
        );
    }

    public static NotaBusinessException firmaRequerida(Long notaId) {
        return new NotaBusinessException(
                "NOTA_004",
                "La nota requiere firma digital para ser finalizada",
                String.format("Nota ID: %d", notaId)
        );
    }

    public static NotaBusinessException permisosDenegados(Long medicoId, Long notaId) {
        return new NotaBusinessException(
                "NOTA_005",
                "El médico no tiene permisos para modificar esta nota",
                String.format("Médico %d intentó modificar nota %d", medicoId, notaId)
        );
    }
}