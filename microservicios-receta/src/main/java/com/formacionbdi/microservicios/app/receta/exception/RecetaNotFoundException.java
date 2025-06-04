package com.formacionbdi.microservicios.app.receta.exception;

/**
 * 🔍 Excepción para recursos no encontrados en recetas
 */
public class RecetaNotFoundException extends RuntimeException {

    private final String recurso;
    private final String identificador;

    public RecetaNotFoundException(String recurso, String identificador) {
        super(String.format("%s no encontrado con identificador: %s", recurso, identificador));
        this.recurso = recurso;
        this.identificador = identificador;
    }

    public RecetaNotFoundException(String message) {
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

    public static RecetaNotFoundException receta(Long recetaId) {
        return new RecetaNotFoundException("Receta", recetaId.toString());
    }

    public static RecetaNotFoundException recetaPorNumero(String numeroReceta) {
        return new RecetaNotFoundException("Receta", numeroReceta);
    }

    public static RecetaNotFoundException medicamento(Long medicamentoId) {
        return new RecetaNotFoundException("Medicamento", medicamentoId.toString());
    }

    public static RecetaNotFoundException medicamentoPorCodigo(String codigoMedicamento) {
        return new RecetaNotFoundException("Medicamento", codigoMedicamento);
    }

    public static RecetaNotFoundException paciente(Long pacienteId) {
        return new RecetaNotFoundException("Paciente", pacienteId.toString());
    }

    public static RecetaNotFoundException medico(Long medicoId) {
        return new RecetaNotFoundException("Médico", medicoId.toString());
    }

    public static RecetaNotFoundException hospitalizacion(Long hospitalizacionId) {
        return new RecetaNotFoundException("Hospitalización", hospitalizacionId.toString());
    }
}
