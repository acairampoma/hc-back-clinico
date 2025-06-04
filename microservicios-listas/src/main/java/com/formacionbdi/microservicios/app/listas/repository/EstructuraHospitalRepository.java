package com.formacionbdi.microservicios.app.listas.repository;

import com.formacionbdi.microservicios.app.listas.models.entity.EstructuraHospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para consultar la vista vista_estructura_hospital
 */
@Repository
public interface EstructuraHospitalRepository extends JpaRepository<EstructuraHospital, Long> {

    /**
     * Obtiene la estructura completa del hospital desde la vista PostgreSQL
     * La vista ya retorna todo el JSON estructurado
     */
    @Query(value = "SELECT CAST(estructura_completa AS TEXT) FROM vista_estructura_hospital",
            nativeQuery = true)
    List<String> obtenerEstructuraCompleta();

    /**
     * Obtiene la estructura del primer hospital activo
     */
    @Query(value = "SELECT CAST(estructura_completa AS TEXT) FROM vista_estructura_hospital LIMIT 1",
            nativeQuery = true)
    Optional<String> obtenerPrimeraEstructura();

    /**
     * Obtiene la estructura del hospital por ID específico
     */
    @Query(value = "SELECT CAST(estructura_completa AS TEXT) FROM vista_estructura_hospital WHERE hospital_id = :hospitalId LIMIT 1",
            nativeQuery = true)
    Optional<String> obtenerEstructuraPorHospitalId(@Param("hospitalId") Long hospitalId);

    /**
     * Verifica si existe estructura para un hospital específico
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM vista_estructura_hospital WHERE hospital_id = :hospitalId",
            nativeQuery = true)
    boolean existeEstructuraPorHospitalId(@Param("hospitalId") Long hospitalId);

    /**
     * Obtiene solo la información básica del hospital (sin camas)
     */
    @Query(value = "SELECT jsonb_build_object(" +
            "'hospital', (estructura_completa->'hospital'), " +
            "'floors', jsonb_agg(" +
            "jsonb_build_object(" +
            "'floor_number', floor_data->'floor_number', " +
            "'specialty', floor_data->'specialty', " +
            "'specialty_code', floor_data->'specialty_code', " +
            "'department_head', floor_data->'department_head', " +
            "'phone_extension', floor_data->'phone_extension', " +
            "'color_theme', floor_data->'color_theme', " +
            "'icon', floor_data->'icon'" +
            ")))::text " +
            "FROM vista_estructura_hospital, " +
            "jsonb_array_elements(estructura_completa->'floors') AS floor_data " +
            "GROUP BY estructura_completa->'hospital'",
            nativeQuery = true)
    Optional<String> obtenerEstructuraBasica();
}