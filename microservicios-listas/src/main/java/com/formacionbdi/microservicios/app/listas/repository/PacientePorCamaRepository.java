// 1. REPOSITORY CORREGIDO
// ===============================================
package com.formacionbdi.microservicios.app.listas.repository;

import com.formacionbdi.microservicios.app.listas.models.entity.PacientePorCama;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gestión de pacientes por cama
 * Utiliza la vista vista_pacientes_por_cama
 */
@Repository
public interface PacientePorCamaRepository extends JpaRepository<PacientePorCama, String> {

    /**
     * Obtiene todas las camas con información de pacientes (JSON como String)
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> obtenerTodasLasCamas();

    /**
     * Obtiene solo las camas ocupadas (con pacientes)
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE patient_data IS NOT NULL ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> obtenerCamasOcupadas();

    /**
     * Obtiene solo las camas disponibles (sin pacientes)
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE patient_data IS NULL ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> obtenerCamasDisponibles();

    /**
     * Obtiene información de una cama específica por número
     * CORREGIDO: Cambiado para devolver List<Object[]> y manejar como lista
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE bed_number = :bedNumber",
            nativeQuery = true)
    List<Object[]> obtenerCamaPorNumero(@Param("bedNumber") String bedNumber);

    /**
     * Busca camas por rango de números (ej: C001-C010)
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE bed_number BETWEEN :startBed AND :endBed ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> obtenerCamasPorRango(@Param("startBed") String startBed, @Param("endBed") String endBed);

    /**
     * Busca pacientes por DNI
     * CORREGIDO: Cambiado para devolver List<Object[]>
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE patient_data->>'personal_info'->>'dni' = :dni",
            nativeQuery = true)
    List<Object[]> buscarPacientePorDni(@Param("dni") String dni);

    /**
     * Busca pacientes por nombre (búsqueda parcial)
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE " +
            "LOWER(patient_data->>'personal_info'->>'first_name') LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
            "LOWER(patient_data->>'personal_info'->>'last_name') LIKE LOWER(CONCAT('%', :nombre, '%')) " +
            "ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> buscarPacientesPorNombre(@Param("nombre") String nombre);

    /**
     * Busca pacientes por médico tratante
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE " +
            "LOWER(patient_data->>'medical_info'->>'attending_physician') LIKE LOWER(CONCAT('%', :medico, '%')) " +
            "ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> buscarPacientesPorMedico(@Param("medico") String medico);

    /**
     * Obtiene estadísticas de ocupación
     */
    @Query(value = "SELECT " +
            "COUNT(*) as total_camas, " +
            "COUNT(patient_data) as camas_ocupadas, " +
            "COUNT(*) - COUNT(patient_data) as camas_disponibles, " +
            "ROUND((COUNT(patient_data) * 100.0 / COUNT(*)), 2) as porcentaje_ocupacion " +
            "FROM vista_pacientes_por_cama",
            nativeQuery = true)
    Object[] obtenerEstadisticasOcupacion();

    /**
     * Verifica si una cama específica está ocupada
     */
    @Query(value = "SELECT CASE WHEN patient_data IS NOT NULL THEN true ELSE false END FROM vista_pacientes_por_cama WHERE bed_number = :bedNumber",
            nativeQuery = true)
    Optional<Boolean> isCamaOcupada(@Param("bedNumber") String bedNumber);

    /**
     * Cuenta total de camas
     */
    @Query(value = "SELECT COUNT(*) FROM vista_pacientes_por_cama", nativeQuery = true)
    Long contarTotalCamas();

    /**
     * Cuenta camas ocupadas
     */
    @Query(value = "SELECT COUNT(*) FROM vista_pacientes_por_cama WHERE patient_data IS NOT NULL", nativeQuery = true)
    Long contarCamasOcupadas();

    /**
     * Cuenta camas disponibles
     */
    @Query(value = "SELECT COUNT(*) FROM vista_pacientes_por_cama WHERE patient_data IS NULL", nativeQuery = true)
    Long contarCamasDisponibles();
}