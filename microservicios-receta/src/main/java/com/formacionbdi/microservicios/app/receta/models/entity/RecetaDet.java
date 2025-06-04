package com.formacionbdi.microservicios.app.receta.models.entity;

import com.formacionbdi.microservicios.app.receta.models.entity.MedicamentoVademecum;
import com.formacionbdi.microservicios.app.receta.models.entity.RecetaCab;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 💊 Entity para Receta Detalle - COMPLETO para soportar interfaz avanzada
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
    private String diagnosticoMedicamento; // 🏥 CIE-10 específico

    // ===== POSOLOGÍA BÁSICA =====
    @Column(name = "dosis", nullable = false, length = 100)
    private String dosis; // "1 tableta", "2 tabletas", "1 cápsula", "5 ml", "10 ml"

    @Column(name = "frecuencia", nullable = false, length = 100)
    private String frecuencia; // "Cada 8 horas", "c/4h", "c/6h", "c/8h", "c/12h", "c/24h", "1 vez/día"

    @Column(name = "duracion_tratamiento", length = 50)
    private String duracionTratamiento; // "3 días", "5 días", "7 días", "10 días", "14 días", "Permanente"

    // ===== VÍA DE ADMINISTRACIÓN =====
    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion; // "Oral (VO)", "Intramuscular (IM)", "Endovenoso (EV)", etc.

    // ===== CANTIDAD Y UNIDADES =====
    @Column(name = "cantidad_total", nullable = false, precision = 8, scale = 2)
    private BigDecimal cantidadTotal;

    @Column(name = "unidad_cantidad", length = 20)
    private String unidadCantidad;

    // ===== INSTRUCCIONES ESPECÍFICAS DEL MOCK =====
    @Column(name = "instrucciones_especiales", columnDefinition = "TEXT")
    private String instruccionesEspeciales; // "Tomar después de las comidas..."

    @Column(name = "con_alimentos", length = 1)
    private String conAlimentos; // S/N

    // ===== NUEVOS CAMPOS PARA SOPORTAR EL MOCK =====

    @Column(name = "momento_administracion", length = 50)
    private String momentoAdministracion; // "Post comidas", "Estómago vacío", "Con agua", "Sin alcohol", "Antes de dormir"

    @Column(name = "observaciones_adicionales", columnDefinition = "TEXT")
    private String observacionesAdicionales; // Campo libre para observaciones

    // ===== CONTROL Y ORDEN =====
    @Column(name = "orden_item")
    private Integer ordenItem;

    @Column(name = "estado", length = 2)
    private String estado; // 01=Activo, 02=Dispensado, 03=Suspendido

    @Column(name = "activo", length = 1)
    private String activo;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    // ===== RELACIONES =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", insertable = false, updatable = false)
    private RecetaCab recetaCab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", insertable = false, updatable = false)
    private MedicamentoVademecum medicamentoVademecum;

    // ===== MÉTODOS HELPER EXPANDIDOS =====

    /**
     * Verifica si el medicamento está activo
     */
    public boolean estaActivo() {
        return "01".equals(estado);
    }

    /**
     * Verifica si ya fue dispensado
     */
    public boolean fueDispensado() {
        return "02".equals(estado);
    }

    /**
     * Verifica si debe tomarse con alimentos
     */
    public boolean debeTomarseConAlimentos() {
        return "S".equals(conAlimentos);
    }

    /**
     * Verifica si tiene diagnóstico específico
     */
    public boolean tieneDiagnosticoEspecifico() {
        return diagnosticoMedicamento != null && !diagnosticoMedicamento.trim().isEmpty();
    }

    /**
     * Verifica si la cantidad está dentro del límite permitido
     */
    public boolean cantidadDentroDelLimite() {
        return cantidadTotal != null &&
                cantidadTotal.compareTo(BigDecimal.ZERO) > 0 &&
                cantidadTotal.compareTo(new BigDecimal("2")) <= 0;
    }

    /**
     * Obtiene la posología completa para mostrar en interfaz
     */
    public String getPosologiaCompleta() {
        StringBuilder posologia = new StringBuilder();

        // Dosis + Frecuencia
        posologia.append(dosis).append(" ").append(frecuencia);

        // Duración
        if (duracionTratamiento != null) {
            posologia.append(" por ").append(duracionTratamiento);
        }

        // Vía de administración
        if (viaAdministracion != null) {
            posologia.append(" - ").append(viaAdministracion);
        }

        return posologia.toString();
    }

    /**
     * Obtiene todas las instrucciones de administración
     */
    public String getInstruccionesCompletas() {
        StringBuilder instrucciones = new StringBuilder();

        // Instrucciones básicas
        if (instruccionesEspeciales != null) {
            instrucciones.append(instruccionesEspeciales);
        }

        // Momento de administración
        if (momentoAdministracion != null) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append(momentoAdministracion);
        }

        // Con/sin alimentos
        if (debeTomarseConAlimentos()) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append("Tomar con alimentos");
        }

        // Observaciones adicionales
        if (observacionesAdicionales != null) {
            if (instrucciones.length() > 0) instrucciones.append(". ");
            instrucciones.append(observacionesAdicionales);
        }

        return instrucciones.toString();
    }

    /**
     * Obtiene el resumen del medicamento para la interfaz
     */
    public String getResumenMedicamento() {
        StringBuilder resumen = new StringBuilder();

        // Nombre y dosis principal
        resumen.append(dosis).append(" ").append(frecuencia);

        if (duracionTratamiento != null) {
            resumen.append(" (").append(duracionTratamiento).append(")");
        }

        return resumen.toString();
    }
}