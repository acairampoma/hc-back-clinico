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
 * Utiliza la vista vista_pacientes_por_cama REFACTORIZADA
 * INCLUYE: Queries optimizadas para notas médicas
 */
@Repository
public interface PacientePorCamaRepository extends JpaRepository<PacientePorCama, String> {

    /**
     * Obtiene todas las camas con información completa (incluye campos para notas médicas)
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
     * Obtiene información COMPLETA de una cama específica por número
     * OPTIMIZADO: Para notas médicas con todos los campos necesarios
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE bed_number = :bedNumber",
            nativeQuery = true)
    List<Object[]> obtenerCamaPorNumero(@Param("bedNumber") String bedNumber);

    /**
     * NUEVO: Obtiene datos específicos para notas médicas de una cama
     */
    @Query(value = "SELECT " +
            "bed_number, " +
            "patient_data->>'hospitalizacion_id' as hospitalizacion_id, " +
            "patient_data->>'numero_cuenta' as numero_cuenta, " +
            "patient_data->>'paciente_id' as paciente_id, " +
            "patient_data->>'medico_tratante_id' as medico_tratante_id, " +
            "patient_data->>'especialidad_id' as especialidad_id, " +
            "patient_data->'personal_info'->>'fullname' as fullname, " +
            "patient_data->'medical_info'->>'primary_diagnosis' as primary_diagnosis " +
            "FROM vista_pacientes_por_cama " +
            "WHERE bed_number = :bedNumber " +
            "AND patient_data IS NOT NULL",
            nativeQuery = true)
    List<Object[]> obtenerDatosNotasMedicas(@Param("bedNumber") String bedNumber);

    /**
     * Busca camas por rango de números (ej: C001-C010)
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE bed_number BETWEEN :startBed AND :endBed ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> obtenerCamasPorRango(@Param("startBed") String startBed, @Param("endBed") String endBed);

    /**
     * Busca pacientes por DNI
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data FROM vista_pacientes_por_cama WHERE patient_data->'personal_info'->>'dni' = :dni",
            nativeQuery = true)
    List<Object[]> buscarPacientePorDni(@Param("dni") String dni);

    /**
     * Busca pacientes por nombre (búsqueda parcial) - MEJORADO
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data " +
            "FROM vista_pacientes_por_cama " +
            "WHERE (" +
            "LOWER(patient_data->'personal_info'->>'first_name') LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
            "LOWER(patient_data->'personal_info'->>'last_name') LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
            "LOWER(patient_data->'personal_info'->>'fullname') LIKE LOWER(CONCAT('%', :nombre, '%'))" +
            ") " +
            "ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> buscarPacientesPorNombre(@Param("nombre") String nombre);

    /**
     * Busca pacientes por médico tratante
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data " +
            "FROM vista_pacientes_por_cama " +
            "WHERE LOWER(patient_data->'medical_info'->>'attending_physician') LIKE LOWER(CONCAT('%', :medico, '%')) " +
            "ORDER BY bed_number",
            nativeQuery = true)
    List<Object[]> buscarPacientesPorMedico(@Param("medico") String medico);

    /**
     * NUEVO: Busca pacientes por hospitalización ID
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data " +
            "FROM vista_pacientes_por_cama " +
            "WHERE (patient_data->>'hospitalizacion_id')::bigint = :hospitalizacionId",
            nativeQuery = true)
    List<Object[]> buscarPacientePorHospitalizacionId(@Param("hospitalizacionId") Long hospitalizacionId);

    /**
     * NUEVO: Busca pacientes por número de cuenta
     */
    @Query(value = "SELECT bed_number, CAST(patient_data AS TEXT) as patient_data " +
            "FROM vista_pacientes_por_cama " +
            "WHERE patient_data->>'numero_cuenta' = :numeroCuenta",
            nativeQuery = true)
    List<Object[]> buscarPacientePorNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

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
     * NUEVO: Obtiene estadísticas por especialidad
     */
    @Query(value = "SELECT " +
            "patient_data->'medical_info'->>'attending_physician' as especialidad, " +
            "COUNT(*) as total_pacientes " +
            "FROM vista_pacientes_por_cama " +
            "WHERE patient_data IS NOT NULL " +
            "GROUP BY patient_data->'medical_info'->>'attending_physician' " +
            "ORDER BY total_pacientes DESC",
            nativeQuery = true)
    List<Object[]> obtenerEstadisticasPorEspecialidad();

    /**
     * Verifica si una cama específica está ocupada
     */
    @Query(value = "SELECT CASE WHEN patient_data IS NOT NULL THEN true ELSE false END FROM vista_pacientes_por_cama WHERE bed_number = :bedNumber",
            nativeQuery = true)
    Optional<Boolean> isCamaOcupada(@Param("bedNumber") String bedNumber);

    /**
     * NUEVO: Verifica si un paciente tiene notas médicas pendientes
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM hospitalizacion_notas hn " +
            "INNER JOIN vista_pacientes_por_cama vpc ON (vpc.patient_data->>'hospitalizacion_id')::bigint = hn.hospitalizacion_id " +
            "WHERE vpc.bed_number = :bedNumber " +
            "AND hn.estado = '01'",
            nativeQuery = true)
    Optional<Boolean> tieneNotasPendientes(@Param("bedNumber") String bedNumber);

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

    /**
     * NUEVO: Cuenta pacientes por especialidad
     */
    @Query(value = "SELECT COUNT(*) FROM vista_pacientes_por_cama " +
            "WHERE patient_data IS NOT NULL " +
            "AND patient_data->'medical_info'->>'attending_physician' LIKE CONCAT('%', :especialidad, '%')",
            nativeQuery = true)
    Long contarPacientesPorEspecialidad(@Param("especialidad") String especialidad);
}