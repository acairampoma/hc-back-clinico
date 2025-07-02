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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 🚀 Entity para Vademécum de Medicamentos - MODERNIZADA JAVA 17
 * ✅ Switch Expressions
 * ✅ Pattern Matching
 * ✅ Text Blocks
 * ✅ Optional chains
 * ✅ Record helpers
 */
@Entity
@Table(name = "medicamentos_vademecum")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicamentoVademecum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_medicamento", unique = true, nullable = false, length = 20)
    private String codigoMedicamento;

    @Column(name = "generic_name", nullable = false, length = 200)
    private String genericName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_names", columnDefinition = "jsonb")
    private JsonNode brandNames; // JSON array de marcas comerciales

    @Column(name = "concentracion", length = 100)
    private String concentracion;

    @Column(name = "forma_farmaceutica", length = 50)
    private String formaFarmaceutica;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion;

    @Column(name = "requiere_receta")
    private Boolean requiereReceta;

    @Column(name = "controlado")
    private Boolean controlado;

    @Column(name = "disponible", length = 1)
    private String disponible; // S/N

    @Column(name = "activo", columnDefinition = "CHAR(1)")
    private String activo; // S/N

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    // =====================================================
    // 🚀 MÉTODOS MODERNIZADOS CON JAVA 17
    // =====================================================

    /**
     * 🔥 MODERNIZADO: Switch Expression para disponibilidad
     */
    public boolean estaDisponible() {
        return switch (Optional.ofNullable(disponible).orElse("N")) {
            case "S" -> true;
            case "N" -> false;
            default -> throw new IllegalStateException("""
                Estado de disponibilidad inválido: %s
                Valores válidos: S (Disponible), N (No disponible)
                """.formatted(disponible));
        };
    }

    /**
     * 🔥 MODERNIZADO: Switch Expression para activo
     */
    public boolean estaActivo() {
        return switch (Optional.ofNullable(activo).orElse("N")) {
            case "S" -> true;
            case "N" -> false;
            default -> throw new IllegalStateException("""
                Estado activo inválido: %s
                Valores válidos: S (Activo), N (Inactivo)
                """.formatted(activo));
        };
    }

    /**
     * 🔥 MODERNIZADO: Optional chain para controlado
     */
    public boolean esMedicamentoControlado() {
        return Optional.ofNullable(controlado).orElse(false);
    }

    /**
     * 🔥 MODERNIZADO: Optional chain para requiere receta
     */
    public boolean requiereRecetaMedica() {
        return Optional.ofNullable(requiereReceta).orElse(true);
    }

    /**
     * 🔥 MODERNIZADO: Descripción completa con Optional chains
     */
    public String getDescripcionCompleta() {
        var descripcion = new StringBuilder();

        // Nombre genérico
        descripcion.append(Optional.ofNullable(genericName).orElse("Sin nombre"));

        // Concentración
        Optional.ofNullable(concentracion)
                .filter(conc -> !conc.trim().isEmpty())
                .ifPresent(conc -> descripcion.append(" ").append(conc));

        // Forma farmacéutica
        Optional.ofNullable(formaFarmaceutica)
                .filter(forma -> !forma.trim().isEmpty())
                .ifPresent(forma -> descripcion.append(" (").append(forma).append(")"));

        return descripcion.toString();
    }

    /**
     * 🔥 MODERNIZADO: Primera marca comercial con Optional
     */
    public String getPrimeraMarcaComercial() {
        return Optional.ofNullable(brandNames)
                .filter(brands -> brands.isArray() && brands.size() > 0)
                .map(brands -> brands.get(0).asText())
                .orElse(genericName); // Fallback al nombre genérico
    }

    /**
     * 🔥 MODERNIZADO: Todas las marcas como List con Stream
     */
    public List<String> getMarcasComercialesList() {
        return Optional.ofNullable(brandNames)
                .filter(brands -> brands.isArray())
                .map(brands -> brands.spliterator())
                .map(spliterator -> java.util.stream.StreamSupport.stream(spliterator, false))
                .orElse(java.util.stream.Stream.empty())
                .map(JsonNode::asText)
                .filter(marca -> !marca.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 🔥 MODERNIZADO: Marcas como string con Switch para formato
     */
    public String getMarcasComercialesString() {
        var marcas = getMarcasComercialesList();

        return switch (marcas.size()) {
            case 0 -> "Sin marcas comerciales";
            case 1 -> marcas.get(0);
            default -> String.join(", ", marcas);
        };
    }

    /**
     * 🔥 MODERNIZADO: Texto para búsqueda con Stream functional
     */
    public String getTextoParaBusqueda() {
        var textos = List.of(
                Optional.ofNullable(genericName).orElse(""),
                Optional.ofNullable(concentracion).orElse(""),
                Optional.ofNullable(codigoMedicamento).orElse(""),
                getMarcasComercialesString()
        );

        return textos.stream()
                .filter(texto -> !texto.trim().isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.joining(" "));
    }

    /**
     * 🔥 MODERNIZADO: Coincidencia con búsqueda usando if encadenado
     */
    public boolean coincideConBusqueda(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return true;
        }

        var busqueda = textoBusqueda.toLowerCase().trim();
        var textoCompleto = getTextoParaBusqueda();

        // No se puede usar boolean en switch - usar if tradicional
        if (busqueda.length() <= 2) {
            return textoCompleto.startsWith(busqueda); // Búsquedas cortas: exactas
        } else {
            return textoCompleto.contains(busqueda);   // Búsquedas largas: contiene
        }
    }

    /**
     * 🔥 MODERNIZADO: Texto para combo con if tradicional
     */
    public String getTextoParaCombo() {
        var codigo = Optional.ofNullable(codigoMedicamento).orElse("SIN-COD");
        var nombre = Optional.ofNullable(genericName).orElse("Sin nombre");
        var concentracion = Optional.ofNullable(this.concentracion).orElse("");

        // No se puede usar boolean en switch - usar if tradicional
        if (concentracion.isEmpty()) {
            return "%s - %s".formatted(codigo, nombre);
        } else {
            return "%s - %s %s".formatted(codigo, nombre, concentracion);
        }
    }

    /**
     * 🔥 MODERNIZADO: Apto para prescripción con multiple validations
     */
    public boolean esAptoParaPrescripcion() {
        return estaActivo() && estaDisponible();
    }

    /**
     * 🔥 MODERNIZADO: Clase CSS con if encadenado
     */
    public String getClaseCss() {
        // Prioridad: Inactivo > No disponible > Controlado > Normal
        if (!estaActivo()) return "medicamento-inactivo";
        if (!estaDisponible()) return "medicamento-no-disponible";

        // No se puede usar boolean en switch - usar if tradicional
        if (esMedicamentoControlado()) {
            return "medicamento-controlado";
        } else {
            return "medicamento-normal";
        }
    }

    /**
     * 🔥 NUEVO: Validación integral con Text Block
     */
    public String validarIntegridad() {
        var errores = new StringBuilder();

        // Validar campos obligatorios
        if (codigoMedicamento == null || codigoMedicamento.trim().isEmpty()) {
            errores.append("- Código de medicamento requerido\n");
        }
        if (genericName == null || genericName.trim().isEmpty()) {
            errores.append("- Nombre genérico requerido\n");
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            errores.append("- Categoría requerida\n");
        }

        // Validar estado lógico
        if (!estaActivo() && estaDisponible()) {
            errores.append("- Medicamento inactivo no puede estar disponible\n");
        }

        return errores.length() > 0 ?
                "Errores de validación:\n" + errores.toString() :
                "Validación exitosa";
    }

    /**
     * 🔥 NUEVO: Información de categoría con Switch Expression
     */
    public String getInfoCategoria() {
        return switch (Optional.ofNullable(categoria).orElse("").toLowerCase()) {
            case "analgesico", "analgésico" -> "💊 Alivio del dolor";
            case "antibiotico", "antibiótico" -> "🦠 Tratamiento infecciones";
            case "antiinflamatorio" -> "🔥 Reduce inflamación";
            case "cardiovascular" -> "❤️ Sistema cardiovascular";
            case "neurologico", "neurológico" -> "🧠 Sistema nervioso";
            case "gastrointestinal" -> "🥄 Sistema digestivo";
            case "respiratorio" -> "🫁 Sistema respiratorio";
            case "endocrino" -> "⚖️ Sistema hormonal";
            case "" -> "Sin categoría definida";
            default -> "📋 " + categoria;
        };
    }

    /**
     * 🔥 NUEVO: Record para resumen de medicamento
     */
    public record ResumenMedicamento(
            Long id,
            String codigo,
            String nombre,
            String categoria,
            String concentracion,
            boolean disponible,
            boolean controlado
    ) {}

    public ResumenMedicamento getResumen() {
        return new ResumenMedicamento(
                id,
                codigoMedicamento,
                genericName,
                categoria,
                concentracion,
                estaDisponible(),
                esMedicamentoControlado()
        );
    }

    /**
     * 🔥 NUEVO: Constantes con Text Block
     */
    public static final String CATEGORIAS_PRINCIPALES = """
        📋 CATEGORÍAS PRINCIPALES DE MEDICAMENTOS:
        
        💊 ANALGÉSICOS:
           • Paracetamol, Ibuprofeno, Aspirina
           • Para alivio del dolor
           
        🦠 ANTIBIÓTICOS:
           • Amoxicilina, Azitromicina, Ciprofloxacino
           • Tratamiento de infecciones
           
        🔥 ANTIINFLAMATORIOS:
           • Diclofenaco, Naproxeno, Celecoxib
           • Reduce inflamación
           
        ❤️ CARDIOVASCULARES:
           • Enalapril, Atenolol, Amlodipino
           • Sistema cardiovascular
           
        🧠 NEUROLÓGICOS:
           • Carbamazepina, Fenitoína, Gabapentina
           • Sistema nervioso
           
        📋 OTROS:
           • Gastrointestinales, Respiratorios, Endocrinos
        """;

    /**
     * 🔥 NUEVO: toString mejorado con información completa
     */
    @Override
    public String toString() {
        return """
            MedicamentoVademecum {
                id=%d, codigo='%s',
                nombre='%s', categoria='%s',
                concentracion='%s', forma='%s',
                disponible=%s, activo=%s, controlado=%s,
                marcas=%s
            }""".formatted(
                id, codigoMedicamento,
                genericName, categoria,
                Optional.ofNullable(concentracion).orElse("N/A"),
                Optional.ofNullable(formaFarmaceutica).orElse("N/A"),
                estaDisponible() ? "Sí" : "No",
                estaActivo() ? "Sí" : "No",
                esMedicamentoControlado() ? "Sí" : "No",
                getMarcasComercialesString()
        );
    }
}