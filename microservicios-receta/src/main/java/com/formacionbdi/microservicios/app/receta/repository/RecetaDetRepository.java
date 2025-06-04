package com.formacionbdi.microservicios.app.receta.repository;


import com.formacionbdi.microservicios.app.receta.models.entity.RecetaDet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 💊 Repository para Receta Detalle - Con JOIN al vademécum
 */
@Repository
public interface RecetaDetRepository extends JpaRepository<RecetaDet, Long> {

    // ===== 💊 CONSULTAS CON JOIN AL VADEMÉCUM =====

    /**
     * Obtiene medicamentos de una receta con información del vademécum
     * ✅ JOIN INTELIGENTE para evitar múltiples llamadas
     */
    @Query("SELECT rd, mv FROM RecetaDet rd " +
            "LEFT JOIN MedicamentoVademecum mv ON rd.medicamentoId = mv.id " +
            "WHERE rd.recetaId = :recetaId " +
            "AND rd.activo = 'S' " +
            "ORDER BY rd.ordenItem ASC")
    List<Object[]> findMedicamentosConVademecumByRecetaId(@Param("recetaId") Long recetaId);

    /**
     * Busca medicamentos por código
     */
    @Query("SELECT rd FROM RecetaDet rd " +
            "WHERE rd.codigoMedicamento = :codigoMedicamento " +
            "AND rd.activo = 'S' " +
            "ORDER BY rd.creadoEn DESC")
    List<RecetaDet> findByCodigoMedicamento(@Param("codigoMedicamento") String codigoMedicamento);

    /**
     * Medicamentos por receta (sin JOIN para operaciones simples)
     */
    @Query("SELECT rd FROM RecetaDet rd " +
            "WHERE rd.recetaId = :recetaId " +
            "AND rd.activo = 'S' " +
            "ORDER BY rd.ordenItem ASC")
    List<RecetaDet> findByRecetaIdOrderByOrden(@Param("recetaId") Long recetaId);

    // ===== 📊 ESTADÍSTICAS DE MEDICAMENTOS =====

    /**
     * Medicamentos más prescritos (con información del vademécum)
     */
    @Query("SELECT mv.genericName, mv.concentracion, COUNT(rd.id) as total " +
            "FROM RecetaDet rd " +
            "JOIN MedicamentoVademecum mv ON rd.medicamentoId = mv.id " +
            "WHERE rd.activo = 'S' " +
            "GROUP BY mv.id, mv.genericName, mv.concentracion " +
            "ORDER BY total DESC")
    List<Object[]> findMedicamentosMasPrescritos();

    /**
     * Medicamentos por diagnóstico específico
     */
    @Query("SELECT rd.diagnosticoMedicamento, mv.genericName, COUNT(rd.id) " +
            "FROM RecetaDet rd " +
            "LEFT JOIN MedicamentoVademecum mv ON rd.medicamentoId = mv.id " +
            "WHERE rd.diagnosticoMedicamento = :diagnostico " +
            "AND rd.activo = 'S' " +
            "GROUP BY rd.diagnosticoMedicamento, mv.genericName " +
            "ORDER BY COUNT(rd.id) DESC")
    List<Object[]> findMedicamentosPorDiagnostico(@Param("diagnostico") String diagnostico);

    // ===== 🔍 VALIDACIONES DE CANTIDAD =====

    /**
     * Verifica si algún medicamento excede la cantidad máxima
     */
    @Query("SELECT CASE WHEN COUNT(rd) > 0 THEN true ELSE false END " +
            "FROM RecetaDet rd " +
            "WHERE rd.recetaId = :recetaId " +
            "AND rd.cantidadTotal > 2 " +
            "AND rd.activo = 'S'")
    boolean existeMedicamentoConCantidadExcesiva(@Param("recetaId") Long recetaId);



}