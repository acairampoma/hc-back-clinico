package com.formacionbdi.microservicios.app.receta.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 🚀 Entity para Receta Detalle - CORREGIDA SIN CAMPOS QUE NO EXISTEN
 * ✅ Switch Expressions
 * ✅ Pattern Matching
 * ✅ Text Blocks
 * ✅ Optional chains
 * 🇵🇪 Por Alan Cairampoma - Lima, Perú
 */
@Entity
@Table(name = "receta_det")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaDet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receta_id", nullable = false)
    private Long recetaId;

    @Column(name = "medicamento_id")
    private Long medicamentoId;

    @Column(name = "codigo_medicamento", nullable = false, length = 20)
    private String codigoMedicamento;

    @Column(name = "diagnostico_medicamento", length = 10)
    private String diagnosticoMedicamento; // CIE-10 específico

    // ===== POSOLOGÍA BÁSICA =====
    @Column(name = "dosis", nullable = false, length = 100)
    private String dosis; // "1 tableta", "2 tabletas", "5 ml"

    @Column(name = "frecuencia", nullable = false, length = 100)
    private String frecuencia; // "Cada 8 horas", "c/4h", "c/6h"

    @Column(name = "duracion_tratamiento", length = 50)
    private String duracionTratamiento; // "3 días", "5 días", "7 días"

    // ===== VÍA DE ADMINISTRACIÓN =====
    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion; // "Oral (VO)", "Intramuscular (IM)"

    // ===== CANTIDAD Y UNIDADES =====
    @Column(name = "cantidad_total", nullable = false, precision = 8, scale = 2)
    private BigDecimal cantidadTotal;

    @Column(name = "unidad_cantidad", length = 20)
    private String unidadCantidad; // "tabletas", "cápsulas", "ml", "frascos"

    // ===== INSTRUCCIONES Y OBSERVACIONES =====
    @Column(name = "instrucciones_especiales", columnDefinition = "TEXT")
    private String instruccionesEspeciales;

    @Column(name = "con_alimentos", length = 1)
    private String conAlimentos; // S/N

    @Column(name = "momento_administracion", length = 50)
    private String momentoAdministracion; // "Post comidas", "Estómago vacío"

    @Column(name = "observaciones_adicionales", columnDefinition = "TEXT")
    private String observacionesAdicionales;

    // ===== CONTROL =====
    @Column(name = "orden_item")
    private Integer ordenItem;

    @Column(name = "estado", length = 2)
    private String estado; // 01=Activo, 02=Dispensado, 03=Suspendido

    @Column(name = "activo", length = 1)
    private String activo; // S/N

    // ===== AUDITORÍA (SOLO LOS QUE EXISTEN) =====
    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    // ❌ COMENTADOS - NO EXISTEN EN LA TABLA REAL
    // @Column(name = "actualizado_por")
    // private Long actualizadoPor;

    // @Column(name = "actualizado_en")
    // private LocalDateTime actualizadoEn;

    // ===== RELACIÓN CON CABECERA =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", insertable = false, updatable = false)
    private RecetaCab recetaCab;

    // =====================================================
    // 🚀 MÉTODOS MODERNIZADOS CON JAVA 17
    // =====================================================

    /**
     * 🔥 MODERNIZADO: Switch Expression para estado
     */
    public boolean estaActivo() {
        return switch (Optional.ofNullable(estado).orElse("03")) {
            case "01" -> true;  // Activo
            case "02", "03" -> false;  // Dispensado, Suspendido
            default -> throw new IllegalStateException("""
                Estado de medicamento inválido: %s
                Estados válidos: 01=Activo, 02=Dispensado, 03=Suspendido
                """.formatted(estado));
        };
    }

    /**
     * 🔥 MODERNIZADO: Switch Expression para dispensado
     */
    public boolean fueDispensado() {
        return switch (Optional.ofNullable(estado).orElse("01")) {
            case "02" -> true;  // Dispensado
            case "01", "03" -> false;
            default -> throw new IllegalStateException("""
                Estado de medicamento inválido: %s
                Estados válidos: 01=Activo, 02=Dispensado, 03=Suspendido
                """.formatted(estado));
        };
    }

    /**
     * 🔥 MODERNIZADO: Switch Expression para suspendido
     */
    public boolean estaSuspendido() {
        return switch (Optional.ofNullable(estado).orElse("01")) {
            case "03" -> true;  // Suspendido
            case "01", "02" -> false;
            default -> throw new IllegalStateException("""
                Estado de medicamento inválido: %s
                Estados válidos: 01=Activo, 02=Dispensado, 03=Suspendido
                """.formatted(estado));
        };
    }

    /**
     * 🔥 MODERNIZADO: Switch Expression para alimentos
     */
    public boolean debeTomarseConAlimentos() {
        return switch (Optional.ofNullable(conAlimentos).orElse("N")) {
            case "S" -> true;
            case "N" -> false;
            default -> throw new IllegalStateException("""
                Valor inválido para 'con_alimentos': %s
                Valores válidos: S (Sí), N (No)
                """.formatted(conAlimentos));
        };
    }

    /**
     * 🔥 MODERNIZADO: Validación diagnóstico con Optional
     */
    public boolean tieneDiagnosticoEspecifico() {
        return Optional.ofNullable(diagnosticoMedicamento)
                .filter(diag -> !diag.trim().isEmpty())
                .isPresent();
    }

    /**
     * 🔥 MODERNIZADO: Validación cantidad con Pattern Matching
     */
    public boolean cantidadEsValida() {
        if (cantidadTotal == null) return false;

        return switch (cantidadTotal.compareTo(BigDecimal.ZERO)) {
            case 1 -> cantidadTotal.compareTo(BigDecimal.valueOf(2.0)) <= 0; // Positivo y <= 2.0
            case 0, -1 -> false; // Cero o negativo
            default -> false;
        };
    }

    /**
     * 🔥 NUEVO: Validación de campos requeridos con Text Block
     */
    public String validarCamposRequeridos() {
        var errores = new StringBuilder();

        if (codigoMedicamento == null || codigoMedicamento.trim().isEmpty()) {
            errores.append("- Código de medicamento requerido\n");
        }
        if (dosis == null || dosis.trim().isEmpty()) {
            errores.append("- Dosis requerida\n");
        }
        if (frecuencia == null || frecuencia.trim().isEmpty()) {
            errores.append("- Frecuencia requerida\n");
        }
        if (!cantidadEsValida()) {
            errores.append("- Cantidad debe estar entre 0.1 y 2.0\n");
        }

        return errores.length() > 0 ?
                "Errores de validación:\n" + errores.toString() :
                "Validación exitosa";
    }

    /**
     * 🔥 MODERNIZADO: Descripción completa con Optional chains
     */
    public String getDescripcionCompleta() {
        var descripcion = new StringBuilder();

        // Nombre del medicamento (si está disponible via JOIN)
        Optional.ofNullable(codigoMedicamento)
                .ifPresent(codigo -> descripcion.append(codigo));

        // Dosis y frecuencia
        Optional.ofNullable(dosis)
                .ifPresent(d -> descripcion.append(" - ").append(d));

        Optional.ofNullable(frecuencia)
                .ifPresent(f -> descripcion.append(" ").append(f));

        // Duración si existe
        Optional.ofNullable(duracionTratamiento)
                .filter(dur -> !dur.trim().isEmpty())
                .ifPresent(dur -> descripcion.append(" por ").append(dur));

        return descripcion.toString();
    }

    /**
     * 🔥 MODERNIZADO: Posología completa con Switch para vía
     */
    public String getPosologiaCompleta() {
        var posologia = new StringBuilder();

        // Dosis + Frecuencia
        posologia.append(Optional.ofNullable(dosis).orElse("Sin dosis"))
                .append(" ")
                .append(Optional.ofNullable(frecuencia).orElse("Sin frecuencia"));

        // Duración
        Optional.ofNullable(duracionTratamiento)
                .filter(dur -> !dur.trim().isEmpty())
                .ifPresent(dur -> posologia.append(" por ").append(dur));

        // Vía de administración con Switch
        var viaDescripcion = switch (Optional.ofNullable(viaAdministracion).orElse("")) {
            case "VO", "Oral" -> " (Vía Oral)";
            case "IM" -> " (Intramuscular)";
            case "EV", "IV" -> " (Endovenoso)";
            case "SC" -> " (Subcutáneo)";
            case "SL" -> " (Sublingual)";
            case "TOP" -> " (Tópico)";
            case "" -> "";
            default -> " (" + viaAdministracion + ")";
        };
        posologia.append(viaDescripcion);

        return posologia.toString();
    }

    /**
     * 🔥 NUEVO: Instrucciones completas con Optional chains
     */
    public String getInstruccionesCompletas() {
        var instrucciones = new StringBuilder();

        // Instrucciones básicas
        Optional.ofNullable(instruccionesEspeciales)
                .filter(inst -> !inst.trim().isEmpty())
                .ifPresent(instrucciones::append);

        // Momento de administración
        Optional.ofNullable(momentoAdministracion)
                .filter(momento -> !momento.trim().isEmpty())
                .ifPresent(momento -> {
                    if (instrucciones.length() > 0) instrucciones.append(". ");
                    instrucciones.append(momento);
                });

        // Con/sin alimentos
        if (debeTomarseConAlimentos()) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append("Tomar con alimentos");
        }

        // Observaciones adicionales
        Optional.ofNullable(observacionesAdicionales)
                .filter(obs -> !obs.trim().isEmpty())
                .ifPresent(obs -> {
                    if (instrucciones.length() > 0) instrucciones.append(". ");
                    instrucciones.append(obs);
                });

        return instrucciones.toString();
    }

    /**
     * 🔥 MODERNIZADO: Descripción de estado con Switch Expression
     */
    public String getDescripcionEstado() {
        return switch (Optional.ofNullable(estado).orElse("")) {
            case "01" -> "Activo";
            case "02" -> "Dispensado";
            case "03" -> "Suspendido";
            case "" -> "Estado no definido";
            default -> "Estado desconocido: " + estado;
        };
    }

    /**
     * 🔥 NUEVO: Resumen para interfaz con Record helper
     */
    public record ResumenMedicamento(
            String codigo,
            String dosis,
            String frecuencia,
            String duracion,
            BigDecimal cantidad,
            String estado
    ) {}

    public ResumenMedicamento getResumen() {
        return new ResumenMedicamento(
                codigoMedicamento,
                dosis,
                frecuencia,
                duracionTratamiento,
                cantidadTotal,
                getDescripcionEstado()
        );
    }

    /**
     * 🔥 NUEVO: Validación de transición de estado
     */
    public boolean puedeTransicionarA(String nuevoEstado) {
        var estadoActual = Optional.ofNullable(this.estado).orElse("01");

        return switch (estadoActual) {
            case "01" -> switch (nuevoEstado) { // Desde Activo
                case "02", "03" -> true;  // A Dispensado o Suspendido
                case "01" -> false;
                default -> false;
            };
            case "02" -> "03".equals(nuevoEstado); // Dispensado solo a Suspendido
            case "03" -> false; // Suspendido no puede cambiar
            default -> false;
        };
    }

    /**
     * 🔥 NUEVO: Constantes con Text Block
     */
    public static final String VALIDATION_RULES = """
        📋 REGLAS DE VALIDACIÓN DE MEDICAMENTO:
        
        🔸 CAMPOS OBLIGATORIOS:
           • Código de medicamento
           • Dosis
           • Frecuencia
           • Cantidad (0.1 - 2.0)
           
        🔸 ESTADOS VÁLIDOS:
           • 01 = Activo
           • 02 = Dispensado
           • 03 = Suspendido
           
        🔸 TRANSICIONES PERMITIDAS:
           • Activo → Dispensado/Suspendido
           • Dispensado → Suspendido
           • Suspendido → Sin cambios
           
        🔸 CANTIDAD MÁXIMA: 2.0 unidades
        🔸 VÍAS ADMINISTRACIÓN:
           • VO/Oral = Vía Oral
           • IM = Intramuscular
           • EV/IV = Endovenoso
           • SC = Subcutáneo
           • SL = Sublingual
           • TOP = Tópico
        """;

    /**
     * 🔥 NUEVO: toString mejorado con información completa
     */
    @Override
    public String toString() {
        return """
            RecetaDet {
                id=%d, codigo='%s',
                dosis='%s', frecuencia='%s',
                cantidad=%s %s, estado='%s' (%s),
                con_alimentos=%s, via='%s'
            }""".formatted(
                id, codigoMedicamento,
                dosis, frecuencia,
                cantidadTotal,
                Optional.ofNullable(unidadCantidad).orElse("unidades"),
                estado, getDescripcionEstado(),
                debeTomarseConAlimentos() ? "Sí" : "No",
                Optional.ofNullable(viaAdministracion).orElse("No especificada")
        );
    }
}