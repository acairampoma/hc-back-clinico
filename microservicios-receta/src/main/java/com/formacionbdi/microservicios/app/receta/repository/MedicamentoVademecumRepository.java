package com.formacionbdi.microservicios.app.receta.repository;


import com.formacionbdi.microservicios.app.receta.models.entity.MedicamentoVademecum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🏥 Repository para Vademécum - Búsquedas optimizadas
 */
@Repository
public interface MedicamentoVademecumRepository extends JpaRepository<MedicamentoVademecum, Long> {

    // ===== 🔍 BÚSQUEDAS INTELIGENTES =====

    /**
     * Busca medicamento por código
     */
    @Query("SELECT m FROM MedicamentoVademecum m " +
            "WHERE m.codigoMedicamento = :codigo " +
            "AND m.activo = 'S'")
    Optional<MedicamentoVademecum> findByCodigoMedicamento(@Param("codigo") String codigo);

    /**
     * Búsqueda flexible por nombre, marca o concentración
     */
    @Query("SELECT m FROM MedicamentoVademecum m " +
            "WHERE (LOWER(m.genericName) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
            "OR LOWER(m.concentracion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
            "OR LOWER(CAST(m.brandNames AS string)) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
            "AND m.disponible = 'S' " +
            "AND m.activo = 'S' " +
            "ORDER BY m.genericName ASC")
    List<MedicamentoVademecum> buscarMedicamentos(@Param("busqueda") String busqueda);

    /**
     * Medicamentos por categoría
     */
    @Query("SELECT m FROM MedicamentoVademecum m " +
            "WHERE m.categoria = :categoria " +
            "AND m.disponible = 'S' " +
            "AND m.activo = 'S' " +
            "ORDER BY m.genericName ASC")
    List<MedicamentoVademecum> findByCategoria(@Param("categoria") String categoria);

    /**
     * Medicamentos disponibles para prescripción
     */
    @Query("SELECT m FROM MedicamentoVademecum m " +
            "WHERE m.disponible = 'S' " +
            "AND m.activo = 'S' " +
            "ORDER BY m.genericName ASC")
    List<MedicamentoVademecum> findMedicamentosDisponibles();

    /**
     * Medicamentos controlados
     */
    @Query("SELECT m FROM MedicamentoVademecum m " +
            "WHERE m.controlado = true " +
            "AND m.disponible = 'S' " +
            "AND m.activo = 'S' " +
            "ORDER BY m.genericName ASC")
    List<MedicamentoVademecum> findMedicamentosControlados();

    // ===== 📋 PARA AUTOCOMPLETE/DROPDOWN =====

    /**
     * Top 10 medicamentos para autocomplete
     */
    @Query(value = "SELECT m.* FROM medicamentos_vademecum m " +
            "WHERE (LOWER(m.generic_name) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
            "OR LOWER(m.concentracion) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
            "AND m.disponible = 'S' " +
            "AND m.activo = 'S' " +
            "ORDER BY m.generic_name ASC " +
            "LIMIT 10",
            nativeQuery = true)
    List<MedicamentoVademecum> findTop10ParaAutocomplete(@Param("busqueda") String busqueda);
}