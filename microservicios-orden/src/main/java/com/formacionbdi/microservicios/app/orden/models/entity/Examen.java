package com.formacionbdi.microservicios.app.orden.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;

/**
 * 🔬 ENTIDAD EXAMEN - JAVA 17 ENHANCED
 *
 * Features Java 17:
 * - Switch expressions para categorización
 * - Pattern matching para validaciones
 * - Text blocks para descripciones
 * - Records para datos inmutables
 *
 * Tabla maestra de exámenes médicos
 * Para JOIN con ordenes_det (solo FK, sin duplicación)
 *
 * Puerto: 8006 | Tabla: examenes
 *
 * @author Microservicio Órdenes
 * @version 2.0 - Java 17
 * @since Spring Boot 3.1.0
 */
@Entity
@Table(name = "examenes", indexes = {
        @Index(name = "idx_examen_codigo", columnList = "codigo", unique = true),
        @Index(name = "idx_examen_categoria", columnList = "categoria"),
        @Index(name = "idx_examen_subcategoria", columnList = "subcategoria"),
        @Index(name = "idx_examen_activo", columnList = "activo"),
        @Index(name = "idx_examen_nombre", columnList = "nombre")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", unique = true, length = 20)
    @Size(max = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 200)
    @NotNull
    @Size(max = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // =====================================================
    // 🏷️ CATEGORIZACIÓN JERÁRQUICA
    // =====================================================

    @Column(name = "categoria", nullable = false, length = 50)
    @NotNull
    @Size(max = 50)
    private String categoria; // LAB, IMG, PROC, FUNC

    @Column(name = "subcategoria", length = 50)
    @Size(max = 50)
    private String subcategoria; // Hematología, Bioquímica, Radiología, etc.

    // =====================================================
    // 🩺 CARACTERÍSTICAS MÉDICAS
    // =====================================================

    @Column(name = "requiere_ayuno", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String requiereAyuno = "N"; // S/N

    @Column(name = "preparacion_especial", columnDefinition = "TEXT")
    private String preparacionEspecial;

    @Column(name = "tipo_muestra", length = 100)
    @Size(max = 100)
    private String tipoMuestra; // Sangre, Orina, Saliva, etc.

    @Column(name = "tiempo_procesamiento", length = 50)
    @Size(max = 50)
    private String tiempoProcesamiento; // "2-4 horas", "24 horas", etc.

    // =====================================================
    // 📊 VALORES DE REFERENCIA Y OBSERVACIONES
    // =====================================================

    @Column(name = "valores_referencia", columnDefinition = "TEXT")
    private String valoresReferencia;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "costo_base", precision = 10, scale = 2)
    private java.math.BigDecimal costoBase;

    @Column(name = "disponible_ambulatorio", length = 1)
    @Builder.Default
    private String disponibleAmbulatorio = "S"; // S/N

    @Column(name = "disponible_hospitalario", length = 1)
    @Builder.Default
    private String disponibleHospitalario = "S"; // S/N

    // =====================================================
    // 📊 AUDITORÍA (4 campos estándar)
    // =====================================================

    @Column(name = "activo", length = 1)
    @Size(max = 1)
    @Builder.Default
    private String activo = "S";

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "creado_en")
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // =====================================================
    // 🔄 LIFECYCLE HOOKS
    // =====================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (creadoEn == null) {
            creadoEn = now;
        }
        if (actualizadoEn == null) {
            actualizadoEn = now;
        }
        if (requiereAyuno == null) {
            requiereAyuno = "N";
        }
        if (activo == null) {
            activo = "S";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // =====================================================
    // 🔥 MÉTODOS DE NEGOCIO CON JAVA 17
    // =====================================================

    /**
     * 🔥 PATTERN MATCHING: Verificaciones básicas
     */
    public boolean esActivo() {
        return "S".equals(this.activo);
    }

    public boolean requiereAyuno() {
        return "S".equals(this.requiereAyuno);
    }

    public boolean estaDisponibleAmbulatorio() {
        return "S".equals(this.disponibleAmbulatorio);
    }

    public boolean estaDisponibleHospitalario() {
        return "S".equals(this.disponibleHospitalario);
    }

    /**
     * 🔥 SWITCH EXPRESSION: Verificaciones por categoría
     */
    public boolean esExamenLaboratorio() {
        return "LAB".equals(this.categoria);
    }

    public boolean esExamenImagenologia() {
        return "IMG".equals(this.categoria);
    }

    public boolean esExamenProcedimiento() {
        return "PROC".equals(this.categoria);
    }

    public boolean esExamenFuncional() {
        return "FUNC".equals(this.categoria);
    }

    /**
     * 🔥 SWITCH EXPRESSION: Descripción de categoría
     */
    public String getCategoriaDescripcion() {
        return switch (categoria) {
            case "LAB" -> "Laboratorio";
            case "IMG" -> "Imagenología";
            case "PROC" -> "Procedimiento";
            case "FUNC" -> "Prueba Funcional";
            default -> "Otro";
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Categoría completa con subcategoría
     */
    public String getCategoriaCompleta() {
        if (subcategoria != null && !subcategoria.trim().isEmpty()) {
            return getCategoriaDescripcion() + " - " + subcategoria;
        }
        return getCategoriaDescripcion();
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Tiempo estimado en horas
     */
    public int getTiempoEstimadoHoras() {
        if (tiempoProcesamiento == null) return 24; // Default

        String tiempo = tiempoProcesamiento.toLowerCase();

        // Java 17 compatible - sin pattern matching
        if (tiempo.contains("inmediato")) return 0;
        if (tiempo.contains("30 min")) return 1;
        if (tiempo.contains("1 hora")) return 1;
        if (tiempo.contains("2-4 horas")) return 4;
        if (tiempo.contains("6 horas")) return 6;
        if (tiempo.contains("12 horas")) return 12;
        if (tiempo.contains("24 horas")) return 24;
        if (tiempo.contains("48 horas")) return 48;
        if (tiempo.contains("72 horas")) return 72;

        return 24; // Default
    }

    /**
     * 🔥 SWITCH EXPRESSION JAVA 17: Nivel de urgencia
     */
    public String getNivelUrgencia() {
        int horas = getTiempoEstimadoHoras();

        return switch (horas) {
            case 0 -> "INMEDIATO";
            case 1, 2 -> "URGENTE";
            case 3, 4, 5, 6 -> "PRIORITARIO";
            default -> horas <= 24 ? "NORMAL" : "PROGRAMADO";
        };
    }

    /**
     * 🔥 PATTERN MATCHING: Valida disponibilidad según origen
     */
    public boolean estaDisponiblePara(String tipoOrigen) {
        return switch (tipoOrigen) {
            case "HOS" -> estaDisponibleHospitalario();
            case "AMB" -> estaDisponibleAmbulatorio();
            case "EMR" -> estaDisponibleHospitalario(); // Emergencia usa hospitalario
            default -> esActivo();
        };
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Verifica compatibilidad con otros exámenes
     */
    public boolean esCompatibleCon(Examen otroExamen) {
        if (otroExamen == null) return true;

        // Reglas de incompatibilidad
        return switch (this.categoria) {
            case "LAB" -> switch (otroExamen.categoria) {
                case "LAB" -> !conflictoLaboratorio(otroExamen);
                case "IMG" -> !requiereContrastePrevio(otroExamen);
                default -> true;
            };
            case "IMG" -> switch (otroExamen.categoria) {
                case "IMG" -> !conflictoImagenes(otroExamen);
                default -> true;
            };
            default -> true;
        };
    }

    /**
     * 🔥 RECORD JAVA 17: Para datos inmutables de resumen
     */
    public record ResumenExamen(
            Long id,
            String codigo,
            String nombre,
            String categoria,
            String categoriaDescripcion,
            String subcategoria,
            boolean requiereAyuno,
            String tiempoProcesamiento,
            int tiempoEstimadoHoras,
            String nivelUrgencia,
            boolean activo
    ) {
        /**
         * Factory method funcional
         */
        public static ResumenExamen from(Examen examen) {
            return new ResumenExamen(
                    examen.getId(),
                    examen.getCodigo(),
                    examen.getNombre(),
                    examen.getCategoria(),
                    examen.getCategoriaDescripcion(),
                    examen.getSubcategoria(),
                    examen.requiereAyuno(),
                    examen.getTiempoProcesamiento(),
                    examen.getTiempoEstimadoHoras(),
                    examen.getNivelUrgencia(),
                    examen.esActivo()
            );
        }
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene resumen inmutable
     */
    public ResumenExamen getResumen() {
        return ResumenExamen.from(this);
    }

    /**
     * 🔥 TEXT BLOCK JAVA 17: Información completa para pacientes
     */
    public String getInformacionPaciente() {
        var ayuno = requiereAyuno() ? "SÍ requiere ayuno" : "NO requiere ayuno";
        var preparacion = preparacionEspecial != null ? preparacionEspecial : "Sin preparación especial";

        return """
               📋 %s
               
               🏷️ Categoría: %s
               ⏰ Tiempo de procesamiento: %s
               🍽️ Ayuno: %s
               
               📝 Preparación:
               %s
               
               💡 Observaciones:
               %s
               """.formatted(
                nombre,
                getCategoriaCompleta(),
                tiempoProcesamiento != null ? tiempoProcesamiento : "Variable",
                ayuno,
                preparacion,
                observaciones != null ? observaciones : "Ninguna"
        );
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Para logging estructurado
     */
    public String getResumenParaLog() {
        return """
               Examen {
                 id: %d,
                 codigo: %s,
                 nombre: %s,
                 categoria: %s,
                 activo: %s
               }
               """.formatted(
                id,
                codigo != null ? codigo : "SIN_CODIGO",
                nombre,
                getCategoriaCompleta(),
                esActivo()
        );
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Calcula complejidad del examen
     */
    public int getComplejidad() {
        int complejidadBase = switch (categoria) {
            case "LAB" -> 1;
            case "IMG" -> 3;
            case "PROC" -> 5;
            case "FUNC" -> 4;
            default -> 2;
        };

        // Factores adicionales
        int factorAyuno = requiereAyuno() ? 1 : 0;
        int factorPreparacion = (preparacionEspecial != null && !preparacionEspecial.trim().isEmpty()) ? 1 : 0;
        int factorTiempo = getTiempoEstimadoHoras() > 24 ? 2 : 0;

        return complejidadBase + factorAyuno + factorPreparacion + factorTiempo;
    }

    /**
     * 🔥 MÉTODO FUNCIONAL: Obtiene lista de palabras clave para búsqueda
     */
    public Set<String> getPalabrasClave() {
        var palabras = new java.util.HashSet<String>();

        // Agregar nombre tokenizado
        if (nombre != null) {
            palabras.addAll(Arrays.asList(nombre.toLowerCase().split("\\s+")));
        }

        // Agregar categorías
        palabras.add(categoria.toLowerCase());
        if (subcategoria != null) {
            palabras.add(subcategoria.toLowerCase());
        }

        // Agregar código
        if (codigo != null) {
            palabras.add(codigo.toLowerCase());
        }

        return palabras;
    }

    // =====================================================
    // 🔧 MÉTODOS HELPER PRIVADOS
    // =====================================================

    /**
     * Verifica conflictos entre exámenes de laboratorio
     */
    private boolean conflictoLaboratorio(Examen otroExamen) {
        // Ejemplo: algunos exámenes no pueden hacerse juntos
        if ("Hematología".equals(this.subcategoria) && "Coagulación".equals(otroExamen.subcategoria)) {
            return this.requiereAyuno() != otroExamen.requiereAyuno();
        }
        return false;
    }

    /**
     * Verifica si el examen requiere contraste previo
     */
    private boolean requiereContrastePrevio(Examen examenImagen) {
        return examenImagen.nombre != null &&
                examenImagen.nombre.toLowerCase().contains("contraste");
    }

    /**
     * Verifica conflictos entre estudios de imagen
     */
    private boolean conflictoImagenes(Examen otroExamen) {
        // Ejemplo: ciertos estudios no pueden hacerse el mismo día
        List<String> estudiosConRadiacion = Arrays.asList("TAC", "RAYOS X", "TOMOGRAFIA");

        boolean esteConRadiacion = estudiosConRadiacion.stream()
                .anyMatch(estudio -> this.nombre.toUpperCase().contains(estudio));
        boolean otroConRadiacion = estudiosConRadiacion.stream()
                .anyMatch(estudio -> otroExamen.nombre.toUpperCase().contains(estudio));

        return esteConRadiacion && otroConRadiacion;
    }
}