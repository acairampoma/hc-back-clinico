package com.formacionbdi.microservicios.app.receta.exception;

import java.util.List;
import java.util.Map;

/**
 * ✅ Excepción para errores de validación específicos de recetas
 */
public class RecetaValidationException extends RuntimeException {

    private final Map<String, List<String>> erroresValidacion;
    private final String campo;

    public RecetaValidationException(String message) {
        super(message);
        this.erroresValidacion = null;
        this.campo = null;
    }

    public RecetaValidationException(String campo, String message) {
        super(message);
        this.campo = campo;
        this.erroresValidacion = null;
    }

    public RecetaValidationException(Map<String, List<String>> erroresValidacion) {
        super("Errores de validación en los datos de la receta");
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

    public static RecetaValidationException diagnosticoInvalido(String diagnostico) {
        return new RecetaValidationException("diagnostico_principal",
                String.format("Diagnóstico CIE-10 inválido: %s", diagnostico));
    }

    public static RecetaValidationException tipoOrigenInvalido(String tipoOrigen) {
        return new RecetaValidationException("tipo_origen",
                String.format("Tipo de origen inválido: %s. Valores permitidos: ACT, HOS", tipoOrigen));
    }

    public static RecetaValidationException estadoInvalido(String estado) {
        return new RecetaValidationException("estado",
                String.format("Estado inválido: %s. Valores permitidos: 01=Activa, 02=Despachada, 03=Vencida, 04=Anulada", estado));
    }

    public static RecetaValidationException fechaVencimientoInvalida() {
        return new RecetaValidationException("fecha_vencimiento",
                "La fecha de vencimiento debe ser posterior a la fecha de receta");
    }

    public static RecetaValidationException medicamentosSinEspecificar() {
        return new RecetaValidationException("medicamentos",
                "La receta debe tener al menos un medicamento especificado");
    }

    public static RecetaValidationException dosisInvalida(String dosis) {
        return new RecetaValidationException("dosis",
                String.format("Dosis inválida: %s. Debe especificar cantidad y unidad", dosis));
    }

    public static RecetaValidationException frecuenciaInvalida(String frecuencia) {
        return new RecetaValidationException("frecuencia",
                String.format("Frecuencia inválida: %s. Debe especificar intervalo de administración", frecuencia));
    }

    public static RecetaValidationException cantidadInvalida(String campo, double valor) {
        return new RecetaValidationException(campo,
                String.format("Cantidad inválida: %.2f. Debe ser entre 0.1 y 2.0", valor));
    }
}
