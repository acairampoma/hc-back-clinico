package com.formacionbdi.microservicios.app.receta.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 🚀 Entity para Receta Cabecera - MODERNIZADA JAVA 17
 * ✅ Switch Expressions
 * ✅ Pattern Matching
 * ✅ Text Blocks
 * ✅ Optional chains
 * 🇵🇪 Por Alan Cairampoma - Lima, Perú
 */
@Entity
@Table(name = "receta_cab")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaCab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_receta", unique = true, nullable = false, length = 20)
    private String numeroReceta;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Column(name = "tipo_origen", nullable = false, length = 3)
    private String tipoOrigen; // ACT=Acto Médico, HOS=Hospitalización, EME=Emergencia

    @Column(name = "origen_id", nullable = false)
    private Long origenId;

    @Column(name = "fecha_receta")
    private LocalDateTime fechaReceta;

    // ✅ CORRECTO: LocalDate para fecha_vencimiento (DATE en BD)
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "diagnostico_principal", length = 10)
    private String diagnosticoPrincipal; // CIE-10

    @Column(name = "indicaciones_generales", columnDefinition = "TEXT")
    private String indicacionesGenerales;

    // ❌ QUITADO: observaciones (no existe en BD)
    // @Column(name = "observaciones", columnDefinition = "TEXT")
    // private String observaciones;

    @Column(name = "estado", length = 2)
    private String estado; // 01=Activa, 02=Despachada, 03=Vencida, 04=Anulada

    @Column(name = "firmada", length = 1)
    private String firmada; // S/N

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "firma_digital", columnDefinition = "jsonb")
    private JsonNode firmaDigital;

    @Column(name = "activo", length = 1)
    private String activo;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // 🔥 DESCOMENTADO - Ya tienes RecetaDet entity
    @OneToMany(mappedBy = "recetaCab", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecetaDet> medicamentos;

    // =====================================================
    // 🚀 MÉTODOS MODERNIZADOS CON JAVA 17
    // =====================================================

    /**
     * 🔥 MODERNIZADO: Switch Expression + Optional
     */
    public boolean estaFirmada() {
        return switch (Optional.ofNullable(firmada).orElse("N")) {
            case "S" -> true;
            case "N" -> false;
            default -> throw new IllegalStateException("""
                Estado de firma inválido: %s
                Valores válidos: S (Sí), N (No)
                """.formatted(firmada));
        };
    }

    /**
     * 🔥 MODERNIZADO: Switch Expression para estados
     */
    public boolean esActiva() {
        return switch (Optional.ofNullable(estado).orElse("04")) {
            case "01" -> true;
            case "02", "03", "04" -> false;
            default -> throw new IllegalStateException("""
                Estado de receta inválido: %s
                Estados válidos: 01=Activa, 02=Despachada, 03=Vencida, 04=Anulada
                """.formatted(estado));
        };
    }

    /**
     * 🔥 MODERNIZADO: Pattern Matching para tipo origen
     */
    public boolean esDeHospitalizacion() {
        return switch (Optional.ofNullable(tipoOrigen).orElse("")) {
            case "HOS" -> true;
            case "ACT", "EME" -> false;
            case "" -> throw new IllegalStateException("Tipo de origen no puede ser nulo");
            default -> throw new IllegalStateException("""
                Tipo de origen inválido: %s
                Tipos válidos: HOS=Hospitalización, ACT=Acto Médico, EME=Emergencia
                """.formatted(tipoOrigen));
        };
    }

    /**
     * 🔥 MODERNIZADO: Switch Expression + Text Block
     */
    public boolean esDeActoMedico() {
        return switch (Optional.ofNullable(tipoOrigen).orElse("")) {
            case "ACT" -> true;
            case "HOS", "EME" -> false;
            case "" -> throw new IllegalStateException("Tipo de origen no puede ser nulo");
            default -> throw new IllegalStateException("""
                Tipo de origen inválido: %s
                Tipos válidos: HOS=Hospitalización, ACT=Acto Médico, EME=Emergencia
                """.formatted(tipoOrigen));
        };
    }

    /**
     * 🔥 NUEVO: Switch Expression para emergencia
     */
    public boolean esDeEmergencia() {
        return switch (Optional.ofNullable(tipoOrigen).orElse("")) {
            case "EME" -> true;
            case "HOS", "ACT" -> false;
            case "" -> throw new IllegalStateException("Tipo de origen no puede ser nulo");
            default -> throw new IllegalStateException("""
                Tipo de origen inválido: %s
                Tipos válidos: HOS=Hospitalización, ACT=Acto Médico, EME=Emergencia
                """.formatted(tipoOrigen));
        };
    }

    /**
     * 🔥 MODERNIZADO: Lógica de modificación con Switch + Optional
     */
    public boolean puedeSerModificada() {
        // Verificar fecha de vencimiento
        if (fechaVencimiento == null) return false;

        var ahora = LocalDateTime.now();
        var limite = fechaVencimiento.atStartOfDay().minusHours(24);
        var dentroDelTiempo = ahora.isBefore(limite);

        // Switch expression para validar estado
        var estadoPermiteModificacion = switch (Optional.ofNullable(estado).orElse("04")) {
            case "01" -> true;  // Solo recetas activas
            case "02", "03", "04" -> false;
            default -> false;
        };

        return dentroDelTiempo && estadoPermiteModificacion;
    }

    /**
     * 🔥 MODERNIZADO: Descripción de estado con Switch Expression
     */
    public String getDescripcionEstado() {
        return switch (Optional.ofNullable(estado).orElse("")) {
            case "01" -> "Activa";
            case "02" -> "Despachada";
            case "03" -> "Vencida";
            case "04" -> "Anulada";
            case "" -> "Estado no definido";
            default -> "Estado desconocido: " + estado;
        };
    }

    /**
     * 🔥 MODERNIZADO: Descripción de tipo origen con Switch Expression
     */
    public String getDescripcionTipoOrigen() {
        return switch (Optional.ofNullable(tipoOrigen).orElse("")) {
            case "HOS" -> "Hospitalización";
            case "ACT" -> "Acto Médico";
            case "EME" -> "Emergencia";
            case "" -> "Tipo no definido";
            default -> "Tipo desconocido: " + tipoOrigen;
        };
    }

    /**
     * 🔥 NUEVO: Validación de firma digital con Pattern Matching
     */
    public boolean tieneFirmaDigitalValida() {
        if (firmaDigital == null) return false;

        // Corrección: No usar boolean en switch, usar if tradicional
        if (!estaFirmada()) return false;

        return firmaDigital.has("imagen_base64") &&
                !firmaDigital.get("imagen_base64").asText().trim().isEmpty() &&
                firmaDigital.get("imagen_base64").asText().length() > 100;
    }

    /**
     * 🔥 NUEVO: Información de firma con Text Block
     */
    public String getInfoFirmaDigital() {
        if (firmaDigital == null) {
            return "Sin firma digital";
        }

        var info = new StringBuilder();

        // Información de la imagen base64
        if (firmaDigital.has("imagen_base64")) {
            var tamano = firmaDigital.get("imagen_base64").asText().length();
            info.append("Imagen: ").append(tamano).append(" caracteres");
        }

        // Método usado para capturar la firma
        if (firmaDigital.has("metodo")) {
            info.append(" | Método: ").append(firmaDigital.get("metodo").asText());
        }

        // Fecha de la firma
        if (firmaDigital.has("fecha_firma")) {
            info.append(" | Fecha: ").append(firmaDigital.get("fecha_firma").asText());
        }

        return info.toString();
    }

    /**
     * 🔥 NUEVO: Validación de transición de estado con Switch Expression
     */
    public boolean puedeTransicionarA(String nuevoEstado) {
        var estadoActual = Optional.ofNullable(this.estado).orElse("04");

        return switch (estadoActual) {
            case "01" -> switch (nuevoEstado) { // Desde Activa
                case "02", "04" -> true;  // A Despachada o Anulada
                case "01", "03" -> false;
                default -> false;
            };
            case "02" -> "04".equals(nuevoEstado); // Solo a Anulada
            case "03", "04" -> false; // Vencida y Anulada no pueden cambiar
            default -> false;
        };
    }

    /**
     * 🔥 NUEVO: Validación completa con Text Block de errores
     */
    public String validarIntegridad() {
        var errores = new StringBuilder();

        // Validar campos obligatorios
        if (pacienteId == null) errores.append("- Paciente ID requerido\n");
        if (medicoId == null) errores.append("- Médico ID requerido\n");
        if (tipoOrigen == null || tipoOrigen.trim().isEmpty()) errores.append("- Tipo origen requerido\n");

        // Validar fechas
        if (fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now())) {
            errores.append("- Fecha de vencimiento no puede ser pasada\n");
        }

        // Validar firma si está marcada como firmada
        if ("S".equals(firmada) && !tieneFirmaDigitalValida()) {
            errores.append("- Firma digital inválida para receta marcada como firmada\n");
        }

        return errores.length() > 0 ?
                "Errores de validación:\n" + errores.toString() :
                "Validación exitosa";
    }

    /**
     * 🔥 NUEVO: toString mejorado con información completa
     */
    @Override
    public String toString() {
        return """
            RecetaCab {
                id=%d, numeroReceta='%s', 
                paciente=%d, medico=%d,
                tipo='%s' (%s), estado='%s' (%s),
                firmada=%s, fechaVencimiento=%s
            }""".formatted(
                id, numeroReceta,
                pacienteId, medicoId,
                tipoOrigen, getDescripcionTipoOrigen(),
                estado, getDescripcionEstado(),
                estaFirmada() ? "Sí" : "No",
                fechaVencimiento
        );
    }
}